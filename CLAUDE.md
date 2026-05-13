# CLAUDE.md - mall-swarm

## Project Overview

mall-swarm is a Spring Cloud microservice e-commerce mall system with 10 modules.

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.2, Spring Cloud 2023.0.1, Spring Cloud Alibaba 2023.0.1.0
- **Build Tool**: Maven (multi-module, parent POM at root)
- **ORM**: MyBatis 3.5.14 + MyBatis Generator (mall-mbg module)
- **Auth**: Sa-Token 1.37.0 (centralized in mall-gateway, JWT tokens, Redis-backed sessions)
- **DB**: MySQL 8.0.29, Druid 1.2.9, PageHelper 6.1.0
- **Search**: Elasticsearch (mall-search module)
- **Gateway**: Spring Cloud Gateway (mall-gateway)
- **Monitor**: Spring Boot Admin 3.2.2 (mall-monitor)
- **Storage**: MinIO 8.4.5, Aliyun OSS
- **Payment**: Alipay SDK 4.38.61
- **API Docs**: Knife4j 4.5.0 (OpenAPI 3)
- **Utils**: Hutool 5.8.16, Lombok

### Module Architecture

| Module | Purpose |
|--------|---------|
| mall-common | Shared API types (CommonResult, CommonPage), GlobalExceptionHandler, Redis service, AOP logging |
| mall-mbg | MyBatis Generator auto-generated models, mappers, XMLs for all DB tables |
| mall-gateway | API gateway with Sa-Token auth filtering, route configuration |
| mall-auth | External OAuth2-style auth center, Feign clients for login forwarding |
| mall-admin | Backend admin CRUD services (product, order, coupon, user management) |
| mall-portal | Frontend member services (cart, order, member, payment, collection) |
| mall-search | Elasticsearch product search service |
| mall-demo | Feign inter-service call testing |
| mall-monitor | Spring Boot Admin server |
| config | Externalized configs per service per environment (Nacos-compatible) |

## Build & Run Commands

```bash
# Build entire project
mvn clean compile -DskipTests

# Build specific module with dependencies
mvn clean compile -pl mall-admin -am -DskipTests

# Package specific module
mvn clean package -pl mall-gateway -am

# Run a module locally
cd mall-admin && mvn spring-boot:run

# Run tests for a module
mvn test -pl mall-admin

# Run a single test class
mvn test -pl mall-admin -Dtest=ClassName

# Docker image build (via fabric8 docker-maven-plugin at package phase)
mvn clean package -pl mall-admin -am
```

**pom.xml notes**: `skipTests` defaults to `true`. `java.version` = 17. All dependency versions managed in root pom `<dependencyManagement>`.

## Code Review Guidelines

### 1. Transaction Boundaries (CRITICAL)

`@Transactional` on service methods must specify `rollbackFor = Exception.class`. Default Spring behavior only rolls back on RuntimeException/Error — checked exceptions silently commit partial work.

```java
// Correct
@Transactional(rollbackFor = Exception.class)
public int create(Entity entity) { ... }

// Also correct - read-only operations
@Transactional(readOnly = true)
public Entity getById(Long id) { ... }

// Incorrect - missing rollbackFor
@Transactional
public int create(Entity entity) { ... }
```

- Do NOT place `@Transactional` on controller methods — always on the service layer.
- When modifying multiple tables, ensure one transactional boundary covers all inserts/updates/deletes.

### 2. Sa-Token Auth Consistency (CRITICAL)

All auth logic is centralized in `mall-gateway/src/main/java/com/macro/mall/config/SaTokenConfig.java`:
- Admin auth: `SaRouter.match("/mall-admin/**", r -> StpUtil.checkLogin())`
- Member auth: `SaRouter.match("/mall-portal/**", r -> StpMemberUtil.checkLogin())`
- Permission checking loads `auth:pathResourceMap` from Redis.

Key rules:
- `StpUtil` (TYPE="login") and `StpMemberUtil` (TYPE="memberLogin") are separate login systems — never mix them.
- New API paths requiring permission checks must be registered in Redis `auth:pathResourceMap`.
- Verify white-listed URLs are in `IgnoreUrlsConfig` if they bypass auth.
- `StpInterfaceImpl` in mall-gateway returns permission list for admin login type only.

### 3. MyBatis SQL Safety (IMPORTANT)

- Custom DAO XMLs (in `src/main/resources/dao/`) must use `#{}` parameter binding, NEVER `${}` for user input.
- Batch inserts must use `<foreach>` with `#{}` placeholders.
- Dynamic SQL (`<if>`, `<choose>`) must not allow raw string concatenation.
- MBG-generated XMLs (in mall-mbg module) are auto-generated — do not manually edit; regenerate instead.
- Custom `*Dao.java` + `*Dao.xml` in mall-admin, mall-portal, mall-search are the ones to review.

### 4. Feign Client Interface Consistency (IMPORTANT)

- Feign client method signatures must match controller method signatures exactly (path, params, return type).
- Mismatched `@RequestParam` (required vs optional) causes runtime errors.
- Both sides must use `CommonResult<T>` wrapper.
- Currently Feign clients exist only in mall-auth and mall-demo — review any new Feign clients carefully.

### 5. Error Handling Completeness (IMPORTANT)

- `GlobalExceptionHandler` in mall-common only handles `ApiException`, `MethodArgumentNotValidException`, `BindException` — check if new code needs additional handlers.
- Services must not catch exceptions silently; re-throw as `ApiException` with appropriate error code.
- Use `Asserts` utility in mall-common for business validation (throws ApiException).
- No bare `try { ... } catch(Exception e) { e.printStackTrace(); }` — use structured logging.
- Payment/order flows must have explicit error handling — money-related operations must not silently fail.

### 6. API Compatibility Across Modules (IMPORTANT)

- `mall-common` is a shared dependency — changes there are HIGH IMPACT and must be backward compatible.
- Changing `CommonResult` or `CommonPage` structure breaks ALL services.
- Changing a controller path in one module can break Feign callers in another.
- Config changes in `config/` directory must stay synchronized across all service configs.

### 7. Coding Conventions

- Controllers use `@Controller` + `@ResponseBody` (not `@RestController`) — follow existing pattern.
- Controller methods return `CommonResult<T>` or `CommonResult<CommonPage<T>>`.
- Service interfaces define the contract, implementations in `*.impl` package.
- Use Lombok `@Data`, `@EqualsAndHashCode` on model classes.
- Knife4j: `@Tag` on controller class, `@Operation` on each endpoint.
- Use `@Validated` on request body parameters for validation.
- Logging: `private static final Logger LOGGER = LoggerFactory.getLogger(ClassName.class)`.
- Configuration: use `@Value` (existing pattern over `@ConfigurationProperties`).

### 8. Security

- No hardcoded credentials — use `@Value` + external config.
- Verify all dynamic SQL uses `#{}` not `${}`.
- JWT tokens managed by Sa-Token — do not bypass gateway auth.
- File upload endpoints must validate file types and sizes.
- Payment callbacks must verify signatures before processing.

### 9. Testing

- Tests use `@SpringBootTest` with `@Transactional` (auto-rollback).
- New service modules should include integration tests.
- Run with: `mvn test -pl <module>`.

## Architecture Notes

- Auth is done at gateway level — individual services trust the gateway.
- Sa-Token uses JWT in Reactor mode (`sa-token-reactor-spring-boot3-starter`) for gateway compatibility.
- Two separate login systems: admin (`StpUtil`, TYPE="login") and member (`StpMemberUtil`, TYPE="memberLogin").
- Configs designed for Nacos config center but stored locally in `config/` directory.
- Aliyun OSS and MinIO are both used (migration in progress).

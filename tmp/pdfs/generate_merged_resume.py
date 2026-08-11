from pathlib import Path

from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parents[2]
SOURCE_PDF = Path(r"C:\Users\lx\Desktop\面试\简历.pdf")
OUTPUT_DIR = ROOT / "output" / "pdf"
OUTPUT_PDF = OUTPUT_DIR / "陈研研_Java后端开发_项目经验合并版.pdf"
PORTRAIT = ROOT / "tmp" / "pdfs" / "portrait.jpg"

PAGE_W, PAGE_H = A4
MARGIN_X = 42
CONTENT_W = PAGE_W - MARGIN_X * 2

BLUE = colors.HexColor("#2F67A1")
BLUE_LIGHT = colors.HexColor("#EAF2F9")
INK = colors.HexColor("#24313D")
MUTED = colors.HexColor("#53606B")
LINE = colors.HexColor("#C8D2DC")
WHITE = colors.white


def register_fonts():
    pdfmetrics.registerFont(TTFont("CN", r"C:\Windows\Fonts\Deng.ttf"))
    pdfmetrics.registerFont(TTFont("CN-Bold", r"C:\Windows\Fonts\Dengb.ttf"))


def extract_portrait():
    if PORTRAIT.exists():
        return
    reader = PdfReader(str(SOURCE_PDF))
    image = reader.pages[0].images[0]
    PORTRAIT.write_bytes(image.data)


def text_width(text, font, size):
    return pdfmetrics.stringWidth(text, font, size)


def wrap_text(text, font, size, max_width):
    lines = []
    current = ""
    for char in text:
        candidate = current + char
        if current and text_width(candidate, font, size) > max_width:
            lines.append(current)
            current = char
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines


def draw_wrapped(c, text, x, y, max_width, font="CN", size=9.4,
                 leading=14.2, color=INK):
    c.setFont(font, size)
    c.setFillColor(color)
    for line in wrap_text(text, font, size, max_width):
        c.drawString(x, y, line)
        y -= leading
    return y


def draw_rich_bullet(c, title, body, x, y, max_width):
    bullet_x = x
    text_x = x + 15
    c.setFillColor(BLUE)
    c.circle(bullet_x + 3.5, y + 3.1, 2.4, fill=1, stroke=0)

    title_text = f"{title}："
    c.setFont("CN-Bold", 10.2)
    c.setFillColor(INK)
    c.drawString(text_x, y, title_text)
    title_w = text_width(title_text, "CN-Bold", 10.2)

    body_x = text_x + title_w
    first_width = max_width - 15 - title_w
    first = ""
    rest = body
    for i in range(1, len(body) + 1):
        if text_width(body[:i], "CN", 10.0) <= first_width:
            first = body[:i]
        else:
            break
    rest = body[len(first):]

    c.setFont("CN", 10.0)
    c.drawString(body_x, y, first)
    y -= 16.0
    for line in wrap_text(rest, "CN", 10.0, max_width - 15):
        c.drawString(text_x, y, line)
        y -= 16.0
    return y - 4


def draw_section_header(c, title, y):
    c.setFillColor(BLUE)
    c.roundRect(MARGIN_X, y - 4, 92, 23, 4, fill=1, stroke=0)
    c.setFillColor(WHITE)
    c.setFont("CN-Bold", 12.5)
    c.drawString(MARGIN_X + 12, y + 2, title)
    c.setStrokeColor(LINE)
    c.setLineWidth(0.8)
    c.line(MARGIN_X + 100, y - 1, PAGE_W - MARGIN_X, y - 1)
    return y - 25


def build_pdf():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    extract_portrait()
    register_fonts()

    c = canvas.Canvas(str(OUTPUT_PDF), pagesize=A4)
    c.setTitle("陈研研 - Java后端开发 - 项目经验合并版")
    c.setAuthor("陈研研")

    # Header
    c.setFillColor(BLUE)
    c.rect(0, PAGE_H - 16, PAGE_W, 16, fill=1, stroke=0)
    c.setFillColor(INK)
    c.setFont("CN-Bold", 24)
    c.drawString(MARGIN_X, PAGE_H - 55, "陈研研")
    c.setFillColor(BLUE)
    c.setFont("CN-Bold", 13)
    c.drawString(MARGIN_X, PAGE_H - 78, "Java 后端开发")

    info_x = 168
    info_y = PAGE_H - 45
    c.setFillColor(MUTED)
    c.setFont("CN", 9.4)
    info_lines = [
        "电话：19565608221    邮箱：3217810383@qq.com",
        "出生：2005.11       民族：汉族       政治面貌：群众",
        "院校：邢台学院      专业：网络工程（本科）",
    ]
    for line in info_lines:
        c.drawString(info_x, info_y, line)
        info_y -= 19

    portrait_w, portrait_h = 58, 78
    c.setStrokeColor(LINE)
    c.setLineWidth(0.8)
    c.rect(PAGE_W - MARGIN_X - portrait_w, PAGE_H - 101,
           portrait_w, portrait_h, fill=0, stroke=1)
    c.drawImage(ImageReader(str(PORTRAIT)),
                PAGE_W - MARGIN_X - portrait_w + 1,
                PAGE_H - 100,
                portrait_w - 2, portrait_h - 2,
                preserveAspectRatio=True, anchor="c")

    y = PAGE_H - 122

    # Skills
    y = draw_section_header(c, "专业技能", y)
    skills = [
        ("Java 基础", "集合、多线程、JVM 内存模型与 GC，具备面向对象与常用设计模式基础。"),
        ("后端与微服务", "Spring Boot、Spring MVC、MyBatis；Spring Cloud Alibaba、Nacos、Gateway、Sa-Token。"),
        ("数据与工程化", "MySQL、Redis、RabbitMQ、Elasticsearch；Maven、Docker、Nginx、Linux。"),
        ("AI 应用开发", "Spring AI、Function Calling、Chat Memory、RAG、Prompt 设计与本地向量化。"),
    ]
    for title, body in skills:
        y = draw_rich_bullet(c, title, body, MARGIN_X + 3, y,
                             CONTENT_W - 6)

    y -= 2

    # Project experience: two original entries merged into one evidence-backed project.
    y = draw_section_header(c, "项目经验", y)
    c.setFillColor(INK)
    c.setFont("CN-Bold", 14.5)
    c.drawString(MARGIN_X, y, "mall 微服务电商与 AI 智能客服系统")
    c.setFillColor(BLUE)
    c.setFont("CN-Bold", 9.5)
    right_title = "后端开发  |  2026.01 - 2026.07"
    c.drawRightString(PAGE_W - MARGIN_X, y + 1, right_title)
    y -= 20

    c.setFillColor(MUTED)
    intro = ("项目简介：面向商城后台与移动端的微服务电商平台，覆盖商品、会员、购物车、订单、营销、"
             "搜索与监控，并在 Portal 服务内集成 AI Agent，提供知识问答与真实业务查询。")
    y = draw_wrapped(c, intro, MARGIN_X, y, CONTENT_W, size=9.5,
                     leading=14.2, color=MUTED)
    y -= 2
    c.setFont("CN", 9.3)
    c.setFillColor(MUTED)
    c.drawString(MARGIN_X, y,
                 "技术栈：Java 17、Spring Boot 3、Spring Cloud Alibaba、MyBatis、MySQL、Redis")
    y -= 14
    c.drawString(MARGIN_X, y,
                 "　　　　RabbitMQ、Sa-Token、Spring AI、Docker")
    y -= 14
    y -= 5

    bullets = [
        (
            "微服务架构与统一鉴权",
            "拆分 Gateway、Auth、Admin、Portal、Search、Monitor 等服务，基于 Nacos 完成注册配置管理；在网关使用 Sa-Token + JWT 实现管理端/用户端双账号体系、白名单放行和接口权限校验。"
        ),
        (
            "订单超时自动关闭",
            "使用 RabbitMQ 消息 TTL、死信交换机与路由键实现“下单-延迟消息-超时取消-库存回滚”链路，将订单关单从定时轮询改为异步触发，减少无效扫描并及时释放库存。"
        ),
        (
            "Agent 闭环编排",
            "基于 Spring AI 将客服升级为 Plan-Act-Observe-Reflect-Synthesis 流程，支持目标拆解、最多 4 轮迭代、反思重规划与降级直答；中间过程写入 Scratchpad，仅持久化最终对话，避免历史上下文污染。"
        ),
        (
            "业务工具调用与结构化展示",
            "封装商品搜索/详情、订单列表/详情 4 类 ToolCallback；使用请求级结果容器隔离并发调用，并将工具结果映射为商品卡片和订单卡片，保证回复数据可追溯且便于前端展示。"
        ),
        (
            "RAG 知识库与会话记忆",
            "构建“查询改写-向量检索-上下文增强”链路，采用本地 ONNX all-MiniLM-L6-v2 向量化、TokenTextSplitter 切块及 SimpleVectorStore 持久化；Redis 保存最近 10 轮问答并设置过期时间，支持多轮客服对话。"
        ),
    ]
    for title, body in bullets:
        y = draw_rich_bullet(c, title, body, MARGIN_X + 3, y,
                             CONTENT_W - 6)

    y -= 12

    # Education
    y = draw_section_header(c, "教育背景", y)
    c.setFillColor(INK)
    c.setFont("CN-Bold", 10.5)
    c.drawString(MARGIN_X, y, "2023.09 - 2027.07")
    c.drawString(MARGIN_X + 144, y, "邢台学院")
    c.drawRightString(PAGE_W - MARGIN_X, y, "网络工程专业  |  本科")
    y -= 18
    courses = ("核心课程：数据结构、计算机组成原理、计算机网络、数据库系统、操作系统、"
               "协议分析与网络编程、离散数学、线性代数。")
    draw_wrapped(c, courses, MARGIN_X, y, CONTENT_W, size=8.9,
                 leading=13.5, color=MUTED)

    # Footer
    c.setStrokeColor(LINE)
    c.line(MARGIN_X, 25, PAGE_W - MARGIN_X, 25)
    c.setFillColor(MUTED)
    c.setFont("CN", 7.5)
    c.drawCentredString(PAGE_W / 2, 13, "求职方向：Java 后端开发")

    c.showPage()
    c.save()
    print(OUTPUT_PDF)


if __name__ == "__main__":
    build_pdf()

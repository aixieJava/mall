package com.macro.mall.portal.agent;

import com.macro.mall.portal.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库构建器：应用启动时把 knowledge/ 下的文档切块、向量化并存入 SimpleVectorStore。
 * 索引文件已存在则直接 load，不重复 embedding。
 */
@Component
public class KnowledgeIngestionService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final SimpleVectorStore vectorStore;
    private final RagProperties props;
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public KnowledgeIngestionService(SimpleVectorStore vectorStore, RagProperties props) {
        this.vectorStore = vectorStore;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.isEnabled()) {
            LOGGER.info("RAG 已禁用，跳过知识库构建");
            return;
        }
        try {
            File storeFile = new File(props.getVectorStoreFile());
            if (storeFile.exists()) {
                vectorStore.load(storeFile);
                LOGGER.info("已从持久化文件加载向量索引: {}", storeFile.getAbsolutePath());
                return;
            }
            List<Document> docs = readKnowledge();
            if (docs.isEmpty()) {
                LOGGER.warn("未读取到任何知识文档，路径: {}", props.getKnowledgePath());
                return;
            }
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(props.getChunkTokenSize())
                    .build();
            List<Document> chunks = splitter.apply(docs);
            LOGGER.info("知识库切块完成: 文档 {} 篇 -> {} 块，开始本地 embedding（首次较慢）", docs.size(), chunks.size());
            vectorStore.add(chunks);
            storeFile.getParentFile().mkdirs();
            vectorStore.save(storeFile);
            LOGGER.info("向量索引构建完成并持久化: {}", storeFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("知识库构建失败: {}", e.getMessage(), e);
        }
    }

    private List<Document> readKnowledge() throws Exception {
        List<Document> result = new ArrayList<>();
        String pattern = props.getKnowledgePath();
        if (!pattern.endsWith("/")) {
            pattern = pattern + "/";
        }
        Resource[] resources = resolver.getResources(pattern + "*.md");
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            TextReader reader = new TextReader(resource);
            reader.getCustomMetadata().put("source", filename);
            result.addAll(reader.get());
        }
        return result;
    }
}

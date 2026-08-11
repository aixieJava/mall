package com.macro.mall.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 知识库配置（对应 application.yml 中 rag.* ）
 */
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /** 是否启用 RAG */
    private boolean enabled = true;

    /** 知识文档目录 */
    private String knowledgePath = "classpath:knowledge/";

    /** SimpleVectorStore 持久化文件路径 */
    private String vectorStoreFile = System.getProperty("user.home") + "/.mall-ai/vector-store.json";

    /** 检索返回的文档数 */
    private int topK = 4;

    /** 相似度阈值 */
    private double similarityThreshold = 0.5;

    /** TokenTextSplitter 目标块大小（token） */
    private int chunkTokenSize = 400;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKnowledgePath() {
        return knowledgePath;
    }

    public void setKnowledgePath(String knowledgePath) {
        this.knowledgePath = knowledgePath;
    }

    public String getVectorStoreFile() {
        return vectorStoreFile;
    }

    public void setVectorStoreFile(String vectorStoreFile) {
        this.vectorStoreFile = vectorStoreFile;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public int getChunkTokenSize() {
        return chunkTokenSize;
    }

    public void setChunkTokenSize(int chunkTokenSize) {
        this.chunkTokenSize = chunkTokenSize;
    }
}

package com.macro.mall.portal.agent;

import com.macro.mall.portal.config.RagProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Component;

/**
 * 构建模块化 RAG advisor：查询改写 -> 向量检索 -> 上下文增强。
 */
@Component
public class RagAdvisorFactory {

    private final SimpleVectorStore vectorStore;
    private final ChatModel chatModel;
    private final RagProperties props;

    public RagAdvisorFactory(SimpleVectorStore vectorStore, ChatModel chatModel, RagProperties props) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.props = props;
    }

    public Advisor retrievalAdvisor() {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(ChatClient.builder(chatModel).build().mutate())
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(props.getSimilarityThreshold())
                        .topK(props.getTopK())
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        // 检索为空时仍允许回答（走工具/常识），不硬拒答
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
}

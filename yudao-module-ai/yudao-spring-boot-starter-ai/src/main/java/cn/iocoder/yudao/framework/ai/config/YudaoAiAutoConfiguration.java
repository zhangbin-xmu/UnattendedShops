package cn.iocoder.yudao.framework.ai.config;

import cn.iocoder.yudao.framework.ai.core.factory.AiModelFactory;
import cn.iocoder.yudao.framework.ai.core.factory.AiModelFactoryImpl;
import cn.iocoder.yudao.framework.ai.core.model.deepseek.DeepSeekChatModel;
import cn.iocoder.yudao.framework.ai.core.model.deepseek.DeepSeekChatOptions;
import cn.iocoder.yudao.framework.ai.core.model.midjourney.api.MidjourneyApi;
import cn.iocoder.yudao.framework.ai.core.model.suno.api.SunoApi;
import cn.iocoder.yudao.framework.ai.core.model.xinghuo.XingHuoChatModel;
import cn.iocoder.yudao.framework.ai.core.model.xinghuo.XingHuoChatOptions;
import com.alibaba.cloud.ai.tongyi.TongYiAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import redis.clients.jedis.JedisPooled;

/**
 * 芋道 AI 自动配置
 *
 * @author fansili
 */
@AutoConfiguration
@EnableConfigurationProperties(YudaoAiProperties.class)
@Slf4j
@Import(TongYiAutoConfiguration.class)
public class YudaoAiAutoConfiguration {

    @Bean
    public AiModelFactory aiModelFactory() {
        return new AiModelFactoryImpl();
    }

    // ========== 各种 AI Client 创建 ==========

    @Bean
    @ConditionalOnProperty(value = "yudao.ai.deepseek.enable", havingValue = "true")
    public DeepSeekChatModel deepSeekChatModel(YudaoAiProperties yudaoAiProperties) {
        YudaoAiProperties.DeepSeekProperties properties = yudaoAiProperties.getDeepSeek();
        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .topP(properties.getTopP())
                .build();
        return new DeepSeekChatModel(properties.getApiKey(), options);
    }

    @Bean
    @ConditionalOnProperty(value = "yudao.ai.xinghuo.enable", havingValue = "true")
    public XingHuoChatModel xingHuoChatClient(YudaoAiProperties yudaoAiProperties) {
        YudaoAiProperties.XingHuoProperties properties = yudaoAiProperties.getXinghuo();
        XingHuoChatOptions options = XingHuoChatOptions.builder()
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .topK(properties.getTopK())
                .build();
        return new XingHuoChatModel(properties.getAppKey(), properties.getSecretKey(), options);
    }

    @Bean
    @ConditionalOnProperty(value = "yudao.ai.midjourney.enable", havingValue = "true")
    public MidjourneyApi midjourneyApi(YudaoAiProperties yudaoAiProperties) {
        YudaoAiProperties.MidjourneyProperties config = yudaoAiProperties.getMidjourney();
        return new MidjourneyApi(config.getBaseUrl(), config.getApiKey(), config.getNotifyUrl());
    }

    @Bean
    @ConditionalOnProperty(value = "yudao.ai.suno.enable", havingValue = "true")
    public SunoApi sunoApi(YudaoAiProperties yudaoAiProperties) {
        return new SunoApi(yudaoAiProperties.getSuno().getBaseUrl());
    }

    // ========== rag 相关 ==========
    @Bean
    @Lazy // TODO 芋艿：临时注释，避免无法启动
    public EmbeddingModel transformersEmbeddingClient() {
        return new TransformersEmbeddingModel(MetadataMode.EMBED);
    }

    /**
     * 我们启动有加载很多 Embedding 模型，不晓得取哪个好，先 new 个 TransformersEmbeddingModel 跑
     */
    @Bean
    @Lazy // TODO 芋艿：临时注释，避免无法启动
    public RedisVectorStore vectorStore(TransformersEmbeddingModel transformersEmbeddingModel, RedisVectorStoreProperties properties,
                                        RedisProperties redisProperties) {
        var config = RedisVectorStore.RedisVectorStoreConfig.builder()
                .withIndexName(properties.getIndex())
                .withPrefix(properties.getPrefix())
                .build();

        RedisVectorStore redisVectorStore = new RedisVectorStore(config, transformersEmbeddingModel,
                new JedisPooled(redisProperties.getHost(), redisProperties.getPort()),
                properties.isInitializeSchema());
        redisVectorStore.afterPropertiesSet();
        return redisVectorStore;
    }

    @Bean
    @Lazy // TODO 芋艿：临时注释，避免无法启动
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(500, 100, 5, 10000, true);
    }

}
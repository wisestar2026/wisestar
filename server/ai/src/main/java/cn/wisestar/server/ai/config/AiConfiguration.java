package cn.wisestar.server.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * AI 模块配置类（AiConfiguration）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，config 包）。</p>
 * <p><b>类职责</b>：为 AI 模块声明所需的 Spring Bean——主要是用于调用大模型
 * HTTP 接口的响应式 {@link WebClient}（Spring WebFlux 的 WebClient，配合
 * SiliconflowChatServiceImpl 的流式请求使用）。</p>
 * <p><b>被谁调用</b>：Spring 容器在装配 ai 模块时自动加载本配置类；其中声明的
 * webClient Bean 会被 {@code SiliconflowChatServiceImpl} 构造器注入使用。</p>
 * <p><b>依赖的服务</b>：依赖 Spring Boot 自动装配的 {@link WebClient.Builder}（由
 * spring-boot-starter-webflux 提供），并基于它定制 WebClient 实例。</p>
 *
 * <p><b>数据流说明</b>：本类不参与 HTTP 业务请求处理，仅负责 Bean 装配：
 * Spring 启动时执行 @Bean 方法 → 创建并注册 webClient Bean
 * → 注入 SiliconflowChatServiceImpl → 供 AI 流式对话请求使用。</p>
 */
@Configuration
public class AiConfiguration {
    /**
     * 创建用于调用 AI 平台的响应式 WebClient Bean。
     *
     * <p><b>功能</b>：基于 Spring Boot 提供的 {@link WebClient.Builder} 构建 WebClient，
     * 关键定制——将内存缓冲上限（maxInMemorySize）从默认值（256KB）调大到
     * 16MB（16 * 1024 * 1024 字节），避免大模型流式响应的数据块因超过默认
     * 内存缓冲而被 WebClient 丢弃/报错。</p>
     *
     * <p><b>参数</b>：builder（{@link WebClient.Builder}，由 Spring Boot 自动配置注入，
     * 已携带默认超时、编解码器等设置）。</p>
     *
     * <p><b>返回值</b>：配置完成的 {@link WebClient} 实例（Spring 单例 Bean）。</p>
     *
     * <p><b>调用方</b>：{@code SiliconflowChatServiceImpl#SiliconflowChatServiceImpl}
     * 构造器注入此 Bean，用于发送 POST /v1/chat/completions 流式请求。</p>
     *
     * @param builder Spring Boot 自动配置的 WebClient.Builder
     * @return 配置好内存缓冲的 WebClient 实例
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB buffer
                .build();
    }

}

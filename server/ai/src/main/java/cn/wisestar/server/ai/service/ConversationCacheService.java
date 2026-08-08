package cn.wisestar.server.ai.service;

import cn.wisestar.server.ai.domain.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 对话缓存服务（ConversationCacheService）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，service 包）。</p>
 * <p><b>类职责</b>：在 JVM 内存中缓存用户对话消息（按会话 id 组织），默认保留 10 分钟；
 * 用于支撑"无状态会话 + 内存缓存"的 AI 对话方案——前端通过会话 id 关联历史消息，
 * 无需后端持久化对话记录。提供：创建会话缓存、追加消息、读取历史消息、
 * 关闭会话（清理缓存）、定时清理过期会话。</p>
 * <p><b>被谁调用</b>：{@code ChatController}（create-conversation 创建缓存、
 * stream 读写消息、close-conversation 清理缓存）。</p>
 * <p><b>依赖的服务</b>：无外部服务依赖（纯 JDK 并发工具）。</p>
 *
 * <p><b>数据流</b>：</p>
 * <pre>
 *   POST /api/ai/chat/create-conversation --&gt; ConversationCacheService#createConversation
 *       --&gt; conversationMap.put(conversationId, new ConversationCache())
 *   GET /api/ai/chat/stream?content=xxx&amp;conversation_id=yyy --&gt; addMessage（写入）
 *       + getMessages（读取历史）
 *   POST /api/ai/chat/close-conversation --&gt; closeConversation（移除）
 *   后台调度线程（每分钟） --&gt; cleanExpiredConversations（清理超过 10 分钟未访问的会话）
 * </pre>
 *
 * @author zzr
 */
@Service
public class ConversationCacheService {

    /**
     * 日志记录器（SLF4J，修复：原实现用 System.out 打印清理日志，改为标准日志框架）。
     */
    private static final Logger log = LoggerFactory.getLogger(ConversationCacheService.class);

    /**
     * 会话缓存容器：key 为会话 id（String），value 为该会话的消息缓存对象
     * （{@link ConversationCache}，内含消息列表与最后访问时间）。
     * 使用 ConcurrentHashMap 保证多线程读写安全。
     */
    private final ConcurrentHashMap<String, ConversationCache> conversationMap = new ConcurrentHashMap<>();

    /**
     * 定时调度线程池（单线程）：用于周期性执行过期会话清理任务
     * （构造器中以 fixedRate 每分钟执行一次 cleanExpiredConversations）。
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 构造器：启动过期清理定时任务。
     *
     * <p><b>功能</b>：创建单线程定时调度器，并以固定频率（初始延迟 1 分钟、
     * 每 1 分钟一次）执行 {@link #cleanExpiredConversations()}，
     * 自动清理超过 10 分钟未访问的会话缓存，防止内存泄漏。</p>
     */
    public ConversationCacheService() {
        // 每分钟清理一次过期的对话
        scheduler.scheduleAtFixedRate(this::cleanExpiredConversations, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 创建新对话缓存。
     *
     * <p><b>功能</b>：为指定会话 id 在缓存容器中放入一个新的空缓存对象
     * （消息列表为空、最后访问时间为当前时间）。若该会话已存在则覆盖旧缓存。</p>
     *
     * <p><b>参数说明</b>：conversationId——会话 id（由 create-conversation 接口生成，
     * UUID 去横线）。</p>
     *
     * <p><b>调用链</b>：ChatController#createConversation → 本方法。</p>
     */
    public void createConversation(String conversationId) {
        conversationMap.put(conversationId, new ConversationCache());
    }

    /**
     * 添加消息到对话。
     *
     * <p><b>功能</b>：将一条用户/AI 消息追加到指定会话的缓存消息列表末尾，
     * 并刷新该会话的最后访问时间（用于过期判断）。会话不存在时静默忽略。</p>
     *
     * <p><b>参数说明</b>：conversationId——会话 id；message——{@link ChatRequest.EnterMessage}
     * （role + content，user 与 assistant 消息都会写入，保证多轮上下文完整）。</p>
     *
     * <p><b>调用链</b>：ChatController#createChatStream（写入用户消息时）→ 本方法。</p>
     */
    public void addMessage(String conversationId, ChatRequest.EnterMessage message) {
        ConversationCache cache = conversationMap.get(conversationId);
        if (cache != null) {
            cache.addMessage(message);
            cache.updateLastAccessTime();
        }
    }

    /**
     * 获取对话历史消息。
     *
     * <p><b>功能</b>：返回指定会话的消息历史列表（不可变视图，防止外部修改缓存数据），
     * 并刷新最后访问时间。会话不存在时返回空列表（而非 null）。</p>
     *
     * <p><b>参数说明</b>：conversationId——会话 id。</p>
     *
     * <p><b>返回值结构</b>：{@code List<ChatRequest.EnterMessage>}（可能为空）。
     * 注意：getMessages 返回的是底层缓存的副本+不可变包装，后续组装 AI 请求体时
     * 会取最近 10 条作为上下文。</p>
     *
     * <p><b>调用链</b>：ChatController#createChatStream（读取历史上下文）→ 本方法。</p>
     */
    public List<ChatRequest.EnterMessage> getMessages(String conversationId) {
        ConversationCache cache = conversationMap.get(conversationId);
        if (cache != null) {
            cache.updateLastAccessTime();
            return cache.getMessages();
        }
        return new ArrayList<>();
    }

    /**
     * 关闭并清理对话。
     *
     * <p><b>功能</b>：从缓存容器中移除指定会话的全部缓存数据（释放内存）。
     * 会话 id 不存在时 remove 为无操作。</p>
     *
     * <p><b>参数说明</b>：conversationId——会话 id。</p>
     *
     * <p><b>调用链</b>：ChatController#closeConversation → 本方法。</p>
     */
    public void closeConversation(String conversationId) {
        conversationMap.remove(conversationId);
    }

    /**
     * 清理过期的对话（超过10分钟未访问）。
     *
     * <p><b>功能</b>：遍历缓存容器，找出最后访问时间距今超过 10 分钟（严格大于）
     * 的会话，逐个从容器移除；若有清理则通过 SLF4J 记录 info 日志。由构造器启动的
     * 定时任务每分钟调用一次。</p>
     *
     * <p><b>逻辑说明</b>：通过 {@link ChronoUnit#MINUTES} 计算
     * 最后访问时间与当前时间的分钟差，>10 即视为过期。</p>
     */
    private void cleanExpiredConversations() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredIds = conversationMap.entrySet().stream()
                .filter(entry -> ChronoUnit.MINUTES.between(entry.getValue().getLastAccessTime(), now) > 10)
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());

        expiredIds.forEach(conversationMap::remove);

        if (!expiredIds.isEmpty()) {
            log.info("清理了 {} 个过期对话缓存", expiredIds.size());
        }
    }

    /**
     * 对话缓存内部类（ConversationCache）。
     *
     * <p><b>类职责</b>：单个会话的消息缓存载体，包含：</p>
     * <ul>
     *   <li>messages——该会话的消息列表（有序，按添加顺序）；</li>
     *   <li>lastAccessTime——最后访问时间（读/写时刷新），用于过期清理判断。</li>
     * </ul>
     *
     * <p><b>线程安全说明</b>：本类未加锁，依赖外部 ConcurrentHashMap 的并发语义
     * 及单会话低并发读写的实际场景（由 ChatController 串行使用）。</p>
     */
    private static class ConversationCache {
        /**
         * 会话消息列表（按添加顺序保存 role/content 消息）。
         */
        private final List<ChatRequest.EnterMessage> messages = new java.util.ArrayList<>();

        /**
         * 最后访问时间（LocalDateTime），读写消息时刷新；用于过期清理判断。
         */
        private LocalDateTime lastAccessTime = LocalDateTime.now();

        /**
         * 添加一条消息到列表末尾。
         *
         * @param message 待添加的消息（role + content）
         */
        public void addMessage(ChatRequest.EnterMessage message) {
            messages.add(message);
        }

        /**
         * 获取消息列表（不可变视图）。
         * <p>先拷贝一份副本再包成 unmodifiableList，避免外部拿到引用后
         * 直接修改缓存内部数据。</p>
         *
         * @return 消息列表不可变视图
         */
        public List<ChatRequest.EnterMessage> getMessages() {
            return Collections.unmodifiableList(new ArrayList<>(messages));
        }

        /**
         * 刷新最后访问时间为当前时间。
         */
        public void updateLastAccessTime() {
            this.lastAccessTime = LocalDateTime.now();
        }

        /**
         * 获取最后访问时间。
         *
         * @return 最后访问时间
         */
        public LocalDateTime getLastAccessTime() {
            return lastAccessTime;
        }
    }
}

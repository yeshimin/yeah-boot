package com.yeshimin.yeahboot.flowcontrol.ratelimit;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RateLimit 拦截器
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final StandardEvaluationContext CONTEXT = new StandardEvaluationContext();

    // map<res, window>
    private final Map<String, SlidingWindow> plainHolder = new ConcurrentHashMap<>();
    // map<res, map<group, latestReqTime>>
    private final Map<String, ConcurrentHashMap<String, Long>> outerGroupHolder = new ConcurrentHashMap<>();
    // map<res, map<group, window>>
    private final Map<String, ConcurrentHashMap<String, SlidingWindow>> innerGroupHolder = new ConcurrentHashMap<>();

    // 每5分钟执行一次清理任务
    private static final long CLEAN_INTERVAL_MS = 5 * 60 * 1000;
    // 10分钟无请求则清理
    private static final long GROUP_EXPIRE_MS = 10 * 60 * 1000;

    @Autowired(required = false)
    private RateLimitService rateLimitService;

    @PostConstruct
    public void init() {
        log.debug("init [yeah-boot] rate limit interceptor...");
    }

    private static final ScheduledExecutorService CLEANER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "RateLimitCleaner");
                t.setDaemon(true);
                return t;
            });

    // constructor
    public RateLimitInterceptor() {
        CLEANER.scheduleAtFixedRate(this::cleanExpiredGroups,
                CLEAN_INTERVAL_MS, CLEAN_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            log.debug("Not a method handler, skip");
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 没有注解或禁用限流
        if (rateLimit == null || !rateLimit.enabled()) {
            log.debug("No rate limit annotation or disabled, skip");
            return true;
        }

        // --------------------------------------------------------------------------------

        // 获取限流配置
        RateLimitConf rlConf = this.getRateLimitConf(rateLimit, method, request);
        log.debug("rlConf: {}", rlConf);
        if (rlConf.isSkip()) {
            log.debug("Skip rate limit, skip");
            return true;
        }

        // --------------------------------------------------------------------------------

        // check limitCount
        SlidingWindow plainWindow = null;
        if (rlConf.getLimitCount() > -1) {
            log.debug("check for [limitCount]");

            plainWindow = plainHolder.computeIfAbsent(rlConf.getResName(),
                    k -> new SlidingWindow(rlConf, "[limitCount]"));
            log.debug("plainWindow: {}", plainWindow);

            boolean allowed = plainWindow.tryAcquire(rlConf.getLimitCount(), rlConf.getReqTime());
            if (!allowed) {
                log.debug("Rate limit exceeded: limitCount");
                response.setStatus(429);
                response.getWriter().write("Rate limit exceeded: limitCount");
                return false;
            }
        }

        // check limitGroup
        ConcurrentHashMap<String, Long> outerGroupMap = null;
        if (rlConf.getLimitGroup() > -1) {
            log.debug("check for [limitGroup]");
            outerGroupMap = outerGroupHolder.computeIfAbsent(rlConf.getResName(), k -> new ConcurrentHashMap<>());

            // 删除过期的分组
            outerGroupMap.entrySet().removeIf(entry -> {
                long reqTime = entry.getValue();
                if (reqTime < rlConf.getWindowFrom()) {
                    log.debug("Remove expired group:{}, windowFrom: {}, reqTime: {}",
                            entry.getKey(), rlConf.getWindowFrom(), reqTime);
                    return true;
                }
                return false;
            });

            // 判断是否超限
            boolean allowed;
            if (outerGroupMap.containsKey(rlConf.getGroupName())) {
                allowed = outerGroupMap.size() <= rlConf.getLimitGroup();
            } else {
                allowed = outerGroupMap.size() + 1 <= rlConf.getLimitGroup();
            }

            if (!allowed) {
                log.debug("Rate limit exceeded: limitGroup");
                response.setStatus(429);
                response.getWriter().write("Rate limit exceeded: limitGroup");
                return false;
            }
        }

        // check limitGroupCount
        SlidingWindow innerGroupWindow = null;
        if (rlConf.getLimitGroupCount() > -1) {
            log.debug("check for [limitGroupCount]");

            ConcurrentHashMap<String, SlidingWindow> map = innerGroupHolder.computeIfAbsent(
                    rlConf.getResName(), k -> new ConcurrentHashMap<>());
            innerGroupWindow = map.computeIfAbsent(rlConf.getGroupName(),
                    k -> new SlidingWindow(rlConf, "[limitGroupCount]"));
            log.debug("innerGroupWindow: {}", innerGroupWindow);

            boolean allowed = innerGroupWindow.tryAcquire(rlConf.getLimitGroupCount(), rlConf.getReqTime());

            if (!allowed) {
                log.debug("Rate limit exceeded: limitGroupCount");
                response.setStatus(429);
                response.getWriter().write("Rate limit exceeded: limitGroupCount");
                return false;
            }
        }

        // 仅当通过所有限流检查，记录请求
        if (rlConf.getLimitCount() > -1) {
            log.debug("Record request for [limitCount]");
            Objects.requireNonNull(plainWindow).increase(rlConf.getReqTime());
        }
        if (rlConf.getLimitGroup() > -1) {
            log.debug("Record request for [limitGroup]");
            Objects.requireNonNull(outerGroupMap).put(rlConf.getGroupName(), rlConf.getReqTime());
        }
        if (rlConf.getLimitGroupCount() > -1) {
            log.debug("Record request for [limitGroupCount]");
            Objects.requireNonNull(innerGroupWindow).increase(rlConf.getReqTime());
        }

        return true;
    }

    /**
     * 获取RateLimitConf配置
     */
    private RateLimitConf getRateLimitConf(RateLimit rl, Method m, HttpServletRequest req) {
        RateLimitConf conf = new RateLimitConf();
        conf.setEnabled(rl.enabled());
        conf.setName(rl.name());
        conf.setGroupType(rl.groupType());
        conf.setCustomGroup(rl.customGroup());
        conf.setLimitCount(rl.limitCount());
        conf.setLimitGroup(rl.limitGroup());
        conf.setLimitGroupCount(rl.limitGroupCount());
        conf.setTimeWindow(rl.timeWindow());
        conf.setBucketSize(rl.bucketSize());
        conf.setDynamicTimeWindow(rl.dynamicTimeWindow());
        conf.setGlobal(rl.global());

        // 修整参数
        if (conf.getLimitCount() < -1) {
            conf.setLimitCount(-1);
        }
        if (conf.getLimitGroup() < -1) {
            conf.setLimitGroup(-1);
        }
        if (conf.getLimitGroupCount() < -1) {
            conf.setLimitGroupCount(-1);
        }

        // 请求时间
        conf.setReqTime(System.currentTimeMillis());

        // 修整参数（最小时间窗口为1000毫秒）
        if (conf.getTimeWindow() < 1000) {
            conf.setTimeWindow(1000);
        }
        // 最小桶大小为100毫秒，且必须能整除时间窗口
        if (conf.getBucketSize() < 100) {
            conf.setBucketSize(100);
        }

        // 计算窗口时间范围
        this.calcWindowTimeRange(conf);

        // 资源名称
        conf.setResName(this.getResName(conf, m));
        // 分组名称
        conf.setGroupName(this.getGroupName(conf, m, req));

        return conf;
    }

    /**
     * 计算时间窗口起始时间
     * 根据是否动态时间窗口选择计算方式
     */
    private void calcWindowTimeRange(RateLimitConf rlConf) {
        if (rlConf.isDynamicTimeWindow()) {
            rlConf.setWindowFrom(rlConf.getReqTime() - rlConf.getTimeWindow());
            rlConf.setWindowTo(rlConf.getReqTime());
        } else {
            rlConf.setWindowFrom(rlConf.getReqTime() / rlConf.getTimeWindow() * rlConf.getTimeWindow());
            rlConf.setWindowTo(rlConf.getWindowFrom() + rlConf.getTimeWindow());
        }
    }

    /**
     * 获取目标资源的名称（一般为接口方法的全限定名称或者自定义名称）
     */
    private String getResName(RateLimitConf rlConf, Method m) {
        return rlConf.getName() == null || rlConf.getName().trim().isEmpty() ?
                m.getDeclaringClass().getName() + "." + m.getName() : rlConf.getName();
    }

    /**
     * 滑动窗口实现（环形桶）
     */
    private static class SlidingWindow {
        private final int windowSizeMs;   // 整体窗口大小
        private final int bucketSizeMs;   // 每个桶的时间长度
        private final int bucketCount;     // 桶数量
        private final AtomicInteger[] buckets; // 每个桶计数
        private volatile long lastUpdateTime;  // 最后更新时间（毫秒）
        private volatile long lastGlobalBucketIndex; // 最后更新全局桶索引

        @Getter
        @Setter
        private volatile long lastReqTime;

        private String logFlag;

        public SlidingWindow(RateLimitConf rlConf, String logFlag) {
            this.windowSizeMs = rlConf.getTimeWindow();
            this.bucketSizeMs = rlConf.getBucketSize();
            // 最好能整除，否则行为未知
            this.bucketCount = this.windowSizeMs / this.bucketSizeMs;
            this.buckets = new AtomicInteger[bucketCount];
            for (int i = 0; i < bucketCount; i++) {
                buckets[i] = new AtomicInteger(0);
            }
            this.lastUpdateTime = System.currentTimeMillis();
            this.lastGlobalBucketIndex = lastUpdateTime / this.bucketSizeMs;

            // 上次请求时间
            this.lastReqTime = System.currentTimeMillis();

            this.logFlag = logFlag;
        }

        public synchronized boolean tryAcquire(int limitCount, long reqTime) {
            this.setLastReqTime(reqTime);
            this.slideWindow(reqTime);

            if (limitCount >= 0 && this.calcTotalCount() >= limitCount) {
                return false;
            }
            return true;
        }

        public void increase(long reqTime) {
            // 当前时间对应的桶++
            int index = (int) (reqTime / bucketSizeMs % bucketCount);
            buckets[index].incrementAndGet();
            // print buckets
            log.debug("{} buckets: {}", logFlag, Arrays.toString(buckets));
        }

        /**
         * 滑动窗口更新：清理过期桶
         */
        private void slideWindow(long reqTime) {
            // 计算请求时间的全局桶索引
            long globalBucketIndex = reqTime / bucketSizeMs;
            long globalBucketsPassed = globalBucketIndex - lastGlobalBucketIndex;

            // log
            log.debug("{} slideWindow: reqTime={}, globalBucketIndex={}, "
                            + "globalBucketsPassed={}, lastGlobalBucketIndex={}",
                    logFlag, reqTime, globalBucketIndex, globalBucketsPassed, lastGlobalBucketIndex);

            // 如果时间没有过去一个桶的大小，直接返回
            if (globalBucketsPassed <= 0) {
                log.debug("{}, no need to slide", logFlag);
                return;
            }

            // 如果时间过去超过所有桶，则全部过期
            if (globalBucketsPassed >= bucketCount) {
                log.debug("{} clear all", logFlag);
                for (AtomicInteger b : buckets) {
                    b.set(0);
                }
            } else {
                // 从lastBucketIndex后一个开始正向清除bucketsPassed个桶
                for (int i = 1; i <= globalBucketsPassed; i++) {
                    int index = (int) ((lastGlobalBucketIndex + i) % bucketCount);
                    log.debug("{} clear index: {}", logFlag, index);
                    buckets[index].set(0);
                }
            }

            lastUpdateTime = reqTime;
            lastGlobalBucketIndex = reqTime / bucketSizeMs;
            log.debug("{} slideWindow: lastUpdateTime={}, lastGlobalBucketIndex={}",
                    logFlag, lastUpdateTime, lastGlobalBucketIndex);
        }

        /**
         * 统计所有桶总和
         */
        private int calcTotalCount() {
            int total = 0;
            for (AtomicInteger b : buckets) {
                total += b.get();
            }
            return total;
        }
    }

    /**
     * 清理过期的 group，防止 innerGroupHolder 无限制增长
     */
    private void cleanExpiredGroups() {
        long now = System.currentTimeMillis();

        try {
            log.debug("[RateLimitCleaner] 开始清理过期 group... now={}", now);

            innerGroupHolder.forEach((resName, groupMap) -> {
                // 遍历 groupMap，但删除时使用 CAS，不直接 removeIf
                for (Map.Entry<String, SlidingWindow> entry : groupMap.entrySet()) {
                    String group = entry.getKey();
                    SlidingWindow window = entry.getValue();

                    long lastReq = window.getLastReqTime();
                    boolean expired = (now - lastReq > GROUP_EXPIRE_MS) && (now - lastReq > window.windowSizeMs);
                    log.debug("[RateLimitCleaner] group={} (resName={}) lastReq={}, expiredMs={}, "
                                    + "GROUP_EXPIRE_MS={}, windowSizeMs={}",
                            group, resName, lastReq, now - lastReq, GROUP_EXPIRE_MS, window.windowSizeMs);
                    if (expired) {
                        // CAS 删除：确保删除的是当前 value，防止误删
                        boolean removed = groupMap.remove(group, window);
                        if (removed) {
                            log.debug("[RateLimitCleaner] 已清理 group={} (resName={}) lastReq={}, expiredMs={}",
                                    group, resName, lastReq, now - lastReq);
                        }
                    }
                }
            });

        } catch (Exception e) {
            log.error("[RateLimitCleaner] 清理异常", e);
        }
    }

    /**
     * 获取分组名称
     */
    private String getGroupName(RateLimitConf rlConf, Method m, HttpServletRequest request) {
        switch (rlConf.getGroupType()) {
            case IP:
                return request.getRemoteAddr();
            case CUSTOM:
                return this.getCustomGroupName(rlConf.getCustomGroup(), request, m);
            case NONE:
            default:
                return String.valueOf(rlConf.getWindowFrom());
        }
    }

    /**
     * 获取自定义分组名称
     * 支持：
     * 1. 普通字符串（不以 # 开头），直接返回
     * 2. SpEL 表达式（以 # 开头），解析返回
     */
    private String getCustomGroupName(String customGroup, HttpServletRequest request, Method method) {
        if (customGroup == null || customGroup.trim().isEmpty()) {
            log.warn("自定义分组名称不能为空");
            throw new RuntimeException("自定义分组名称不能为空");
        }

        // 1. 普通字符串，直接返回
        if (!customGroup.trim().startsWith("#")) {
            return customGroup;
        }

        // 2. SpEL 表达式 —— 动态解析
        try {
            // 上下文变量，可扩展
            CONTEXT.setVariable("request", request);
            CONTEXT.setVariable("method", method);

            // rls = Rate Limit Service
            CONTEXT.setVariable("rls", rateLimitService);

            Object val = PARSER.parseExpression(customGroup).getValue(CONTEXT);
            if (val == null) {
                log.warn("解析分组名称失败");
                throw new RuntimeException("解析分组名称失败");
            }
            log.debug("SpEL 解析分组名称成功：{}", val);
            return String.valueOf(val);
        } catch (Exception e) {
            log.warn("SpEL 解析分组名称失败");
            throw new RuntimeException("解析分组名称失败");
        }
    }
}

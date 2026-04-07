package com.aikey.gateway.auth;

import com.aikey.entity.VirtualKey;

/**
 * 虚拟Key认证上下文
 *
 * <p>使用ThreadLocal在请求生命周期内传递已认证的VirtualKey对象，
 * 供后续的限流、额度、调度等服务使用。</p>
 *
 * <p>必须在过滤器的finally块中调用clear()防止内存泄漏。</p>
 */
public class VirtualKeyAuthContext {

    private static final ThreadLocal<VirtualKey> CONTEXT = new ThreadLocal<>();

    public static void set(VirtualKey virtualKey) {
        CONTEXT.set(virtualKey);
    }

    public static VirtualKey get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

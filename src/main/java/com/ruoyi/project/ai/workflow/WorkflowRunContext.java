package com.ruoyi.project.ai.workflow;

import lombok.Builder;
import lombok.Data;

/**
 * 当前工作流执行上下文，用于工具调用日志关联。
 */
public final class WorkflowRunContext {

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private WorkflowRunContext() {
    }

    public static Context get() {
        return CONTEXT.get();
    }

    public static void set(Context context) {
        CONTEXT.set(context);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    @Data
    @Builder
    public static class Context {
        private Long executionId;
        private Long stepRunId;
        private String workflowKey;
        private String stepKey;
    }
}

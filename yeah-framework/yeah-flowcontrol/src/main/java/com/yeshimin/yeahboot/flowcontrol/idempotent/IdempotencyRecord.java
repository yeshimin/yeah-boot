package com.yeshimin.yeahboot.flowcontrol.idempotent;

import lombok.Data;

/**
 * 幂等提交在 Redis 中保存的状态记录。
 */
@Data
class IdempotencyRecord {

    /** 首次请求控制器参数的固定长度摘要。 */
    private String requestHash;

    /** 当前提交意图的处理状态。 */
    private Status status;

    /** 幂等状态机的有限状态。 */
    enum Status {
        /** 已抢占幂等键，首次请求正在执行业务。 */
        PROCESSING,

        /** 首次请求已成功完成，后续相同请求不再重复执行业务。 */
        COMPLETED
    }
}

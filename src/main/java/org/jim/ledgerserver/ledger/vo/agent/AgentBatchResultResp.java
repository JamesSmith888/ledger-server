package org.jim.ledgerserver.ledger.vo.agent;

import java.util.List;

/**
 * Agent 专用的批量操作结果响应
 * 
 * @author James Smith
 */
public record AgentBatchResultResp(
        int successCount,
        int failedCount,
        List<AgentTransactionResp> successItems,
        List<FailedItem> failedItems,
        String message
) {
    /**
     * 失败项
     */
    public record FailedItem(
            int index,
            String name,
            String reason
    ) {}
}

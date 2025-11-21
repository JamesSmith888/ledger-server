package org.jim.ledgerserver.ledger.vo;

import java.util.List;

/**
 * 批量更新模板排序请求
 * @author James Smith
 */
public record TemplateSortOrderReq(
        List<Long> templateIds
) {
}

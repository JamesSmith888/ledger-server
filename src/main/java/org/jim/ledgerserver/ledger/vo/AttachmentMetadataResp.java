package org.jim.ledgerserver.ledger.vo;

import java.time.LocalDateTime;

/**
 * 附件信息响应VO（用于列表展示，不包含文件数据）
 * @author James Smith
 */
public record AttachmentMetadataResp(
        Long id,
        Long transactionId,
        String fileName,
        String fileType,
        Long fileSize,
        Integer width,
        Integer height,
        Long uploadedByUserId,
        LocalDateTime createTime,
        boolean hasThumbnail
) {
}

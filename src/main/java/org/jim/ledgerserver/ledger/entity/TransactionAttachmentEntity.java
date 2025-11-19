package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

/**
 * 交易附件实体类
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "transaction_attachment")
public class TransactionAttachmentEntity extends BaseEntity {

    /**
     * 关联的交易ID
     */
    private Long transactionId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型（MIME type）
     * 例如：image/jpeg, image/png, application/pdf
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件数据
     * 使用 MEDIUMBLOB 存储，最大 16MB
     */
    @Lob
    private byte[] fileData;

    /**
     * 缩略图数据（仅图片类型）
     * 用于列表显示，减少数据传输
     */
    @Lob
    private byte[] thumbnailData;

    /**
     * 图片宽度（仅图片类型）
     */
    private Integer width;

    /**
     * 图片高度（仅图片类型）
     */
    private Integer height;

    /**
     * 上传该附件的用户ID
     */
    private Long uploadedByUserId;

}

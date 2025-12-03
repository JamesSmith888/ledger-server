package org.jim.ledgerserver.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

/**
 * 用户头像实体类
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "user_avatar")
public class UserAvatarEntity extends BaseEntity {

    /**
     * 关联的用户ID
     */
    private Long userId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型（MIME type）
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件数据
     */
    @Lob
    private byte[] fileData;

}

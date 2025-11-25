package org.jim.ledgerserver.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

import java.time.LocalDateTime;

/**
 * 用户基本信息
 *
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "user")
public class UserEntity extends BaseEntity {

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像URL
     */
    private String avatarUrl;

    /**
     * 用户邮箱（用于登录）
     */
    private String email;

    /**
     * 用户电话（用于登录）
     */
    private String phone;

    /**
     * 用户名（用于登录）
     */
    private String username;

    /**
     * 用户密码（加密存储）
     */
    private String password;

    /**
     * 性别。1-男，2-女，3-中性，4-保密
     */
    private Integer gender;

    /**
     * 用户类型。
     */
    private Integer userType;

    /**
     * 用户状态。1-正常(默认)，2-禁言，3-封号
     */
    @Column(name = "status", columnDefinition = "int default 1")
    private Integer status;

    /**
     * 注册IP地址
     */
    private String registerIp;

    /**
     * 上次登录IP地址
     */
    private String lastLoginIp;

    /**
     * 上次登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 默认账本ID
     */
    private Long defaultLedgerId;

    /**
     * 用户角色：USER-普通用户，ADMIN-管理员
     */
    @Column(name = "role", columnDefinition = "varchar(20) default 'USER'")
    private String role;

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    /**
     * 判断是否为普通用户
     */
    public boolean isUser() {
        return "USER".equals(this.role);
    }

}

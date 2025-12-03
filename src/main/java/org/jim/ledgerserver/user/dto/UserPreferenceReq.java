package org.jim.ledgerserver.user.dto;

import org.jim.ledgerserver.user.enums.PreferenceType;

/**
 * 创建/更新用户偏好请求
 *
 * @author James Smith
 */
public record UserPreferenceReq(
        PreferenceType type,
        String keyword,
        String correction,
        String note,
        Long categoryId
) {}

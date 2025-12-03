package org.jim.ledgerserver.user.dto;

import java.util.List;

/**
 * 批量同步用户偏好请求
 *
 * @author James Smith
 */
public record UserPreferenceSyncReq(
        List<UserPreferenceReq> preferences
) {}

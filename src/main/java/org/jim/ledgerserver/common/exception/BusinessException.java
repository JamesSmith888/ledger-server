package org.jim.ledgerserver.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 * @author James Smith
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    /**
     * 默认错误码为400
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /**
     * 自定义错误码
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

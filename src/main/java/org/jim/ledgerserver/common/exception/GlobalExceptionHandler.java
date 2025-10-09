package org.jim.ledgerserver.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.jim.ledgerserver.common.JSONResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * @author James Smith
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * @param e 业务异常
     * @return 统一返回结果
     */
    @ExceptionHandler(BusinessException.class)
    public JSONResult<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return JSONResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理其他未知异常
     * @param e 异常
     * @return 统一返回结果
     */
    @ExceptionHandler(Exception.class)
    public JSONResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return JSONResult.error(500, "系统异常，请联系管理员");
    }
}

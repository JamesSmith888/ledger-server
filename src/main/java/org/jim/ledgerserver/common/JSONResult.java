package org.jim.ledgerserver.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author James Smith
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class JSONResult<T> {
    private int code;

    private String message;

    private T data;


    public static <T> JSONResult<T> success() {
        return new JSONResult<>(200, "Success", null);
    }

    public static <T> JSONResult<T> success(T data) {
        return new JSONResult<>(200, "Success", data);
    }

    public static <T> JSONResult<T> success(String message, T data) {
        return new JSONResult<>(200, message, data);
    }

    public static <T> JSONResult<T> error(int code, String message) {
        return new JSONResult<>(code, message, null);
    }

    /**
     * 失败结果，错误码400
     * @param message 错误信息
     * @return JSONResult
     */
    public static <T> JSONResult<T> fail(String message) {
        return new JSONResult<>(400, message, null);
    }

}

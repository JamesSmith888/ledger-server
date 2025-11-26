package org.jim.ledgerserver.ledger.vo.export;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 导出结果
 * 
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class ExportResult {
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 导出的数据（JSON格式时使用）
     */
    private Object data;
    
    /**
     * Base64编码的数据（Excel等二进制格式时使用）
     */
    private String base64Data;
    
    /**
     * 建议的文件名
     */
    private String fileName;
    
    /**
     * 创建成功结果
     */
    public static ExportResult success(Object data) {
        return new ExportResult()
                .setSuccess(true)
                .setMessage("导出成功")
                .setData(data);
    }
    
    /**
     * 创建成功结果（带Base64数据）
     */
    public static ExportResult successWithBase64(String base64Data, String fileName) {
        return new ExportResult()
                .setSuccess(true)
                .setMessage("导出成功")
                .setBase64Data(base64Data)
                .setFileName(fileName);
    }
    
    /**
     * 创建失败结果
     */
    public static ExportResult fail(String message) {
        return new ExportResult()
                .setSuccess(false)
                .setMessage(message);
    }
}

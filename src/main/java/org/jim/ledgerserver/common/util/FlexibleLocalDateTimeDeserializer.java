package org.jim.ledgerserver.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 灵活的 LocalDateTime 反序列化器
 * 支持多种日期时间格式：
 * - ISO 8601 带 Z 后缀: 2025-11-27T07:10:30.149Z
 * - ISO 8601 带时区: 2025-11-27T07:10:30+08:00
 * - 标准格式: 2025-11-27T07:10:30
 * - 中文常用格式: 2025-11-27 07:10:30
 * 
 * @author James Smith
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter STANDARD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_LOCAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        text = text.trim();
        
        try {
            // 1. 尝试解析 ISO 8601 带 Z 后缀的 UTC 时间
            if (text.endsWith("Z")) {
                Instant instant = Instant.parse(text);
                return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            }
            
            // 2. 尝试解析带时区偏移的 ISO 8601 格式 (如 2025-11-27T07:10:30+08:00)
            if (text.contains("+") || (text.contains("-") && text.lastIndexOf("-") > 10)) {
                try {
                    return LocalDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                } catch (DateTimeParseException ignored) {
                    // 继续尝试其他格式
                }
            }
            
            // 3. 尝试解析标准 ISO LocalDateTime 格式 (如 2025-11-27T07:10:30)
            if (text.contains("T")) {
                return LocalDateTime.parse(text, ISO_LOCAL_FORMATTER);
            }
            
            // 4. 尝试解析中文常用格式 (如 2025-11-27 07:10:30)
            return LocalDateTime.parse(text, STANDARD_FORMATTER);
            
        } catch (DateTimeParseException e) {
            throw new IOException("无法解析日期时间: " + text + ", 支持的格式: " +
                    "yyyy-MM-dd HH:mm:ss, yyyy-MM-ddTHH:mm:ss, ISO 8601 (带Z或时区)", e);
        }
    }
}

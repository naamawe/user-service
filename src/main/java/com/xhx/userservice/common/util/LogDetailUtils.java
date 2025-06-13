package com.xhx.userservice.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 此工具类由AI生成
 * author: ChatGPT
 */
public class LogDetailUtils {

    private static final Logger logger = LoggerFactory.getLogger(LogDetailUtils.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 传入多个 key-value 字段，返回序列化的 JSON 字符串。
     * 出异常返回简单字符串（fallback）。
     *
     * @param detailMap 详情字段
     * @param fallback  序列化失败时返回的字符串
     * @return JSON字符串 或 fallback
     */
    public static String buildDetailJson(Map<String, Object> detailMap, String fallback) {
        try {
            return OBJECT_MAPPER.writeValueAsString(detailMap);
        } catch (Exception e) {
            logger.error("日志详情序列化失败", e);
            return fallback;
        }
    }

    /**
     * 传入 message 和额外字段，自动帮你组装 Map，序列化 JSON
     * @param message   主消息，比如“用户注册成功”
     * @param extraFields 额外字段（比如用户名等）
     * @return JSON字符串 或 简单消息字符串
     */
    public static String buildDetailJson(String message, Map<String, Object> extraFields) {
        try {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("message", message);
            if (extraFields != null && !extraFields.isEmpty()) {
                map.putAll(extraFields);
            }
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            logger.error("日志详情序列化失败", e);
            // 简单降级成 message 字符串
            return message;
        }
    }
}

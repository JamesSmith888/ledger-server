package org.jim.ledgerserver.completion.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.completion.dto.CompletionPhraseDTO;
import org.jim.ledgerserver.completion.dto.CompletionPhraseRequest;
import org.jim.ledgerserver.completion.dto.CompletionQueryResponse;
import org.jim.ledgerserver.completion.service.CompletionPhraseService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 智能补全 API
 * 
 * 提供高频短语的存储和检索功能
 * 设计目标：低延迟、高命中率
 *
 * @author James Smith
 */
@RestController
@RequestMapping("/api/completion")
public class CompletionController {

    @Resource
    private CompletionPhraseService phraseService;

    /**
     * 查询补全结果
     * 
     * 根据用户输入的前缀，返回匹配的补全候选
     * 优化点：
     * 1. 优先从内存缓存中匹配
     * 2. 返回补全部分（prefix 之后的内容）
     * 
     * @param prefix 用户输入的前缀（至少1个字符）
     * @return 补全候选列表
     */
    @GetMapping("/query")
    public JSONResult<CompletionQueryResponse> query(@RequestParam String prefix) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        if (prefix == null || prefix.isEmpty()) {
            return JSONResult.fail("前缀不能为空");
        }

        CompletionQueryResponse response = phraseService.queryCompletions(userId, prefix);
        return JSONResult.success(response);
    }

    /**
     * 添加或更新短语
     * 
     * 当用户发送消息后调用，记录用户的输入
     * 如果短语已存在，则增加使用频率
     * 
     * @param request 短语信息
     * @return 保存后的短语
     */
    @PostMapping("/phrase")
    public JSONResult<CompletionPhraseDTO> addPhrase(@Valid @RequestBody CompletionPhraseRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        CompletionPhraseDTO dto = phraseService.addOrUpdatePhrase(userId, request);
        return JSONResult.success(dto);
    }

    /**
     * 获取高频短语（用于客户端缓存初始化）
     * 
     * 客户端启动时调用，获取 top N 高频短语
     * 存入本地缓存，实现离线快速补全
     * 
     * @param limit 返回数量，默认 50，最大 100
     * @return 高频短语列表
     */
    @GetMapping("/phrases/top")
    public JSONResult<List<CompletionPhraseDTO>> getTopPhrases(
            @RequestParam(defaultValue = "50") int limit) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        limit = Math.min(limit, 100);
        List<CompletionPhraseDTO> phrases = phraseService.getTopPhrases(userId, limit);
        return JSONResult.success(phrases);
    }

    /**
     * 增量同步短语
     * 
     * 获取某时间之后更新的短语，用于增量同步
     * 
     * @param since 时间戳（毫秒）
     * @return 更新的短语列表
     */
    @GetMapping("/phrases/sync")
    public JSONResult<List<CompletionPhraseDTO>> sync(@RequestParam long since) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        LocalDateTime sinceTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(since), 
            ZoneId.systemDefault()
        );
        
        List<CompletionPhraseDTO> phrases = phraseService.getUpdatedSince(userId, sinceTime);
        return JSONResult.success(phrases);
    }

    /**
     * 刷新用户缓存
     * 
     * 手动触发缓存刷新（通常不需要调用）
     */
    @PostMapping("/cache/refresh")
    public JSONResult<Void> refreshCache() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        phraseService.refreshUserCache(userId);
        return JSONResult.success(null);
    }
}

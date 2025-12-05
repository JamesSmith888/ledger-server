package org.jim.ledgerserver.completion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jim.ledgerserver.completion.dto.CompletionPhraseDTO;
import org.jim.ledgerserver.completion.dto.CompletionPhraseRequest;
import org.jim.ledgerserver.completion.dto.CompletionQueryResponse;
import org.jim.ledgerserver.completion.dto.CompletionQueryResponse.CompletionResult;
import org.jim.ledgerserver.completion.entity.CompletionPhraseEntity;
import org.jim.ledgerserver.completion.repository.CompletionPhraseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 补全短语服务
 * 
 * 核心功能：
 * 1. 高频短语存储与检索
 * 2. LRU 淘汰策略（每用户最多存储 200 条）
 * 3. 内存缓存加速查询
 *
 * @author James Smith
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompletionPhraseService {

    private final CompletionPhraseRepository repository;

    /**
     * 每用户最大短语数量
     */
    private static final int MAX_PHRASES_PER_USER = 200;

    /**
     * 查询返回最大数量
     */
    private static final int MAX_QUERY_RESULTS = 5;

    /**
     * 内存缓存：userId -> 高频短语列表
     * 生产环境应使用 Redis 等分布式缓存
     */
    private final ConcurrentHashMap<Long, List<CompletionPhraseDTO>> userPhraseCache = new ConcurrentHashMap<>();

    /**
     * 根据前缀查询补全结果
     * 
     * 性能优化：
     * 1. 先查内存缓存
     * 2. 缓存未命中再查数据库
     */
    public CompletionQueryResponse queryCompletions(Long userId, String prefix) {
        long startTime = System.currentTimeMillis();
        
        if (prefix == null || prefix.length() < 1) {
            return new CompletionQueryResponse()
                .setPrefix(prefix)
                .setResults(List.of())
                .setFromCache(false)
                .setQueryTimeMs(0L);
        }

        // 优先从缓存中匹配
        List<CompletionPhraseDTO> cachedPhrases = userPhraseCache.get(userId);
        boolean fromCache = cachedPhrases != null;

        List<CompletionResult> results;
        if (fromCache) {
            // 从缓存中前缀匹配
            results = cachedPhrases.stream()
                .filter(p -> p.getPhrase().startsWith(prefix) && !p.getPhrase().equals(prefix))
                .sorted((a, b) -> {
                    // 按频率降序，最后使用时间降序
                    int freqCompare = b.getFrequency().compareTo(a.getFrequency());
                    if (freqCompare != 0) return freqCompare;
                    return b.getLastUsedAt().compareTo(a.getLastUsedAt());
                })
                .limit(MAX_QUERY_RESULTS)
                .map(p -> new CompletionResult()
                    .setPhrase(p.getPhrase())
                    .setCompletion(p.getPhrase().substring(prefix.length()))
                    .setScore(calculateScore(p))
                    .setSourceType(p.getSourceType()))
                .collect(Collectors.toList());
        } else {
            // 从数据库查询
            List<CompletionPhraseEntity> entities = repository.findByUserIdAndPhrasePrefix(
                userId, 
                prefix, 
                PageRequest.of(0, MAX_QUERY_RESULTS)
            );
            
            results = entities.stream()
                .filter(e -> !e.getPhrase().equals(prefix)) // 排除完全相同的
                .map(e -> new CompletionResult()
                    .setPhrase(e.getPhrase())
                    .setCompletion(e.getPhrase().substring(prefix.length()))
                    .setScore(calculateScore(e))
                    .setSourceType(e.getSourceType()))
                .collect(Collectors.toList());
        }

        long queryTime = System.currentTimeMillis() - startTime;
        log.debug("Completion query for user {} with prefix '{}': {} results in {}ms (cache: {})",
            userId, prefix, results.size(), queryTime, fromCache);

        return new CompletionQueryResponse()
            .setPrefix(prefix)
            .setResults(results)
            .setFromCache(fromCache)
            .setQueryTimeMs(queryTime);
    }

    /**
     * 添加或更新短语
     */
    @Transactional
    public CompletionPhraseDTO addOrUpdatePhrase(Long userId, CompletionPhraseRequest request) {
        String phrase = request.getPhrase().trim();
        
        // 检查是否已存在
        Optional<CompletionPhraseEntity> existing = repository
            .findByUserIdAndPhraseAndDeleteTimeIsNull(userId, phrase);

        CompletionPhraseEntity entity;
        if (existing.isPresent()) {
            // 已存在，增加频率
            entity = existing.get();
            repository.incrementFrequency(entity.getId(), System.currentTimeMillis());
            entity.setFrequency(entity.getFrequency() + 1);
            entity.setLastUsedAt(System.currentTimeMillis());
        } else {
            // 不存在，检查是否需要淘汰旧数据
            long count = repository.countByUserIdAndDeleteTimeIsNull(userId);
            if (count >= MAX_PHRASES_PER_USER) {
                // 淘汰最不常用的短语
                int toDelete = (int) (count - MAX_PHRASES_PER_USER + 1);
                repository.deleteOldestPhrases(userId, toDelete);
                log.info("Evicted {} old phrases for user {}", toDelete, userId);
            }

            // 创建新短语
            entity = new CompletionPhraseEntity()
                .setUserId(userId)
                .setPhrase(phrase)
                .setPhrasePrefix(phrase.substring(0, Math.min(10, phrase.length())))
                .setFrequency(1)
                .setLastUsedAt(System.currentTimeMillis())
                .setSourceType(request.getSourceType())
                .setCategory(request.getCategory());
            
            entity = repository.save(entity);
        }

        // 刷新缓存
        refreshUserCache(userId);

        return toDTO(entity);
    }

    /**
     * 批量添加短语（用于初始化预设短语）
     */
    @Transactional
    public void addPresetPhrases(Long userId, List<String> phrases) {
        for (String phrase : phrases) {
            CompletionPhraseRequest request = new CompletionPhraseRequest()
                .setPhrase(phrase)
                .setSourceType("PRESET");
            addOrUpdatePhrase(userId, request);
        }
    }

    /**
     * 获取用户的高频短语（用于客户端缓存初始化）
     */
    public List<CompletionPhraseDTO> getTopPhrases(Long userId, int limit) {
        List<CompletionPhraseEntity> entities = repository.findTopPhrasesByUserId(
            userId, 
            PageRequest.of(0, Math.min(limit, MAX_PHRASES_PER_USER))
        );
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取自某时间后更新的短语（用于增量同步）
     */
    public List<CompletionPhraseDTO> getUpdatedSince(Long userId, LocalDateTime since) {
        return repository.findUpdatedSince(userId, since).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 刷新用户缓存
     */
    public void refreshUserCache(Long userId) {
        List<CompletionPhraseDTO> topPhrases = getTopPhrases(userId, 100); // 缓存 top 100
        userPhraseCache.put(userId, topPhrases);
        log.debug("Refreshed cache for user {}: {} phrases", userId, topPhrases.size());
    }

    /**
     * 清除用户缓存
     */
    public void clearUserCache(Long userId) {
        userPhraseCache.remove(userId);
    }

    /**
     * 计算热度分数
     * 公式：frequency * decay(lastUsedAt)
     * decay 根据时间衰减，7天前的权重减半
     */
    private double calculateScore(CompletionPhraseDTO dto) {
        long daysSinceLastUse = (System.currentTimeMillis() - dto.getLastUsedAt()) / (1000 * 60 * 60 * 24);
        double decay = Math.pow(0.9, daysSinceLastUse); // 每天衰减 10%
        return dto.getFrequency() * decay;
    }

    private double calculateScore(CompletionPhraseEntity entity) {
        long daysSinceLastUse = (System.currentTimeMillis() - entity.getLastUsedAt()) / (1000 * 60 * 60 * 24);
        double decay = Math.pow(0.9, daysSinceLastUse);
        return entity.getFrequency() * decay;
    }

    private CompletionPhraseDTO toDTO(CompletionPhraseEntity entity) {
        return new CompletionPhraseDTO()
            .setId(entity.getId())
            .setPhrase(entity.getPhrase())
            .setFrequency(entity.getFrequency())
            .setLastUsedAt(entity.getLastUsedAt())
            .setSourceType(entity.getSourceType())
            .setCategory(entity.getCategory());
    }
}

package org.jim.ledgerserver.ledger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.enums.LedgerTypeEnum;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.ledger.entity.*;
import org.jim.ledgerserver.ledger.repository.*;
import org.jim.ledgerserver.ledger.vo.export.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据导出服务
 * 提供用户数据导出功能，支持JSON、CSV、Excel格式
 * 
 * @author James Smith
 */
@Service
public class ExportService {

    @Resource
    private TransactionRepository transactionRepository;

    @Resource
    private CategoryRepository categoryRepository;

    @Resource
    private PaymentMethodRepository paymentMethodRepository;

    @Resource
    private LedgerRepository ledgerRepository;

    @Resource
    private LedgerMemberRepository ledgerMemberRepository;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 导出数据
     * 
     * @param userId 用户ID
     * @param request 导出请求
     * @return 导出结果
     */
    @Transactional(readOnly = true)
    public ExportResult exportData(Long userId, ExportDataReq request) {
        String format = request.format();
        String dataType = request.dataType();

        // 根据数据类型获取数据
        Object exportData = switch (dataType) {
            case "ALL" -> exportAllData(userId, request);
            case "TRANSACTIONS" -> exportTransactions(userId, request);
            case "CATEGORIES" -> exportCategories(userId);
            case "PAYMENT_METHODS" -> exportPaymentMethods(userId);
            case "LEDGERS" -> exportLedgers(userId);
            default -> throw new IllegalArgumentException("不支持的数据类型: " + dataType);
        };

        // 根据格式返回结果
        return switch (format) {
            case "JSON" -> ExportResult.success(exportData);
            case "CSV" -> {
                String csvData = convertToCsv(exportData, dataType);
                yield ExportResult.success(csvData);
            }
            case "EXCEL" -> {
                String base64Data = convertToExcel(exportData, dataType);
                String fileName = generateFileName("xlsx");
                yield ExportResult.successWithBase64(base64Data, fileName);
            }
            default -> throw new IllegalArgumentException("不支持的导出格式: " + format);
        };
    }

    /**
     * 获取导出预览信息
     */
    @Transactional(readOnly = true)
    public ExportPreviewResp getExportPreview(Long userId, ExportDataReq request) {
        Long ledgerId = request.ledgerId();
        
        // 获取用户可访问的账本ID列表
        List<Long> accessibleLedgerIds = getAccessibleLedgerIds(userId);
        
        // 如果指定了账本，验证权限
        if (ledgerId != null && !accessibleLedgerIds.contains(ledgerId)) {
            throw new IllegalArgumentException("无权访问指定账本");
        }

        List<Long> targetLedgerIds = ledgerId != null 
            ? Collections.singletonList(ledgerId) 
            : accessibleLedgerIds;

        // 统计各类数据数量
        long transactionCount = countTransactions(userId, targetLedgerIds);
        // 用户分类 + 系统分类
        long categoryCount = categoryRepository.findByCreatedByUserIdAndDeleteTimeIsNull(userId).size()
                + categoryRepository.findByIsSystemTrueAndDeleteTimeIsNull().size();
        long paymentMethodCount = paymentMethodRepository.findByUserIdAndDeleteTimeIsNull(userId).size();
        long ledgerCount = targetLedgerIds.size();

        // 估算文件大小（粗略估计）
        long estimatedBytes = transactionCount * 200 + categoryCount * 100 + 
                              paymentMethodCount * 80 + ledgerCount * 150;
        String estimatedSize = formatFileSize(estimatedBytes);

        return new ExportPreviewResp()
                .setTransactionCount(transactionCount)
                .setCategoryCount(categoryCount)
                .setPaymentMethodCount(paymentMethodCount)
                .setLedgerCount(ledgerCount)
                .setEstimatedSize(estimatedSize);
    }

    /**
     * 导出所有数据
     */
    private ExportFullData exportAllData(Long userId, ExportDataReq request) {
        ExportFullData fullData = new ExportFullData();
        fullData.setExportTime(LocalDateTime.now());
        fullData.setUserId(userId);

        // 导出账本
        List<ExportLedgerData> ledgers = exportLedgers(userId);
        fullData.setLedgers(ledgers);

        // 导出交易
        List<ExportTransactionData> transactions = exportTransactions(userId, request);
        fullData.setTransactions(transactions);

        // 导出分类
        List<ExportCategoryData> categories = exportCategories(userId);
        fullData.setCategories(categories);

        // 导出支付方式
        List<ExportPaymentMethodData> paymentMethods = exportPaymentMethods(userId);
        fullData.setPaymentMethods(paymentMethods);

        // 设置统计信息
        ExportFullData.ExportStatistics statistics = new ExportFullData.ExportStatistics();
        statistics.setLedgerCount(ledgers.size());
        statistics.setTransactionCount(transactions.size());
        statistics.setCategoryCount(categories.size());
        statistics.setPaymentMethodCount(paymentMethods.size());
        fullData.setStatistics(statistics);

        return fullData;
    }

    /**
     * 导出交易记录
     */
    private List<ExportTransactionData> exportTransactions(Long userId, ExportDataReq request) {
        // 获取用户可访问的账本
        List<Long> accessibleLedgerIds = getAccessibleLedgerIds(userId);
        
        Long ledgerId = request.ledgerId();
        if (ledgerId != null) {
            if (!accessibleLedgerIds.contains(ledgerId)) {
                throw new IllegalArgumentException("无权访问指定账本");
            }
            accessibleLedgerIds = Collections.singletonList(ledgerId);
        }
        
        // 用于Lambda表达式的final变量
        final List<Long> finalAccessibleLedgerIds = accessibleLedgerIds;

        // 批量获取账本、分类、支付方式信息（避免N+1查询）
        Map<Long, LedgerEntity> ledgerMap = ledgerRepository.findAllById(finalAccessibleLedgerIds)
                .stream().collect(Collectors.toMap(LedgerEntity::getId, l -> l));
        
        Map<Long, CategoryEntity> categoryMap = getAllCategoriesForUser(userId);
        Map<Long, PaymentMethodEntity> paymentMethodMap = paymentMethodRepository
                .findByUserIdAndDeleteTimeIsNull(userId)
                .stream().collect(Collectors.toMap(PaymentMethodEntity::getId, p -> p));

        // 查询交易记录
        List<TransactionEntity> transactions = transactionRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            
            // 只查询未删除的
            predicates.add(cb.isNull(root.get("deleteTime")));
            
            // 账本筛选
            if (!finalAccessibleLedgerIds.isEmpty()) {
                predicates.add(root.get("ledgerId").in(finalAccessibleLedgerIds));
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });

        // 转换为导出格式
        return transactions.stream()
                .map(tx -> convertToExportTransaction(tx, ledgerMap, categoryMap, paymentMethodMap))
                .collect(Collectors.toList());
    }

    /**
     * 导出分类
     */
    private List<ExportCategoryData> exportCategories(Long userId) {
        // 获取用户分类和系统分类
        List<CategoryEntity> userCategories = categoryRepository
                .findByCreatedByUserIdAndDeleteTimeIsNull(userId);
        List<CategoryEntity> systemCategories = categoryRepository
                .findByIsSystemTrueAndDeleteTimeIsNull();

        // 合并分类列表
        List<CategoryEntity> allCategories = new ArrayList<>();
        allCategories.addAll(userCategories);
        allCategories.addAll(systemCategories);

        return allCategories.stream()
                .map(this::convertToExportCategory)
                .collect(Collectors.toList());
    }

    /**
     * 导出支付方式
     */
    private List<ExportPaymentMethodData> exportPaymentMethods(Long userId) {
        List<PaymentMethodEntity> paymentMethods = paymentMethodRepository
                .findByUserIdAndDeleteTimeIsNull(userId);

        return paymentMethods.stream()
                .map(this::convertToExportPaymentMethod)
                .collect(Collectors.toList());
    }

    /**
     * 导出账本
     */
    private List<ExportLedgerData> exportLedgers(Long userId) {
        // 获取用户拥有的账本
        List<LedgerEntity> ownedLedgers = ledgerRepository.findByOwnerUserIdAndDeleteTimeIsNull(userId);
        
        // 获取用户参与的共享账本
        List<LedgerMemberEntity> memberships = ledgerMemberRepository.findByUserId(userId);
        List<Long> sharedLedgerIds = memberships.stream()
                .map(LedgerMemberEntity::getLedgerId)
                .collect(Collectors.toList());
        List<LedgerEntity> sharedLedgers = sharedLedgerIds.isEmpty() 
            ? Collections.emptyList()
            : ledgerRepository.findAllById(sharedLedgerIds).stream()
                .filter(l -> l.getDeleteTime() == null)
                .collect(Collectors.toList());

        // 合并账本列表
        Map<Long, ExportLedgerData> ledgerDataMap = new LinkedHashMap<>();
        
        for (LedgerEntity ledger : ownedLedgers) {
            ledgerDataMap.put(ledger.getId(), convertToExportLedger(ledger, true));
        }
        
        for (LedgerEntity ledger : sharedLedgers) {
            if (!ledgerDataMap.containsKey(ledger.getId())) {
                ledgerDataMap.put(ledger.getId(), convertToExportLedger(ledger, false));
            }
        }

        return new ArrayList<>(ledgerDataMap.values());
    }

    /**
     * 获取用户可访问的账本ID列表
     */
    private List<Long> getAccessibleLedgerIds(Long userId) {
        // 用户拥有的账本
        List<Long> ownedLedgerIds = ledgerRepository.findByOwnerUserIdAndDeleteTimeIsNull(userId)
                .stream().map(LedgerEntity::getId).collect(Collectors.toList());
        
        // 用户参与的共享账本
        List<Long> sharedLedgerIds = ledgerMemberRepository.findByUserId(userId)
                .stream().map(LedgerMemberEntity::getLedgerId).collect(Collectors.toList());

        Set<Long> allIds = new HashSet<>();
        allIds.addAll(ownedLedgerIds);
        allIds.addAll(sharedLedgerIds);
        
        return new ArrayList<>(allIds);
    }

    /**
     * 获取用户可用的所有分类（用户自建 + 系统分类）
     */
    private Map<Long, CategoryEntity> getAllCategoriesForUser(Long userId) {
        List<CategoryEntity> userCategories = categoryRepository
                .findByCreatedByUserIdAndDeleteTimeIsNull(userId);
        List<CategoryEntity> systemCategories = categoryRepository
                .findByIsSystemTrueAndDeleteTimeIsNull();
        
        Map<Long, CategoryEntity> map = new HashMap<>();
        userCategories.forEach(c -> map.put(c.getId(), c));
        systemCategories.forEach(c -> map.put(c.getId(), c));
        
        return map;
    }

    /**
     * 统计交易数量
     */
    private long countTransactions(Long userId, List<Long> ledgerIds) {
        if (ledgerIds.isEmpty()) {
            return 0;
        }
        
        return transactionRepository.count((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deleteTime")));
            predicates.add(root.get("ledgerId").in(ledgerIds));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    // ==================== 数据转换方法 ====================

    private ExportTransactionData convertToExportTransaction(
            TransactionEntity tx,
            Map<Long, LedgerEntity> ledgerMap,
            Map<Long, CategoryEntity> categoryMap,
            Map<Long, PaymentMethodEntity> paymentMethodMap) {
        
        ExportTransactionData data = new ExportTransactionData();
        data.setId(tx.getId());
        data.setName(tx.getDescription());
        data.setDescription(tx.getDescription());
        data.setAmount(tx.getAmount());
        data.setType(TransactionTypeEnum.getByCode(tx.getType()) != null 
                ? TransactionTypeEnum.getByCode(tx.getType()).name() : "UNKNOWN");
        data.setTransactionDateTime(tx.getTransactionDateTime());
        data.setCreateTime(tx.getCreateTime());

        // 账本信息
        if (tx.getLedgerId() != null && ledgerMap.containsKey(tx.getLedgerId())) {
            data.setLedgerName(ledgerMap.get(tx.getLedgerId()).getName());
        }

        // 分类信息
        if (tx.getCategoryId() != null && categoryMap.containsKey(tx.getCategoryId())) {
            CategoryEntity category = categoryMap.get(tx.getCategoryId());
            data.setCategoryName(category.getName());
            data.setCategoryIcon(category.getIcon());
        }

        // 支付方式信息
        if (tx.getPaymentMethodId() != null && paymentMethodMap.containsKey(tx.getPaymentMethodId())) {
            data.setPaymentMethodName(paymentMethodMap.get(tx.getPaymentMethodId()).getName());
        }

        return data;
    }

    private ExportCategoryData convertToExportCategory(CategoryEntity category) {
        return new ExportCategoryData()
                .setId(category.getId())
                .setName(category.getName())
                .setIcon(category.getIcon())
                .setColor(category.getColor())
                .setType(TransactionTypeEnum.getByCode(category.getType()) != null 
                        ? TransactionTypeEnum.getByCode(category.getType()).name() : "UNKNOWN")
                .setSortOrder(category.getSortOrder())
                .setSystem(Boolean.TRUE.equals(category.getIsSystem()))
                .setDescription(category.getDescription())
                .setFrequent(Boolean.TRUE.equals(category.getIsFrequent()))
                .setCreateTime(category.getCreateTime());
    }

    private ExportPaymentMethodData convertToExportPaymentMethod(PaymentMethodEntity pm) {
        return new ExportPaymentMethodData()
                .setId(pm.getId())
                .setName(pm.getName())
                .setIcon(pm.getIcon())
                .setType(pm.getType())
                .setDefault(Boolean.TRUE.equals(pm.getIsDefault()))
                .setSortOrder(pm.getSortOrder())
                .setCreateTime(pm.getCreateTime());
    }

    private ExportLedgerData convertToExportLedger(LedgerEntity ledger, boolean isOwner) {
        return new ExportLedgerData()
                .setId(ledger.getId())
                .setName(ledger.getName())
                .setDescription(ledger.getDescription())
                .setType(LedgerTypeEnum.getTypeDescription(ledger.getType()))
                .setOwner(isOwner)
                .setCreateTime(ledger.getCreateTime());
    }

    // ==================== 格式转换方法 ====================

    /**
     * 转换为CSV格式
     */
    private String convertToCsv(Object data, String dataType) {
        StringBuilder sb = new StringBuilder();

        if (data instanceof ExportFullData fullData) {
            // 导出完整数据时，只导出交易记录为CSV
            appendTransactionsCsv(sb, fullData.getTransactions());
        } else if (data instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof ExportTransactionData) {
                @SuppressWarnings("unchecked")
                List<ExportTransactionData> transactions = (List<ExportTransactionData>) list;
                appendTransactionsCsv(sb, transactions);
            } else if (first instanceof ExportCategoryData) {
                @SuppressWarnings("unchecked")
                List<ExportCategoryData> categories = (List<ExportCategoryData>) list;
                appendCategoriesCsv(sb, categories);
            } else if (first instanceof ExportPaymentMethodData) {
                @SuppressWarnings("unchecked")
                List<ExportPaymentMethodData> paymentMethods = (List<ExportPaymentMethodData>) list;
                appendPaymentMethodsCsv(sb, paymentMethods);
            } else if (first instanceof ExportLedgerData) {
                @SuppressWarnings("unchecked")
                List<ExportLedgerData> ledgers = (List<ExportLedgerData>) list;
                appendLedgersCsv(sb, ledgers);
            }
        }

        return sb.toString();
    }

    private void appendTransactionsCsv(StringBuilder sb, List<ExportTransactionData> transactions) {
        // CSV 头部
        sb.append("ID,名称,描述,金额,类型,交易时间,账本,分类,支付方式,创建时间\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ExportTransactionData tx : transactions) {
            sb.append(tx.getId()).append(",");
            sb.append(escapeCsv(tx.getDescription())).append(",");
            sb.append(escapeCsv(tx.getDescription())).append(",");
            sb.append(tx.getAmount()).append(",");
            sb.append(tx.getType()).append(",");
            sb.append(tx.getTransactionDateTime() != null ? tx.getTransactionDateTime().format(formatter) : "").append(",");
            sb.append(escapeCsv(tx.getLedgerName())).append(",");
            sb.append(escapeCsv(tx.getCategoryName())).append(",");
            sb.append(escapeCsv(tx.getPaymentMethodName())).append(",");
            sb.append(tx.getCreateTime() != null ? tx.getCreateTime().format(formatter) : "").append("\n");
        }
    }

    private void appendCategoriesCsv(StringBuilder sb, List<ExportCategoryData> categories) {
        sb.append("ID,名称,图标,颜色,类型,排序,是否系统,描述,是否常用,创建时间\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ExportCategoryData cat : categories) {
            sb.append(cat.getId()).append(",");
            sb.append(escapeCsv(cat.getName())).append(",");
            sb.append(escapeCsv(cat.getIcon())).append(",");
            sb.append(escapeCsv(cat.getColor())).append(",");
            sb.append(cat.getType()).append(",");
            sb.append(cat.getSortOrder()).append(",");
            sb.append(cat.isSystem() ? "是" : "否").append(",");
            sb.append(escapeCsv(cat.getDescription())).append(",");
            sb.append(cat.isFrequent() ? "是" : "否").append(",");
            sb.append(cat.getCreateTime() != null ? cat.getCreateTime().format(formatter) : "").append("\n");
        }
    }

    private void appendPaymentMethodsCsv(StringBuilder sb, List<ExportPaymentMethodData> paymentMethods) {
        sb.append("ID,名称,图标,类型,是否默认,排序,创建时间\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ExportPaymentMethodData pm : paymentMethods) {
            sb.append(pm.getId()).append(",");
            sb.append(escapeCsv(pm.getName())).append(",");
            sb.append(escapeCsv(pm.getIcon())).append(",");
            sb.append(escapeCsv(pm.getType())).append(",");
            sb.append(pm.isDefault() ? "是" : "否").append(",");
            sb.append(pm.getSortOrder()).append(",");
            sb.append(pm.getCreateTime() != null ? pm.getCreateTime().format(formatter) : "").append("\n");
        }
    }

    private void appendLedgersCsv(StringBuilder sb, List<ExportLedgerData> ledgers) {
        sb.append("ID,名称,描述,类型,是否所有者,创建时间\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ExportLedgerData ledger : ledgers) {
            sb.append(ledger.getId()).append(",");
            sb.append(escapeCsv(ledger.getName())).append(",");
            sb.append(escapeCsv(ledger.getDescription())).append(",");
            sb.append(ledger.getType()).append(",");
            sb.append(ledger.isOwner() ? "是" : "否").append(",");
            sb.append(ledger.getCreateTime() != null ? ledger.getCreateTime().format(formatter) : "").append("\n");
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // 如果包含逗号、引号或换行，需要用引号包裹并转义引号
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 转换为Excel格式（Base64编码）
     * 注意：这里返回的是简化版本的CSV作为Excel兼容格式
     * 如需真正的Excel格式，需要添加Apache POI依赖
     */
    private String convertToExcel(Object data, String dataType) {
        // 简化实现：将CSV转为Base64
        // 如需真正的Excel格式，可以引入Apache POI库
        String csvData = convertToCsv(data, dataType);
        return Base64.getEncoder().encodeToString(csvData.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String generateFileName(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "ledger_export_" + timestamp + "." + extension;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}

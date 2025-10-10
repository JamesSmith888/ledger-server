package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账本 MCP 工具类
 * @author James Smith
 */
@Component
@Slf4j
public class LedgerMCP {

    @Resource
    private LedgerService ledgerService;

    @Tool(description = """
            Purpose: Create a new ledger for a user
            
            Prerequisites:
            - NONE - Direct ledger creation process
            
            Parameters:
            - name: Ledger name (required, must be unique per user)
            - description: Ledger description (optional)
            - ownerUserId: Owner user ID (required)
            
            Returns:
            - Success: Ledger creation successful message with ledger info
            - Failure: Error message if creation fails
            
            Error Handling:
            - If ledger name already exists for the user:
              1. Return "账本名称已存在" error message
              2. Suggest alternative ledger names
              3. Wait for user confirmation
              4. Retry creation with confirmed name
            
            Workflow:
            1. Validate name and ownerUserId are provided
            2. Check for duplicate ledger name under the same user
            3. Create ledger if validation passes
            4. Return success message with ledger details
            """)
    public String createLedger(String name, String description, Long ownerUserId) {
        log.info("Creating ledger: name={}, ownerUserId={}", name, ownerUserId);
        LedgerEntity ledger = ledgerService.create(name, description, ownerUserId);
        log.info("Ledger created: {}", ledger);
        return String.format("账本创建成功: ID=%d, 名称=%s", ledger.getId(), ledger.getName());
    }

    @Tool(description = """
            Purpose: Query ledger details by ledger ID
            
            Prerequisites:
            - NONE
            
            Parameters:
            - id: Ledger ID (required)
            
            Returns:
            - Success: Ledger details including name, description, owner ID, create/update time
            - Failure: Error message if ledger not found
            
            Workflow:
            1. Validate ledger ID is provided
            2. Query ledger from database
            3. Return ledger details
            """)
    public String getLedger(Long id) {
        log.info("Getting ledger by id: {}", id);
        LedgerEntity ledger = ledgerService.findById(id);
        return formatLedgerInfo(ledger);
    }

    @Tool(description = """
            Purpose: Query all ledgers owned by a specific user
            
            Prerequisites:
            - NONE
            
            Parameters:
            - ownerUserId: Owner user ID (required)
            
            Returns:
            - Success: List of ledgers with their details
            - Failure: Error message if query fails
            
            Workflow:
            1. Validate owner user ID is provided
            2. Query all ledgers by owner user ID
            3. Return list of ledger details
            """)
    public String listUserLedgers(Long ownerUserId) {
        log.info("Listing ledgers for user: {}", ownerUserId);
        List<LedgerEntity> ledgers = ledgerService.findByOwnerUserId(ownerUserId);
        
        if (ledgers.isEmpty()) {
            return "该用户暂无账本";
        }
        
        return ledgers.stream()
                .map(this::formatLedgerInfo)
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool(description = """
            Purpose: Update ledger information
            
            Prerequisites:
            - NONE
            
            Parameters:
            - id: Ledger ID (required)
            - name: New ledger name (optional, if provided must not conflict with existing names)
            - description: New ledger description (optional)
            
            Returns:
            - Success: Update successful message with updated ledger info
            - Failure: Error message if update fails
            
            Error Handling:
            - If new name conflicts with existing ledger name:
              1. Return "账本名称已存在" error
              2. Suggest alternative names
            
            Workflow:
            1. Validate ledger ID is provided
            2. Check if ledger exists
            3. Validate new name if provided (no duplicates)
            4. Update ledger information
            5. Return success message
            """)
    public String updateLedger(Long id, String name, String description) {
        log.info("Updating ledger: id={}, name={}, description={}", id, name, description);
        LedgerEntity ledger = ledgerService.update(id, name, description);
        log.info("Ledger updated: {}", ledger);
        return String.format("账本更新成功: %s", formatLedgerInfo(ledger));
    }

    @Tool(description = """
            Purpose: Delete a ledger (soft delete - mark as deleted)
            
            Prerequisites:
            - NONE
            
            Parameters:
            - id: Ledger ID (required)
            
            Returns:
            - Success: Deletion successful message
            - Failure: Error message if deletion fails
            
            Note:
            - This is a soft delete operation, the ledger record remains in database
            - The ledger's deleteTime field will be set to current timestamp
            - Deleted ledgers can potentially be restored
            
            Workflow:
            1. Validate ledger ID is provided
            2. Check if ledger exists and not already deleted
            3. Set deleteTime to current timestamp
            4. Return success message
            """)
    public String deleteLedger(Long id) {
        log.info("Deleting ledger: id={}", id);
        ledgerService.delete(id);
        log.info("Ledger deleted: id={}", id);
        return String.format("账本已删除: ID=%d", id);
    }

    /**
     * 格式化账本信息
     */
    private String formatLedgerInfo(LedgerEntity ledger) {
        return String.format(
                "账本ID: %d\n名称: %s\n描述: %s\n所有者ID: %d\n创建时间: %s\n更新时间: %s",
                ledger.getId(),
                ledger.getName(),
                ledger.getDescription() != null ? ledger.getDescription() : "无",
                ledger.getOwnerUserId(),
                ledger.getCreateTime(),
                ledger.getUpdateTime()
        );
    }
}

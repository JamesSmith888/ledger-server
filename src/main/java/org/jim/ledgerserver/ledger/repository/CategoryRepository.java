package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 交易分类数据访问层
 * @author James Smith
 */
@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    /**
     * 根据分类类型查找分类（包含系统预设和用户自定义）
     * @param type 分类类型
     * @return 分类列表
     */
    @Query("SELECT c FROM category c WHERE c.type = :type AND c.deleteTime IS NULL ORDER BY c.isSystem DESC, c.sortOrder ASC, c.createTime ASC")
    List<CategoryEntity> findByType(@Param("type") Integer type);

    /**
     * 根据分类类型和用户ID查找分类（包含系统预设和用户自定义）
     * @param type 分类类型
     * @param userId 用户ID
     * @return 分类列表
     */
    @Query("SELECT c FROM category c WHERE c.type = :type AND (c.isSystem = true OR c.createdByUserId = :userId) AND c.deleteTime IS NULL ORDER BY c.isSystem DESC, c.sortOrder ASC, c.createTime ASC")
    List<CategoryEntity> findByTypeAndUserId(@Param("type") Integer type, @Param("userId") Long userId);

    /**
     * 查找用户可见的所有分类（包含系统预设和用户自定义）
     * @param userId 用户ID
     * @return 分类列表
     */
    @Query("SELECT c FROM category c WHERE (c.isSystem = true OR c.createdByUserId = :userId) AND c.deleteTime IS NULL ORDER BY c.type ASC, c.isSystem DESC, c.sortOrder ASC, c.createTime ASC")
    List<CategoryEntity> findAllByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查找用户自定义的分类
     * @param userId 用户ID
     * @return 分类列表
     */
    List<CategoryEntity> findByCreatedByUserIdAndDeleteTimeIsNull(Long userId);

    /**
     * 根据用户ID分页查找用户自定义的分类
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分类分页结果
     */
    Page<CategoryEntity> findByCreatedByUserIdAndDeleteTimeIsNull(Long userId, Pageable pageable);

    /**
     * 查找系统预设分类
     * @return 系统预设分类列表
     */
    List<CategoryEntity> findByIsSystemTrueAndDeleteTimeIsNull();

    /**
     * 根据名称和类型查找分类（用于检查重复）
     * @param name 分类名称
     * @param type 分类类型
     * @param userId 用户ID（null表示系统分类）
     * @return 分类实体
     */
    @Query("SELECT c FROM category c WHERE c.name = :name AND c.type = :type AND " +
           "((:userId IS NULL AND c.isSystem = true) OR (c.createdByUserId = :userId)) AND c.deleteTime IS NULL")
    Optional<CategoryEntity> findByNameAndTypeAndUserId(@Param("name") String name, 
                                                        @Param("type") Integer type, 
                                                        @Param("userId") Long userId);

    /**
     * 查找系统预设分类中的最大排序值
     * @param type 分类类型
     * @return 最大排序值
     */
    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM category c WHERE c.type = :type AND c.isSystem = true AND c.deleteTime IS NULL")
    Integer findMaxSortOrderByTypeAndIsSystemTrue(@Param("type") Integer type);

    /**
     * 查找用户自定义分类中的最大排序值
     * @param type 分类类型
     * @param userId 用户ID
     * @return 最大排序值
     */
    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM category c WHERE c.type = :type AND c.createdByUserId = :userId AND c.deleteTime IS NULL")
    Integer findMaxSortOrderByTypeAndUserId(@Param("type") Integer type, @Param("userId") Long userId);
}
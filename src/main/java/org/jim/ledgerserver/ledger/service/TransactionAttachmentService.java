package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.entity.TransactionAttachmentEntity;
import org.jim.ledgerserver.ledger.repository.TransactionAttachmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 交易附件业务逻辑层
 * @author James Smith
 */
@Component
public class TransactionAttachmentService {

    @Resource
    private TransactionAttachmentRepository attachmentRepository;

    @Resource
    private TransactionService transactionService;

    // 文件大小限制
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB（图片）
    @SuppressWarnings("unused")
    private static final long MAX_OTHER_FILE_SIZE = 10 * 1024 * 1024; // 10MB（其他文件，预留用于将来扩展）

    // 支持的图片类型
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    // 缩略图尺寸
    private static final int THUMBNAIL_MAX_WIDTH = 200;
    private static final int THUMBNAIL_MAX_HEIGHT = 200;

    /**
     * 上传附件
     * @param transactionId 交易ID
     * @param file 上传的文件
     * @return 附件实体
     */
    @Transactional
    public TransactionAttachmentEntity uploadAttachment(Long transactionId, MultipartFile file) {
        // 验证交易是否存在（会抛出异常如果不存在）
        transactionService.findById(transactionId);
        
        // 验证权限
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException("无法识别文件类型");
        }

        long fileSize = file.getSize();

        // 验证文件类型和大小
        boolean isImage = SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase());
        
        if (!isImage) {
            // 暂不支持非图片文件
            throw new BusinessException("当前仅支持上传图片文件（JPG、PNG、GIF、WebP）");
        }

        if (isImage && fileSize > MAX_IMAGE_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }

        // 检查交易的附件总大小（可选限制）
        long currentTotalSize = attachmentRepository.sumFileSizeByTransactionId(transactionId);
        if (currentTotalSize + fileSize > 50 * 1024 * 1024) { // 单个交易最多 50MB
            throw new BusinessException("该交易的附件总大小不能超过 50MB");
        }

        try {
            byte[] fileData = file.getBytes();
            
            // 创建附件实体
            TransactionAttachmentEntity attachment = new TransactionAttachmentEntity();
            attachment.setTransactionId(transactionId);
            attachment.setFileName(originalFilename);
            attachment.setFileType(contentType);
            attachment.setFileSize(fileSize);
            attachment.setFileData(fileData);
            attachment.setUploadedByUserId(currentUserId);

            // 如果是图片，生成缩略图
            if (isImage) {
                try {
                    BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(fileData));
                    if (originalImage != null) {
                        attachment.setWidth(originalImage.getWidth());
                        attachment.setHeight(originalImage.getHeight());
                        
                        // 生成缩略图
                        byte[] thumbnailData = generateThumbnail(originalImage);
                        attachment.setThumbnailData(thumbnailData);
                    }
                } catch (Exception e) {
                    // 缩略图生成失败不影响主流程
                    System.err.println("生成缩略图失败: " + e.getMessage());
                }
            }

            return attachmentRepository.save(attachment);
        } catch (IOException e) {
            throw new BusinessException("文件读取失败: " + e.getMessage());
        }
    }

    /**
     * 生成缩略图
     * @param originalImage 原始图片
     * @return 缩略图字节数组
     */
    private byte[] generateThumbnail(BufferedImage originalImage) throws IOException {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 计算缩略图尺寸（保持比例）
        double scale = Math.min(
                (double) THUMBNAIL_MAX_WIDTH / originalWidth,
                (double) THUMBNAIL_MAX_HEIGHT / originalHeight
        );

        int thumbnailWidth = (int) (originalWidth * scale);
        int thumbnailHeight = (int) (originalHeight * scale);

        // 创建缩略图
        BufferedImage thumbnail = new BufferedImage(
                thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
        g.dispose();

        // 转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "jpg", baos);
        return baos.toByteArray();
    }

    /**
     * 获取附件列表（仅元数据，用于列表展示）
     * @param transactionId 交易ID
     * @return 附件元数据列表
     */
    public List<TransactionAttachmentEntity> getAttachmentMetadata(Long transactionId) {
        return attachmentRepository.findMetadataByTransactionId(transactionId);
    }

    /**
     * 获取附件列表（包含缩略图）
     * @param transactionId 交易ID
     * @return 附件列表（包含缩略图）
     */
    public List<TransactionAttachmentEntity> getAttachmentsWithThumbnails(Long transactionId) {
        return attachmentRepository.findByTransactionIdWithThumbnails(transactionId);
    }

    /**
     * 根据ID获取附件（包含完整文件数据）
     * @param attachmentId 附件ID
     * @return 附件实体
     */
    public TransactionAttachmentEntity getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException("附件不存在"));
    }

    /**
     * 删除附件（逻辑删除）
     * @param attachmentId 附件ID
     */
    @Transactional
    public void deleteAttachment(Long attachmentId) {
        TransactionAttachmentEntity attachment = getAttachment(attachmentId);
        
        // 验证权限
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 只有上传者可以删除
        if (!currentUserId.equals(attachment.getUploadedByUserId())) {
            throw new BusinessException("无权限删除该附件");
        }

        attachment.setDeleteTime(LocalDateTime.now());
        attachmentRepository.save(attachment);
    }

    /**
     * 统计交易的附件数量
     * @param transactionId 交易ID
     * @return 附件数量
     */
    public long countAttachments(Long transactionId) {
        return attachmentRepository.countByTransactionId(transactionId);
    }

}

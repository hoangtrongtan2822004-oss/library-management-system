package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.EbookDownloadRepository;
import com.ibizabroker.lms.dao.EbookRepository;
import com.ibizabroker.lms.entity.Ebook;
import com.ibizabroker.lms.entity.EbookDownload;
import com.ibizabroker.lms.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Feature 5: E-book Management Service
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EbookService {

    private final EbookRepository ebookRepository;
    private final EbookDownloadRepository downloadRepository;

    @Value("${ebook.upload.path:uploads/ebooks}")
    private String uploadPath;

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public Page<Ebook> getAllEbooks(Pageable pageable) {
        return ebookRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Ebook> getPublicEbooks(Pageable pageable) {
        return ebookRepository.findByIsPublicTrue(pageable);
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public Ebook getEbookById(Long id) {
        return ebookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ebook với ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Ebook> searchEbooks(String search, String fileType, Boolean isPublic, Pageable pageable) {
        return ebookRepository.findWithFilters(search, fileType, isPublic, pageable);
    }

    @Transactional(readOnly = true)
    public List<Ebook> getTopDownloaded(int limit) {
        return ebookRepository.findTopDownloaded(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<Ebook> getNewestEbooks(int limit) {
        return ebookRepository.findNewest(PageRequest.of(0, limit));
    }

    public Ebook uploadEbook(MultipartFile file, com.ibizabroker.lms.dto.EbookCreateRequest dto, Integer uploaderId) throws IOException {
        // Create upload directory if not exists
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";
        String newFilename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadDir.resolve(newFilename);

        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create ebook entity
        Ebook ebook = new Ebook();
        ebook.setTitle(dto.getTitle());
        ebook.setAuthor(dto.getAuthor());
        ebook.setFilePath(filePath.toString());
        ebook.setFileType(extension.replace(".", "").toUpperCase());
        ebook.setFileSize(file.getSize());
        ebook.setCoverUrl(dto.getCoverUrl());
        ebook.setDescription(dto.getDescription());
        ebook.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true);
        ebook.setMaxDownloadsPerUser(dto.getMaxDownloadsPerUser() != null ? dto.getMaxDownloadsPerUser() : 3);
        ebook.setUploadedBy(uploaderId);
        ebook.setUploadedAt(LocalDateTime.now());

        return ebookRepository.save(ebook);
    }

    @SuppressWarnings("null")
    public Ebook updateEbook(Long id, com.ibizabroker.lms.dto.EbookCreateRequest dto) {
        Ebook ebook = getEbookById(id);
        if (dto.getTitle() != null) ebook.setTitle(dto.getTitle());
        if (dto.getAuthor() != null) ebook.setAuthor(dto.getAuthor());
        if (dto.getDescription() != null) ebook.setDescription(dto.getDescription());
        if (dto.getCoverUrl() != null) ebook.setCoverUrl(dto.getCoverUrl());
        if (dto.getIsPublic() != null) ebook.setIsPublic(dto.getIsPublic());
        if (dto.getMaxDownloadsPerUser() != null) ebook.setMaxDownloadsPerUser(dto.getMaxDownloadsPerUser());
        return ebookRepository.save(ebook);
    }

    public void deleteEbook(Long id) throws IOException {
        Ebook ebook = getEbookById(id);
        // Delete file
        Path filePath = Paths.get(ebook.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        ebookRepository.delete(ebook);
    }

    public void incrementViewCount(Long id) {
        ebookRepository.incrementViewCount(id);
    }

    public EbookDownload recordDownload(Long ebookId, Integer userId, String ipAddress) {
        Ebook ebook = getEbookById(ebookId);
        
        // Check download limit
        long userDownloads = downloadRepository.countByEbookAndUser(ebookId, userId);
        if (userDownloads >= ebook.getMaxDownloadsPerUser()) {
            throw new IllegalStateException("Bạn đã đạt giới hạn tải xuống cho ebook này.");
        }

        // Record download
        EbookDownload download = new EbookDownload();
        download.setEbook(ebook);
        download.setUserId(userId);
        download.setIpAddress(ipAddress);
        download.setDownloadedAt(LocalDateTime.now());

        ebookRepository.incrementDownloadCount(ebookId);
        return downloadRepository.save(download);
    }

    @Transactional(readOnly = true)
    public boolean canUserDownload(Long ebookId, Integer userId) {
        Ebook ebook = getEbookById(ebookId);
        long userDownloads = downloadRepository.countByEbookAndUser(ebookId, userId);
        return userDownloads < ebook.getMaxDownloadsPerUser();
    }

    @Transactional(readOnly = true)
    public List<EbookDownload> getUserDownloadHistory(Integer userId) {
        return downloadRepository.findByUserId(userId);
    }
}

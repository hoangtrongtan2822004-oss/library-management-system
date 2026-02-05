package com.ibizabroker.lms.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.ibizabroker.lms.dao.MemberCardRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.MemberCardRequest;
import com.ibizabroker.lms.dto.MemberCardResponse;
import com.ibizabroker.lms.entity.MemberCard;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.exceptions.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberCardService {

    private final MemberCardRepository memberCardRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public MemberCardResponse create(MemberCardRequest request) {
        Users user = usersRepository.findById(request.userId().intValue())
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        MemberCard card = MemberCard.builder()
                .cardNumber(generateCardNumber())
                .barcodeType(request.barcodeType())
                .status(MemberCard.MemberCardStatus.ACTIVE)
                .issuedAt(LocalDateTime.now())
                .expiredAt(request.expiredAt())
                .metadata(request.metadata())
                .user(user)
                .build();

        memberCardRepository.save(card);
        return toResponse(card);
    }

    @Transactional
    public MemberCardResponse update(Long id, MemberCardRequest request) {
        MemberCard card = memberCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Member card không tồn tại"));
        Users user = usersRepository.findById(request.userId().intValue())
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        card.setUser(user);
        card.setBarcodeType(request.barcodeType());
        card.setExpiredAt(request.expiredAt());
        card.setMetadata(request.metadata());
        card.setStatus(MemberCard.MemberCardStatus.ACTIVE);
        return toResponse(card);
    }

    @Transactional
    public MemberCardResponse revoke(Long id, String reason) {
        MemberCard card = memberCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Member card không tồn tại"));
        card.setStatus(MemberCard.MemberCardStatus.REVOKED);
        if (reason != null && !reason.isBlank()) {
            String meta = Optional.ofNullable(card.getMetadata()).orElse("");
            card.setMetadata((meta + "\nRevoked: " + reason).trim());
        }
        return toResponse(card);
    }

    @Transactional
    public MemberCardResponse getOrCreateForUser(Integer userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        MemberCard card = memberCardRepository.findFirstByUserUserIdOrderByIssuedAtDesc(userId)
                .orElseGet(() -> memberCardRepository.save(MemberCard.builder()
                        .cardNumber(generateCardNumber())
                        .barcodeType(MemberCard.BarcodeType.QR)
                        .status(MemberCard.MemberCardStatus.ACTIVE)
                        .issuedAt(LocalDateTime.now())
                        .metadata("Auto-issued digital card")
                        .user(user)
                        .build()));

        return toResponse(card);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<MemberCardResponse> search(String keyword,
                                           MemberCard.MemberCardStatus status,
                                           MemberCard.BarcodeType barcodeType,
                                           LocalDateTime from,
                                           LocalDateTime to,
                                           Pageable pageable) {
        List<Specification<MemberCard>> specs = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("cardNumber")), "%" + kw + "%"),
                    cb.like(cb.lower(root.join("user").get("username")), "%" + kw + "%"),
                    cb.like(cb.lower(root.join("user").get("fullName")), "%" + kw + "%")
            ));
        }
        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (barcodeType != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("barcodeType"), barcodeType));
        }
        if (from != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("issuedAt"), from));
        }
        if (to != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("issuedAt"), to));
        }

        Specification<MemberCard> spec = Specification.allOf(specs);
        return memberCardRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public MemberCardResponse get(Long id) {
        return memberCardRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Member card không tồn tại"));
    }

    public byte[] generateBarcodeImage(Long id, int width, int height) {
        MemberCard card = memberCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Member card không tồn tại"));
        String content = card.getCardNumber();
        BarcodeFormat format = card.getBarcodeType() == MemberCard.BarcodeType.QR
                ? BarcodeFormat.QR_CODE
                : BarcodeFormat.CODE_128;
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, format, width, height);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo barcode", e);
        }
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public byte[] generateBarcodeForUser(Integer userId, int width, int height) {
        MemberCard card = memberCardRepository.findFirstByUserUserIdOrderByIssuedAtDesc(userId)
                .orElseThrow(() -> new NotFoundException("User chưa có thẻ"));
        return generateBarcodeImage(card.getId(), width, height);
    }

    public byte[] generatePdf(Long id) {
        MemberCard card = memberCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Member card không tồn tại"));
        byte[] barcode = generateBarcodeImage(id, 240, 80);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A6.getWidth(), PDRectangle.A6.getHeight()));
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setNonStrokingColor(40, 40, 40);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.beginText();
                cs.newLineAtOffset(40, page.getMediaBox().getHeight() - 60);
                cs.showText("THẺ THƯ VIỆN");
                cs.endText();

                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.beginText();
                cs.newLineAtOffset(40, page.getMediaBox().getHeight() - 90);
                cs.showText("Họ tên: " + safe(card.getUser().getFullName(), card.getUser().getName()));
                cs.endText();

                cs.beginText();
                cs.newLineAtOffset(40, page.getMediaBox().getHeight() - 110);
                cs.showText("Mã thẻ: " + card.getCardNumber());
                cs.endText();

                cs.beginText();
                cs.newLineAtOffset(40, page.getMediaBox().getHeight() - 130);
                cs.showText("Lớp/Khoa: " + safe(card.getUser().getStudentClass(), "N/A"));
                cs.endText();

                if (card.getExpiredAt() != null) {
                    cs.beginText();
                    cs.newLineAtOffset(40, page.getMediaBox().getHeight() - 150);
                    cs.showText("Hết hạn: " + card.getExpiredAt());
                    cs.endText();
                }

                // Draw barcode image
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, barcode, "barcode");
                cs.drawImage(pdImage, 40, 60, 200, 70);
            }

            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo PDF thẻ", e);
        }
    }

    private MemberCardResponse toResponse(MemberCard card) {
        Users u = card.getUser();
        return MemberCardResponse.builder()
                .id(card.getId())
                .cardNumber(card.getCardNumber())
                .barcodeType(card.getBarcodeType())
                .status(card.getStatus())
                .issuedAt(card.getIssuedAt())
                .expiredAt(card.getExpiredAt())
                .metadata(card.getMetadata())
                .userId(u.getUserId())
                .username(u.getUsername())
                .fullName(Optional.ofNullable(u.getFullName()).orElse(u.getName()))
                .email(u.getEmail())
                .studentClass(u.getStudentClass())
                .build();
    }

    private String generateCardNumber() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        return raw.substring(0, 12).toUpperCase();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

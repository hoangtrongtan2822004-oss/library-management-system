package com.ibizabroker.lms.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ibizabroker.lms.dao.MemberCardRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.MemberCardDto;
import com.ibizabroker.lms.dto.MemberCardRequest;
import com.ibizabroker.lms.entity.BarcodeType;
import com.ibizabroker.lms.entity.MemberCard;
import com.ibizabroker.lms.entity.MemberCardStatus;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.exceptions.NotFoundException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCardService {

    private final MemberCardRepository memberCardRepository;
    private final UsersRepository usersRepository;

    public MemberCardDto create(MemberCardRequest request) {
        Users user = usersRepository.findById(request.getUserId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        MemberCard card = new MemberCard();
        card.setUser(user);
        card.setBarcodeType(request.getBarcodeType());
        card.setExpiredAt(parseDateTime(request.getExpiredAt()));
        card.setMetadata(request.getMetadata());
        card.setStatus(MemberCardStatus.ACTIVE);
        card.setCardNumber(generateCardNumber(user.getUserId()));

        return MemberCardDto.fromEntity(memberCardRepository.save(card));
    }

    public MemberCardDto update(Long id, MemberCardRequest request) {
        MemberCard card = getEntity(id);
        Users user = usersRepository.findById(request.getUserId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        card.setUser(user);
        card.setBarcodeType(request.getBarcodeType());
        card.setExpiredAt(parseDateTime(request.getExpiredAt()));
        card.setMetadata(request.getMetadata());
        refreshStatus(card);

        return MemberCardDto.fromEntity(memberCardRepository.save(card));
    }

    public MemberCardDto revoke(Long id, String reason) {
        MemberCard card = getEntity(id);
        card.setStatus(MemberCardStatus.REVOKED);
        card.setRevokedAt(LocalDateTime.now());
        card.setRevokedReason(reason);
        return MemberCardDto.fromEntity(memberCardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public Page<MemberCardDto> search(
        String keyword,
        MemberCardStatus status,
        BarcodeType barcodeType,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    ) {
        return memberCardRepository.search(keyword, status, barcodeType, from, to, pageable)
            .map(card -> {
                refreshStatus(card);
                return MemberCardDto.fromEntity(card);
            });
    }

    @Transactional(readOnly = true)
    public MemberCardDto get(Long id) {
        MemberCard card = getEntity(id);
        refreshStatus(card);
        return MemberCardDto.fromEntity(card);
    }

    @Transactional(readOnly = true)
    public byte[] generateBarcodePng(Long id, int width, int height) throws IOException {
        MemberCard card = getEntity(id);
        String payload = card.getCardNumber();
        BufferedImage image = createBarcodeImage(payload, card.getBarcodeType(), width, height);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateCardPdf(Long id) throws IOException {
        MemberCard card = getEntity(id);
        refreshStatus(card);
        BufferedImage barcode = createBarcodeImage(card.getCardNumber(), card.getBarcodeType(), 260, 80);

        Rectangle pageSize = new Rectangle(300, 180);
        Document document = new Document(pageSize, 16, 16, 12, 12);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

            Paragraph title = new Paragraph("Library Member Card", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            Users user = card.getUser();
            document.add(new Paragraph("Member: " + valueOrEmpty(user.getName()), bodyFont));
            document.add(new Paragraph("Username: " + valueOrEmpty(user.getUsername()), bodyFont));
            document.add(new Paragraph("Card #: " + card.getCardNumber(), bodyFont));
            if (card.getExpiredAt() != null) {
                document.add(new Paragraph("Expires: " + card.getExpiredAt().toLocalDate(), bodyFont));
            }

            document.add(new Paragraph(" "));

            Image barcodeImg = Image.getInstance(bufferedImageToBytes(barcode));
            barcodeImg.scaleToFit(260, 80);
            barcodeImg.setAlignment(Element.ALIGN_CENTER);
            document.add(barcodeImg);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IOException("Không thể tạo PDF thẻ", e);
        }
    }

    private BufferedImage createBarcodeImage(String content, BarcodeType type, int width, int height) throws IOException {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix;
            if (type == BarcodeType.QR) {
                matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            } else {
                matrix = new Code128Writer().encode(content, BarcodeFormat.CODE_128, width, height, hints);
            }
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException e) {
            throw new IOException("Không thể tạo barcode", e);
        }
    }

    private byte[] bufferedImageToBytes(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }

    private String generateCardNumber(Integer userId) {
        for (int i = 0; i < 5; i++) {
            String random = RandomStringUtils.randomNumeric(6);
            String cardNumber = "LIB-" + userId + "-" + random;
            if (!memberCardRepository.existsByCardNumber(cardNumber)) {
                return cardNumber;
            }
        }
        return "LIB-" + userId + "-" + RandomStringUtils.randomNumeric(8);
    }

    private MemberCard getEntity(Long id) {
        return memberCardRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thẻ"));
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ex) {
            try {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
                return date.atStartOfDay();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private void refreshStatus(MemberCard card) {
        if (card.getStatus() == MemberCardStatus.ACTIVE && card.getExpiredAt() != null) {
            if (card.getExpiredAt().isBefore(LocalDateTime.now())) {
                card.setStatus(MemberCardStatus.EXPIRED);
                memberCardRepository.save(card);
            }
        }
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }
}

package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dao.ReservationRepository;
import com.ibizabroker.lms.dao.RenewalRequestRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.BatchLoanRequest;
import com.ibizabroker.lms.dto.BatchLoanResponse;
import com.ibizabroker.lms.dto.LoanRequest;
import com.ibizabroker.lms.dto.RenewRequest;
import com.ibizabroker.lms.dto.ReservationRequest;
import com.ibizabroker.lms.entity.*;
import com.ibizabroker.lms.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@Service
public class CirculationService {

    private final BooksRepository booksRepo;
    private final LoanRepository loanRepo;
    private final ReservationRepository reservationRepo;
    private final RenewalRequestRepository renewalRepo;
    private final SystemSettingService systemSettingService;
    private final UsersRepository usersRepo;

    public CirculationService(BooksRepository b, LoanRepository l, ReservationRepository r, SystemSettingService settings, RenewalRequestRepository renewalRepo, UsersRepository usersRepo){
        this.booksRepo=b; this.loanRepo=l; this.reservationRepo=r; this.systemSettingService = settings; this.renewalRepo = renewalRepo; this.usersRepo = usersRepo;
    }

    /**
     * 📚 Loan Book - Mượn sách với Pessimistic Locking
     * 
     * ⚡ Concurrency Control (Phase 8):
     * - ✅ Pessimistic Lock: SELECT FOR UPDATE trước khi validate
     * - ✅ Atomic Operation: Lock → Validate → Decrement trong 1 transaction
     * - ✅ Race Condition Fixed: 2 requests không thể bypass business rules
     * 
     * 🎯 Before (Race Condition):
     * - Request A: Check availability (10 copies) → Pass
     * - Request B: Check availability (10 copies) → Pass
     * - Request A: Decrement → 9 copies
     * - Request B: Decrement → 8 copies
     * - Result: Both borrowed but reservation queue not honored
     * 
     * 🎯 After (Pessimistic Lock):
     * - Request A: Lock book → Check availability → Decrement → Release lock
     * - Request B: Wait for lock → Check availability → Decrement → Release lock
     * - Result: Sequential processing, business rules enforced
     * 
     * @param req LoanRequest with bookId, memberId, quantity
     * @return Last created Loan
     */
    @Transactional
    public Loan loanBook(LoanRequest req) {
        // 1. Xác định số lượng cần mượn (mặc định là 1 nếu không nhập)
        int quantityToBorrow = (req.getQuantity() != null && req.getQuantity() > 0) ? req.getQuantity() : 1;
        Loan lastLoan = null;

        // 2. Thực hiện vòng lặp để mượn từng cuốn
        for (int i = 0; i < quantityToBorrow; i++) {
            // ✅ Phase 8: Lock book first (SELECT FOR UPDATE)
            Books book = booksRepo.findByIdWithLock(req.getBookId())
                .orElseThrow(() -> new IllegalStateException("Sách không tồn tại."));
            
            // Check reservation queue (now atomic with lock)
            List<Reservation> queue = reservationRepo
                .findTop10ByBookIdAndStatusOrderByCreatedAtAsc(req.getBookId(), ReservationStatus.ACTIVE);
            
            if(!queue.isEmpty() && !queue.get(0).getMemberId().equals(req.getMemberId()))
                throw new IllegalStateException("Sách đang được giữ chỗ cho người khác.");

            // Check availability (now safe - book locked)
            if (book.getNumberOfCopiesAvailable() <= 0) {
                throw new IllegalStateException("Không đủ số lượng sách trong kho.");
            }

            // Trừ tồn kho (now atomic - within locked transaction)
            int updated = booksRepo.decrementAvailable(req.getBookId());
            if(updated == 0) throw new IllegalStateException("Không đủ số lượng sách trong kho.");

            LocalDate today = LocalDate.now();
            Loan loan = new Loan();
            loan.setBook(book);  // ✅ Use setBook() with entity object
            // ✅ Fetch and set member entity
            Users member = usersRepo.findById(req.getMemberId())
                .orElseThrow(() -> new IllegalStateException("Người dùng không tồn tại."));
            loan.setMember(member);
            loan.setLoanDate(today);
            int defaultDays = systemSettingService.getInt(SystemSettingService.KEY_LOAN_MAX_DAYS, 14);
            loan.setDueDate(today.plusDays(req.getLoanDays() != null ? req.getLoanDays() : defaultDays));
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setFineStatus(FineStatus.NO_FINE);  // ✅ Use enum
            
            // Lưu ý: Tạm thời chúng ta chưa lưu studentName/Class vào DB để tránh lỗi cấu trúc bảng.
            // Thông tin này hiện tại dùng để xác nhận ở phía Frontend.
            
            lastLoan = loanRepo.save(loan);

            if(!queue.isEmpty() && queue.get(0).getMemberId().equals(req.getMemberId())){
                Reservation r = queue.get(0);
                r.setStatus(ReservationStatus.FULFILLED);
                reservationRepo.save(r);
            }
        }
        
        return lastLoan; // Trả về đơn cuối cùng để lấy trạng thái
    }

    @Transactional
    public Loan returnBook(Integer loanId){
        @SuppressWarnings("null")
        Loan loan = loanRepo.findById(loanId).orElseThrow(() -> new NotFoundException("Loan not found"));
        if(loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) return loan;
        
        LocalDate returnDate = LocalDate.now();
        loan.setReturnDate(returnDate);
        loan.setStatus(LoanStatus.RETURNED);

        if (returnDate.isAfter(loan.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
            if (overdueDays > 0) {
                BigDecimal finePerDay = systemSettingService.getBigDecimal(SystemSettingService.KEY_FINE_PER_DAY, new BigDecimal("2000"));
                BigDecimal totalFine = finePerDay.multiply(BigDecimal.valueOf(overdueDays));
                loan.setFineAmount(totalFine);
                loan.setFineStatus(FineStatus.UNPAID);  // ✅ Use enum
            }
        }
        
        booksRepo.incrementAvailable(loan.getBookId());
        return loanRepo.save(loan);
    }

    @Transactional
    public Loan renewLoan(RenewRequest req){
        @SuppressWarnings("null")
        Loan loan = loanRepo.findById(req.getLoanId()).orElseThrow(() -> new NotFoundException("Loan not found"));
        if(loan.getStatus() != LoanStatus.ACTIVE) throw new IllegalStateException("Loan is not active.");
        boolean isReservedByOthers = reservationRepo.existsByBookIdAndStatusAndMemberIdNot(
            loan.getBookId(), ReservationStatus.ACTIVE, loan.getMemberId());
        if(isReservedByOthers) throw new IllegalStateException("Book is reserved by another member; cannot renew.");
        int extra = req.getExtraDays()!=null?req.getExtraDays():7;
        loan.setDueDate(loan.getDueDate().plusDays(extra));
        return loanRepo.save(loan);
    }

    @Transactional
    public RenewalRequest requestRenewal(RenewRequest req, Integer memberId){
        @SuppressWarnings("null")
        Loan loan = loanRepo.findById(req.getLoanId()).orElseThrow(() -> new NotFoundException("Loan not found"));
        if(!loan.getMemberId().equals(memberId)) throw new IllegalStateException("Không thể yêu cầu gia hạn cho khoản mượn của người khác");
        // Ngăn tạo trùng yêu cầu PENDING cho cùng loan
        RenewalRequest existing = renewalRepo.findFirstByLoanIdAndStatus(loan.getId(), RenewalStatus.PENDING);
        if(existing != null) throw new IllegalStateException("Đã có yêu cầu gia hạn đang chờ xử lý cho khoản mượn này");
        RenewalRequest rr = new RenewalRequest();
        rr.setLoanId(loan.getId());
        rr.setMemberId(memberId);
        rr.setExtraDays(req.getExtraDays()!=null?req.getExtraDays():7);
        rr.setStatus(RenewalStatus.PENDING);
        rr.setCreatedAt(LocalDateTime.now());
        return renewalRepo.save(rr);
    }

    @Transactional
    public Loan approveRenewal(Long requestId, String note){
        @SuppressWarnings("null")
        RenewalRequest rr = renewalRepo.findById(requestId).orElseThrow(() -> new NotFoundException("Renewal request not found"));
        if(rr.getStatus()!=RenewalStatus.PENDING) throw new IllegalStateException("Request already decided");
        @SuppressWarnings("null")
        Loan loan = loanRepo.findById(rr.getLoanId()).orElseThrow(() -> new NotFoundException("Loan not found"));
        boolean isReservedByOthers = reservationRepo.existsByBookIdAndStatusAndMemberIdNot(
            loan.getBookId(), ReservationStatus.ACTIVE, loan.getMemberId());
        if(isReservedByOthers) throw new IllegalStateException("Book is reserved by another member; cannot renew.");
        int extra = rr.getExtraDays()!=null?rr.getExtraDays():7;
        loan.setDueDate(loan.getDueDate().plusDays(extra));
        rr.setStatus(RenewalStatus.APPROVED);
        rr.setDecidedAt(LocalDateTime.now());
        rr.setAdminNote(note);
        renewalRepo.save(rr);
        return loanRepo.save(loan);
    }

    @Transactional
    public RenewalRequest rejectRenewal(Long requestId, String note){
        @SuppressWarnings("null")
        RenewalRequest rr = renewalRepo.findById(requestId).orElseThrow(() -> new NotFoundException("Renewal request not found"));
        if(rr.getStatus()!=RenewalStatus.PENDING) throw new IllegalStateException("Request already decided");
        rr.setStatus(RenewalStatus.REJECTED);
        rr.setDecidedAt(LocalDateTime.now());
        rr.setAdminNote(note);
        return renewalRepo.save(rr);
    }

    @Transactional
    public Reservation placeReservation(ReservationRequest req){
        if(reservationRepo.existsByBookIdAndMemberIdAndStatus(req.getBookId(), req.getMemberId(), ReservationStatus.ACTIVE))
            throw new IllegalStateException("Already reserved.");
        Reservation r = new Reservation();
        r.setBookId(req.getBookId());
        r.setMemberId(req.getMemberId());
        r.setStatus(ReservationStatus.ACTIVE);
        r.setCreatedAt(LocalDateTime.now());
        r.setExpireAt(LocalDateTime.now().plusDays(3));
        return reservationRepo.save(r);
    }

    @Transactional
    public void cancelReservation(Integer reservationId){
        @SuppressWarnings("null")
        Reservation r = reservationRepo.findById(reservationId).orElseThrow(() -> new NotFoundException("Reservation not found"));
        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepo.save(r);
    }

    @Transactional(readOnly = true)
    public List<Loan> findLoans(Integer memberId, Integer bookId, boolean activeOnly) {
        if (memberId != null) {
            if (activeOnly) {
                return loanRepo.findByMemberIdAndStatus(memberId, LoanStatus.ACTIVE);
            } else {
                return loanRepo.findByMemberId(memberId);
            }
        }
        if (bookId != null) {
            return loanRepo.findByBookId(bookId);
        }
        throw new IllegalStateException("Yêu cầu phải có memberId hoặc bookId");
    }

    // === TRIỂN KHAI PHƯƠNG THỨC CÒN THIẾU TẠI ĐÂY ===
    @Transactional(readOnly = true)
    public List<Reservation> findReservations(Integer memberId) {
        return reservationRepo.findByMemberIdAndStatus(memberId, ReservationStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<RenewalRequest> findRenewalsByMember(Integer memberId){
        return renewalRepo.findByMemberId(memberId);
    }

    /**
     * Batch create loans for multiple books for a single user
     * Used by Admin Scanner Loan Mode for efficient bulk checkout
     */
    @Transactional
    public BatchLoanResponse batchCreateLoans(BatchLoanRequest request) {
        List<BatchLoanResponse.LoanResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (Integer bookId : request.getBookIds()) {
            try {
                // Create individual loan request
                LoanRequest loanRequest = new LoanRequest();
                loanRequest.setBookId(bookId);
                loanRequest.setMemberId(request.getUserId());
                loanRequest.setQuantity(1);
                if (request.getLoanDays() != null) {
                    loanRequest.setLoanDays(request.getLoanDays());
                }

                // Create loan using existing method
                Loan createdLoan = loanBook(loanRequest);

                // Record success
                results.add(new BatchLoanResponse.LoanResult(
                    bookId,
                    createdLoan.getId(),
                    true,
                    null
                ));
                successCount++;

            } catch (Exception e) {
                // Record failure
                results.add(new BatchLoanResponse.LoanResult(
                    bookId,
                    null,
                    false,
                    e.getMessage()
                ));
                failureCount++;
            }
        }

        return new BatchLoanResponse(results, successCount, failureCount);
    }
}
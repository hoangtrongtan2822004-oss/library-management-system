package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dao.RenewalRequestRepository;
import com.ibizabroker.lms.dao.ReservationRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.entity.*;
import com.ibizabroker.lms.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CirculationService — fine calculation, renewal, and guard conditions.
 *
 * All external collaborators are mocked; LocalDate.now() is the live clock
 * so dueDate fixtures use relative offsets (minusDays / plusDays).
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CirculationServiceTest {

    // ── dependencies ──────────────────────────────────────────────────────────
    @Mock BooksRepository       booksRepo;
    @Mock LoanRepository        loanRepo;
    @Mock ReservationRepository reservationRepo;
    @Mock SystemSettingService  systemSettingService;
    @Mock RenewalRequestRepository renewalRepo;
    @Mock UsersRepository       usersRepo;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks
    CirculationService circulationService;

    // ── shared fixtures ───────────────────────────────────────────────────────
    private Books  book;
    private Users  member;

    @BeforeEach
    void setUpFixtures() {
        book = new Books();
        book.setId(10);
        book.setName("Clean Code");
        book.setNumberOfCopiesAvailable(3);

        member = new Users();
        member.setUserId(42);
        member.setUsername("alice");
        member.setEmail("alice@example.com");
    }

    // =========================================================================
    //  returnBook() — fine calculation
    // =========================================================================
    @Nested
    @DisplayName("returnBook() — fine calculation")
    class ReturnBookFineTests {

        /**
         * Builds an ACTIVE loan with the given dueDate and wires up the common mocks
         * needed for the returnBook() happy path.
         */
        private Loan buildActiveLoan(LocalDate dueDate) {
            Loan loan = new Loan();
            loan.setId(1);
            loan.setBook(book);
            loan.setMember(member);
            loan.setLoanDate(dueDate.minusDays(14));
            loan.setDueDate(dueDate);
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setFineStatus(FineStatus.NO_FINE);

            when(loanRepo.findById(1)).thenReturn(Optional.of(loan));
            when(loanRepo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usersRepo.findById(42)).thenReturn(Optional.of(member));
            when(booksRepo.incrementAvailable(10)).thenReturn(1);
            // eventPublisher.publishEvent() is void — no stub needed; Mockito ignores by default
            return loan;
        }

        @Test
        @DisplayName("On-time return: no fine, status=RETURNED")
        void returnOnTime_NoFine() {
            // dueDate = tomorrow → will not be overdue today
            buildActiveLoan(LocalDate.now().plusDays(1));

            Loan result = circulationService.returnBook(1);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.RETURNED);
            assertThat(result.getFineAmount()).isNull();
            assertThat(result.getFineStatus()).isEqualTo(FineStatus.NO_FINE);
            verify(systemSettingService, never()).getBigDecimal(anyString(), any());
        }

        @Test
        @DisplayName("Return exactly on due-date: no fine")
        void returnOnDueDate_NoFine() {
            buildActiveLoan(LocalDate.now());   // dueDate = today

            Loan result = circulationService.returnBook(1);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.RETURNED);
            assertThat(result.getFineAmount()).isNull();
        }

        @ParameterizedTest(name = "{0} overdue days → fine {1}")
        @CsvSource({
            "1,  2000",
            "3,  6000",
            "7, 14000",
            "30,60000"
        })
        @DisplayName("Overdue return: fine = overdueDays × finePerDay (default 2000)")
        void returnOverdue_FineCalculated(long overdueDays, long expectedFine) {
            buildActiveLoan(LocalDate.now().minusDays(overdueDays));
            when(systemSettingService.getBigDecimal(
                    eq(SystemSettingService.KEY_FINE_PER_DAY), any()))
                .thenReturn(new BigDecimal("2000"));

            Loan result = circulationService.returnBook(1);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.RETURNED);
            assertThat(result.getFineStatus()).isEqualTo(FineStatus.UNPAID);
            assertThat(result.getFineAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(expectedFine));
        }

        @Test
        @DisplayName("Custom finePerDay from SystemSettings is applied")
        void customFinePerDay_IsApplied() {
            // overdue by 5 days, custom rate = 5000 → fine = 25000
            buildActiveLoan(LocalDate.now().minusDays(5));
            when(systemSettingService.getBigDecimal(
                    eq(SystemSettingService.KEY_FINE_PER_DAY), any()))
                .thenReturn(new BigDecimal("5000"));

            Loan result = circulationService.returnBook(1);

            assertThat(result.getFineAmount())
                .isEqualByComparingTo(new BigDecimal("25000"));
        }

        @Test
        @DisplayName("Already-returned loan is idempotent (returns without changes)")
        void alreadyReturned_IsIdempotent() {
            Loan loan = new Loan();
            loan.setId(1);
            loan.setBook(book);
            loan.setMember(member);
            loan.setStatus(LoanStatus.RETURNED);        // ← already done
            loan.setReturnDate(LocalDate.now().minusDays(1));

            when(loanRepo.findById(1)).thenReturn(Optional.of(loan));

            Loan result = circulationService.returnBook(1);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.RETURNED);
            verify(loanRepo, never()).save(any());      // no persistence side-effect
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("OVERDUE status loan can be returned and fine is assigned")
        void overdueStatusLoan_CanBeReturnedWithFine() {
            Loan loan = new Loan();
            loan.setId(1);
            loan.setBook(book);
            loan.setMember(member);
            loan.setLoanDate(LocalDate.now().minusDays(20));
            loan.setDueDate(LocalDate.now().minusDays(3));
            loan.setStatus(LoanStatus.OVERDUE);         // ← already flagged overdue by batch
            loan.setFineStatus(FineStatus.UNPAID);

            when(loanRepo.findById(1)).thenReturn(Optional.of(loan));
            when(loanRepo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usersRepo.findById(42)).thenReturn(Optional.of(member));
            when(booksRepo.incrementAvailable(10)).thenReturn(1);
            when(systemSettingService.getBigDecimal(
                    eq(SystemSettingService.KEY_FINE_PER_DAY), any()))
                .thenReturn(new BigDecimal("2000"));

            Loan result = circulationService.returnBook(1);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.RETURNED);
            assertThat(result.getFineAmount()).isEqualByComparingTo(new BigDecimal("6000"));
        }

        @Test
        @DisplayName("Loan not found throws NotFoundException")
        void loanNotFound_ThrowsNotFoundException() {
            when(loanRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> circulationService.returnBook(999))
                .isInstanceOf(NotFoundException.class);
        }
    }

    // =========================================================================
    //  renewLoan() — guard conditions
    // =========================================================================
    @Nested
    @DisplayName("renewLoan() — guard conditions")
    class RenewLoanTests {

        private com.ibizabroker.lms.dto.RenewRequest request(int loanId, int extraDays) {
            com.ibizabroker.lms.dto.RenewRequest req = new com.ibizabroker.lms.dto.RenewRequest();
            req.setLoanId(loanId);
            req.setExtraDays(extraDays);
            return req;
        }

        @Test
        @DisplayName("Active loan renewed: dueDate extended by extraDays")
        void activeLoan_Renewed() {
            LocalDate originalDue = LocalDate.now().plusDays(3);
            Loan loan = new Loan();
            loan.setId(5);
            loan.setBook(book);
            loan.setMember(member);
            loan.setDueDate(originalDue);
            loan.setStatus(LoanStatus.ACTIVE);

            when(loanRepo.findById(5)).thenReturn(Optional.of(loan));
            when(reservationRepo.existsByBookIdAndStatusAndMemberIdNot(10, ReservationStatus.ACTIVE, 42))
                .thenReturn(false);
            when(loanRepo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            Loan result = circulationService.renewLoan(request(5, 7));

            assertThat(result.getDueDate()).isEqualTo(originalDue.plusDays(7));
        }

        @Test
        @DisplayName("Non-active loan throws IllegalStateException")
        void nonActiveLoan_Throws() {
            Loan loan = new Loan();
            loan.setId(5);
            loan.setBook(book);
            loan.setMember(member);
            loan.setStatus(LoanStatus.RETURNED);

            when(loanRepo.findById(5)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> circulationService.renewLoan(request(5, 7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("Book reserved by another member: renewal blocked")
        void bookReservedByOther_Throws() {
            Loan loan = new Loan();
            loan.setId(5);
            loan.setBook(book);
            loan.setMember(member);
            loan.setDueDate(LocalDate.now().plusDays(2));
            loan.setStatus(LoanStatus.ACTIVE);

            when(loanRepo.findById(5)).thenReturn(Optional.of(loan));
            when(reservationRepo.existsByBookIdAndStatusAndMemberIdNot(10, ReservationStatus.ACTIVE, 42))
                .thenReturn(true);   // ← another member holds a reservation

            assertThatThrownBy(() -> circulationService.renewLoan(request(5, 7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reserved");
        }
    }

    // =========================================================================
    //  Books entity — borrow / return availability
    // =========================================================================
    @Nested
    @DisplayName("Books entity — availability guards")
    class BooksEntityTests {

        @Test
        @DisplayName("borrowBook() decrements available count")
        void borrowBook_DecrementsCount() {
            book.borrowBook();
            assertThat(book.getNumberOfCopiesAvailable()).isEqualTo(2);
        }

        @Test
        @DisplayName("borrowBook() on 0 copies throws IllegalStateException")
        void borrowBook_ZeroCopies_Throws() {
            book.setNumberOfCopiesAvailable(0);
            assertThatThrownBy(book::borrowBook)
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("returnBook() increments available count")
        void returnBook_IncrementsCount() {
            book.returnBook();
            assertThat(book.getNumberOfCopiesAvailable()).isEqualTo(4);
        }
    }
}

package com.ibizabroker.lms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class LoanRequest {
    @NotNull(message = "Book ID is required")
    private Integer bookId;
    
    @NotNull(message = "Member ID is required")
    private Integer memberId;
    
    @Min(value = 1, message = "Loan days must be at least 1")
    private Integer loanDays = 14;

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }
    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    public Integer getLoanDays() { return loanDays; }
    public void setLoanDays(Integer loanDays) { this.loanDays = loanDays; }

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;      // Số lượng mượn
    private String studentName;    // Tên học sinh (dùng để đối chiếu/ghi chú)
    private String studentClass;
    
    public String getStudentClass() { return studentClass; }
    public void setStudentClass(String studentClass) { this.studentClass = studentClass; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}

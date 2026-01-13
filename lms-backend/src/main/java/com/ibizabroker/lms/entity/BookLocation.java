package com.ibizabroker.lms.entity;

import jakarta.persistence.*;

/**
 * Feature 4: Location Tracking - Quản lý vị trí sách trong thư viện
 */
@Entity
@Table(name = "book_locations")
public class BookLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "floor", nullable = false)
    private Integer floor; // Tầng

    @Column(name = "zone", length = 10)
    private String zone; // Khu vực (A, B, C...)

    @Column(name = "shelf_code", length = 20, nullable = false)
    private String shelfCode; // Mã kệ sách (VD: A1-01)

    @Column(name = "`row_number`")
    private Integer rowNumber; // Hàng

    @Column(name = "position")
    private Integer position; // Vị trí trên kệ

    @Column(name = "description", length = 255)
    private String description; // Mô tả thêm

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getShelfCode() { return shelfCode; }
    public void setShelfCode(String shelfCode) { this.shelfCode = shelfCode; }

    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFullLocation() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tầng ").append(floor);
        if (zone != null) sb.append(" - Khu ").append(zone);
        sb.append(" - Kệ ").append(shelfCode);
        if (rowNumber != null) sb.append(" - Hàng ").append(rowNumber);
        if (position != null) sb.append(" - Vị trí ").append(position);
        return sb.toString();
    }
}

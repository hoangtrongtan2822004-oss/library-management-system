package com.ibizabroker.lms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "parent_id")
    private Integer parentId;

    @Column(name = "color", length = 32)
    private String color;

    @Column(name = "icon_class", length = 100)
    private String iconClass;

    @Transient
    private Integer bookCount;

    @ManyToMany(mappedBy = "categories")
    @JsonIgnore
    private Set<Books> books = new HashSet<>();

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getIconClass() { return iconClass; }
    public void setIconClass(String iconClass) { this.iconClass = iconClass; }
    public Integer getBookCount() { return bookCount; }
    public void setBookCount(Integer bookCount) { this.bookCount = bookCount; }

    @JsonIgnore
    public Set<Books> getBooks() { return books; }
    public void setBooks(Set<Books> books) { this.books = books; }
}
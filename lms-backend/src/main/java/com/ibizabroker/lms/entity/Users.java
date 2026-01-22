package com.ibizabroker.lms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uk_users_username", columnNames = "username")
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(nullable = false, length = 50)
    private String username;

    @JsonIgnore
    @Column(nullable = false, length = 200)
    private String password;

    @Column(length = 100)
    private String name;

    // === THÊM TRƯỜNG EMAIL VÀO ĐÂY ===
    @Column(length = 100)
    private String email;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "student_class")
    private String studentClass;

    @Column(name = "phone_number")
    private String phoneNumber;

    // ... Getter và Setter cho 2 trường này ...
    public String getStudentClass() { return studentClass; }
    public void setStudentClass(String studentClass) { this.studentClass = studentClass; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_user_role_userid_roleid", columnNames = {"user_id","role_id"})
    )
    private Set<Role> roles = new HashSet<>();

    // Getters and Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    // === THÊM GETTER/SETTER CHO EMAIL ===
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public void addRole(Role role) { this.roles.add(role); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Users)) return false;
        Users users = (Users) o;
        return Objects.equals(username, users.username);
    }
    @Override public int hashCode() { return Objects.hash(username); }
}
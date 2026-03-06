// UsersRepository.java
package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Integer> {

       @Query("""
              select distinct u from Users u
              left join fetch u.roles r
              where u.userId = :id
       """)
       Optional<Users> findByIdWithRoles(@Param("id") Integer id);

    Optional<Users> findByUsername(String username);

    // (giữ nguyên nếu nơi khác đang dùng)
    @Query("""
           select distinct u from Users u
           left join fetch u.roles r
           where u.username = :username
           """)
    Optional<Users> findByUsernameWithRoles(@Param("username") String username);

    // 👇 BẢN IGNORE-CASE — dùng cho xác thực
    @Query("""
           select distinct u from Users u
           left join fetch u.roles r
           where lower(u.username) = lower(:username)
           """)
    Optional<Users> findByUsernameWithRolesIgnoreCase(@Param("username") String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

       Optional<Users> findFirstByEmailIgnoreCaseOrderByUserIdAsc(String email);

       Optional<Users> findByGoogleId(String googleId);
}

package com.lawfirm.repository;

import com.lawfirm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查找用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据部门ID查找用户列表
     */
    List<User> findByDepartmentId(Long departmentId);

    /**
     * 根据状态查找用户列表
     */
    List<User> findByStatus(Integer status);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据职位查找用户列表（性能优化）
     */
    List<User> findByPosition(String position);

    /**
     * 根据用户名删除用户（用于测试）
     */
    void deleteByUsername(String username);
}

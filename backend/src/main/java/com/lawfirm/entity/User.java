package com.lawfirm.entity;

import com.lawfirm.converter.EncryptConverter;
import com.lawfirm.enums.UserStatus;
import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user", indexes = {
    @Index(name = "idx_user_real_name", columnList = "real_name"),
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_phone", columnList = "phone"),
    @Index(name = "idx_user_department", columnList = "department_id"),
    @Index(name = "idx_user_status", columnList = "status"),
    @Index(name = "idx_user_deleted", columnList = "deleted")
})
public class User extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "用户名只能包含字母、数字和下划线，长度3-20个字符")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Column(name = "password", nullable = false)
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Email(message = "邮箱格式不正确")
    @Column(name = "email", length = 255)
    @Convert(converter = EncryptConverter.class)
    private String email;

    @Column(name = "phone", length = 255)
    @Convert(converter = EncryptConverter.class)
    private String phone;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "position", length = 50)
    private String position;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "last_login_time")
    private java.time.LocalDateTime lastLoginTime;

    /**
     * 获取用户状态枚举
     */
    public UserStatus getUserStatus() {
        return UserStatus.fromCode(this.status);
    }

    /**
     * 设置用户状态
     */
    public void setUserStatus(UserStatus userStatus) {
        this.status = userStatus.getCode();
    }

    /**
     * 检查用户是否启用
     */
    public boolean isActive() {
        return getUserStatus().isActive();
    }
}

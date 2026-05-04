package com.lawfirm.dto;

import com.lawfirm.validation.PasswordStrength;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建请求
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    private String username;

    /**
     * 密码
     * 安全要求：密码长度≥8位，包含大小写字母、数字
     */
    @NotBlank(message = "密码不能为空")
    @PasswordStrength(message = "密码必须至少8位，包含大小写字母和数字")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private Long departmentId;

    private String position;

    private String avatar;

    private Integer status = 1;
}

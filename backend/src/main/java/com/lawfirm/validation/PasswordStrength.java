package com.lawfirm.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 密码强度验证注解
 * 要求：密码长度≥8位，包含大小写字母、数字
 */
@Documented
@Constraint(validatedBy = PasswordStrengthValidatorImpl.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordStrength {

    String message() default "密码必须至少8位，包含大小写字母和数字";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 最小长度
     */
    int minLength() default 8;

    /**
     * 是否必须包含大写字母
     */
    boolean requireUppercase() default true;

    /**
     * 是否必须包含小写字母
     */
    boolean requireLowercase() default true;

    /**
     * 是否必须包含数字
     */
    boolean requireDigit() default true;

    /**
     * 是否必须包含特殊字符
     */
    boolean requireSpecialChar() default false;
}

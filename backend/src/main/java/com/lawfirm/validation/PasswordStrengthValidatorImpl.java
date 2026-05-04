package com.lawfirm.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * 密码强度验证器实现
 */
public class PasswordStrengthValidatorImpl implements ConstraintValidator<PasswordStrength, String> {

    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecialChar;

    // 正则表达式模式
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    @Override
    public void initialize(PasswordStrength constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecialChar = constraintAnnotation.requireSpecialChar();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        // 检查长度
        if (password.length() < minLength) {
            return false;
        }

        // 检查大写字母
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            return false;
        }

        // 检查小写字母
        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            return false;
        }

        // 检查数字
        if (requireDigit && !DIGIT_PATTERN.matcher(password).find()) {
            return false;
        }

        // 检查特殊字符
        if (requireSpecialChar && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return false;
        }

        return true;
    }
}

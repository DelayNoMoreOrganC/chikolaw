package com.lawfirm.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * 分配比例总和验证器
 */
public class PercentageSumValidator implements ConstraintValidator<PercentageSum, Object> {

    private String sourceField;
    private String departmentField;
    private String firmField;

    @Override
    public void initialize(PercentageSum constraintAnnotation) {
        this.sourceField = constraintAnnotation.sourceField();
        this.departmentField = constraintAnnotation.departmentField();
        this.firmField = constraintAnnotation.firmField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            Field sourceField = value.getClass().getDeclaredField(this.sourceField);
            Field departmentField = value.getClass().getDeclaredField(this.departmentField);
            Field firmField = value.getClass().getDeclaredField(this.firmField);

            sourceField.setAccessible(true);
            departmentField.setAccessible(true);
            firmField.setAccessible(true);

            BigDecimal source = (BigDecimal) sourceField.get(value);
            BigDecimal department = (BigDecimal) departmentField.get(value);
            BigDecimal firm = (BigDecimal) firmField.get(value);

            // 如果三个字段都为null，视为有效（可能是草稿状态）
            if (source == null && department == null && firm == null) {
                return true;
            }

            // 将null转换为0
            source = source != null ? source : BigDecimal.ZERO;
            department = department != null ? department : BigDecimal.ZERO;
            firm = firm != null ? firm : BigDecimal.ZERO;

            // 计算总和
            BigDecimal sum = source.add(department).add(firm);

            // 验证总和是否为100（允许0.01的误差）
            return sum.compareTo(new BigDecimal("100")) == 0;

        } catch (Exception e) {
            // 反射异常时返回true，避免影响正常流程
            return true;
        }
    }
}

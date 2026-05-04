package com.lawfirm.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 分配比例总和验证注解
 * 确保sourcePersonPercentage、departmentPercentage、firmPercentage三者之和为100%
 */
@Documented
@Constraint(validatedBy = PercentageSumValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PercentageSum {
    String message() default "分配比例总和必须为100%";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 需要验证的三个字段名
     */
    String sourceField() default "sourcePersonPercentage";

    String departmentField() default "departmentPercentage";

    String firmField() default "firmPercentage";
}

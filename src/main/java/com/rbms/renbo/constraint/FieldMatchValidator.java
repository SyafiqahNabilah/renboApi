package com.rbms.renbo.constraint;

import org.springframework.beans.BeanWrapperImpl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {

    private String firstFieldName;
    private String secondFieldName;

    @Override
    public void initialize(final FieldMatch constraintAnnotation) {
        firstFieldName = constraintAnnotation.first();
        secondFieldName = constraintAnnotation.second();
    }

    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext context) {
    try {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);

        Object first = wrapper.getPropertyValue(firstFieldName);
        Object second = wrapper.getPropertyValue(secondFieldName);

        return (first == null && second == null) || 
               (first != null && first.equals(second));
    } catch (Exception e) {
        return true;
    }
}
}
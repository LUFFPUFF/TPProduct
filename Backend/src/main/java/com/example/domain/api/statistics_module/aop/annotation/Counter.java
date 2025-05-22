package com.example.domain.api.statistics_module.aop.annotation;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Counter {
    String name();
    String description() default "";
    String conditionSpEL() default "true";
    String incrementAmountSpEL() default "1.0";
    Tag[] tags() default {};
}

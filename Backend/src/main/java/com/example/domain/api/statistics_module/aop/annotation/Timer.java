package com.example.domain.api.statistics_module.aop.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Timer {

    String name();
    String description() default "";
    String conditionSpEL() default "true";
    Tag[] tags() default {};
    boolean recordExceptions() default true;
    String exceptionTagKey() default "exception_class";
    String outcomeTagKey() default "outcome";
}

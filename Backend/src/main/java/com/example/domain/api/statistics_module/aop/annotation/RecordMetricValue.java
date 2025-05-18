package com.example.domain.api.statistics_module.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RecordMetricValue {

    String name();
    String description() default "";
    String companyIdSpEL() default "";
    String channelSpEL() default "";
    String[] additionalTagsSpEL() default {};
    String valueSpEL();
}

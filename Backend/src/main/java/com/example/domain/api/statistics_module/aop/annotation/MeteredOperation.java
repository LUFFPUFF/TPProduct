package com.example.domain.api.statistics_module.aop.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MeteredOperation {
    String prefix() default "";
    Counter[] counters() default {};
    Timer[] timers() default {};
}

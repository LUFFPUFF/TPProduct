package com.example.domain.api.statistics_module.aop;

import com.example.domain.api.statistics_module.aop.annotation.Counter;
import com.example.domain.api.statistics_module.aop.annotation.MeteredOperation;
import com.example.domain.api.statistics_module.aop.annotation.Tag;
import com.example.domain.api.statistics_module.aop.annotation.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMetricsAspect {

    private final MeterRegistry meterRegistry;
    private final ApplicationContext applicationContext;
    private final SpelExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("execution(@com.example.domain.api.statistics_module.aop.annotation.MeteredOperation * *(..)) " +
            "|| @within(com.example.domain.api.statistics_module.aop.annotation.MeteredOperation)")
    public Object meteredOperationAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        MeteredOperation meteredOperation = method.getAnnotation(MeteredOperation.class);
        if (meteredOperation == null) {
            Class<?> targetClass = joinPoint.getTarget().getClass();
            meteredOperation = targetClass.getAnnotation(MeteredOperation.class);
            if (meteredOperation == null) {
                log.warn("MeteredOperation annotation not found on method {} or class {}, but aspect was triggered. Proceeding without metrics.",
                        method.getName(), targetClass.getSimpleName());
                return joinPoint.proceed();
            }
        }

        String prefix = meteredOperation.prefix();
        if (meteredOperation.timers().length > 0) {
            Timer timerAnnotation  = meteredOperation.timers()[0];
            EvaluationContext initialContext = createEvaluationContext(joinPoint.getTarget(), method, joinPoint.getArgs(), null, null);
            if (!shouldRecord(timerAnnotation.conditionSpEL(), initialContext, "timer " + timerAnnotation.name())) {
                return joinPoint.proceed();
            }

            io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start(meterRegistry);
            Object result = null;
            Throwable throwable = null;
            try {
                result = joinPoint.proceed();
                return result;
            } catch (Throwable t) {
                throwable = t;
                throw t;
            } finally {
                try {
                    EvaluationContext finalContext = createEvaluationContext(joinPoint.getTarget(), method, joinPoint.getArgs(), result, throwable);
                    Tags tags = buildTags(timerAnnotation.tags(), finalContext, "timer " + timerAnnotation.name());

                    String outcomeValue = (throwable == null) ? "SUCCESS" : "FAILURE";
                    tags = tags.and(timerAnnotation.outcomeTagKey(), outcomeValue);

                    if (timerAnnotation.recordExceptions() && throwable != null) {
                        tags = tags.and(timerAnnotation.exceptionTagKey(), ClassUtils.getShortName(throwable.getClass()));
                    }
                    sample.stop(meterRegistry.timer(prefix + timerAnnotation.name(), tags));
                    log.trace("Recorded timer: {}, Tags: {}", prefix + timerAnnotation.name(), tags);
                } catch (Exception e) {
                    log.error("Failed to record timer {} for method {}: {}",
                            prefix + timerAnnotation.name(), method.getName(), e.getMessage(), e);
                }
                handleCounters(meteredOperation.counters(), prefix, joinPoint, result, throwable);
            }
        } else {
            Object result = null;
            Throwable throwable = null;
            try {
                result = joinPoint.proceed();
                return result;
            } catch (Throwable t) {
                throwable = t;
                throw t;
            } finally {
                handleCounters(meteredOperation.counters(), prefix, joinPoint, result, throwable);
            }
        }

    }

    private void handleCounters(Counter[] counters, String prefix, JoinPoint joinPoint, Object result, Throwable throwable) {
        if (counters.length == 0) return;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = createEvaluationContext(joinPoint.getTarget(), method, args, result, throwable);

        for (Counter counterAnnotation : counters) {
            if (shouldRecord(counterAnnotation.conditionSpEL(), context, "counter " + counterAnnotation.name())) {
                try {
                    Tags tags = buildTags(counterAnnotation.tags(), context, "counter " + counterAnnotation.name());
                    double amount = evaluateSpel(counterAnnotation.incrementAmountSpEL(), context, Double.class, 1.0, "counter increment amount");
                    meterRegistry.counter(prefix + counterAnnotation.name(), tags).increment(amount);
                    log.trace("Incremented counter: {}, Amount: {}, Tags: {}", prefix + counterAnnotation.name(), amount, tags);
                } catch (Exception e) {
                    log.error("Failed to increment counter {} for method {}: {}",
                            prefix + counterAnnotation.name(), method.getName(), e.getMessage(), e);
                }
            }
        }
    }

    private boolean shouldRecord(String conditionSpEL, EvaluationContext context, String metricDescription) {
        if (!StringUtils.hasText(conditionSpEL) || "true".equalsIgnoreCase(conditionSpEL.trim())) {
            return true;
        }
        try {
            Expression expression = expressionParser.parseExpression(conditionSpEL);
            Boolean should = expression.getValue(context, Boolean.class);
            return Boolean.TRUE.equals(should);
        } catch (Exception e) {
            log.warn("Failed to evaluate SpEL condition '{}' for metric [{}]: {}. Assuming false.",
                    conditionSpEL, metricDescription, e.getMessage());
            return false;
        }
    }

    private Tags buildTags(Tag[] tagAnnotations, EvaluationContext context, String metricDescription) {
        Tags tags = Tags.empty();
        if (tagAnnotations == null || tagAnnotations.length == 0) {
            return tags;
        }
        for (Tag tagAnnotation : tagAnnotations) {
            String tagValue = evaluateSpel(tagAnnotation.valueSpEL(), context, String.class, "unknown",
                    "tag '" + tagAnnotation.key() + "' for " + metricDescription);
            tags = tags.and(tagAnnotation.key(), tagValue);
        }
        return tags;
    }

    private EvaluationContext createEvaluationContext(Object rootObject, Method method, Object[] args, Object result, Throwable throwable) {
        StandardEvaluationContext context = new MethodBasedEvaluationContext(rootObject, method, args, parameterNameDiscoverer);
        context.setBeanResolver(new BeanFactoryResolver(this.applicationContext));
        context.setVariable("args", args);
        context.setVariable("result", result);
        context.setVariable("throwable", throwable);
        return context;
    }

    private <T> T evaluateSpel(String spelExpression, EvaluationContext context, Class<T> expectedType, T defaultValue, String descriptionForLog) {
        if (!StringUtils.hasText(spelExpression)) {
            return defaultValue;
        }
        try {
            Expression expression = expressionParser.parseExpression(spelExpression);
            T value = expression.getValue(context, expectedType);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            log.warn("Failed to evaluate SpEL expression '{}' for [{}]: {}. Returning default value '{}'.",
                    spelExpression, descriptionForLog, e.getMessage(), defaultValue);
            return defaultValue;
        }
    }
}

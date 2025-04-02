package com.example.domain.api.ans_api_module.template.services;

import com.example.domain.api.ans_api_module.template.exception.InvalidTagException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final static int MAX_TAG_LENGTH = 50;
    private final static int MAX_TAGS_PER_ANSWER  = 10;
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]+$");

    public Set<String> normalizeAndValidateTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }

        if (tags.size() > MAX_TAGS_PER_ANSWER) {
            throw new InvalidTagException(
                    String.format("Maximum %d tags allowed per answer", MAX_TAGS_PER_ANSWER));
        }

        return tags.stream()
                .map(this::normalizeSingleTag)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(
                        () -> new LinkedHashSet<>(MAX_TAGS_PER_ANSWER)));
    }

    private String normalizeSingleTag(String tag) {
        String normalized = StringUtils.normalizeSpace(tag);

        if (normalized == null) return null;

        normalized = normalized.trim();
        if (normalized.length() > MAX_TAG_LENGTH) {
            normalized = normalized.substring(0, MAX_TAG_LENGTH);
        }

        if (!TAG_PATTERN.matcher(normalized).matches()) {
            throw new InvalidTagException(
                    String.format("Tag '%s' contains invalid characters", normalized));
        }

        return normalized.toLowerCase();
    }

    public void validateTags(Set<String> tags) {
        if (tags != null) {
            tags.forEach(tag -> {
                if (!TAG_PATTERN.matcher(tag).matches()) {
                    throw new InvalidTagException(
                            String.format("Invalid tag format: %s", tag));
                }
            });
        }
    }
}

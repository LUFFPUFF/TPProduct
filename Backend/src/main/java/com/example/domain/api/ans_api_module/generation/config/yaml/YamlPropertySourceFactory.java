package com.example.domain.api.ans_api_module.generation.config.yaml;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;

import java.util.Objects;
import java.util.Properties;

public class YamlPropertySourceFactory implements PropertySourceFactory {
    @Override
    public @NotNull PropertySource<?> createPropertySource(String name, EncodedResource resource) {
        YamlPropertiesFactoryBean propertiesFactoryBean = new YamlPropertiesFactoryBean();
        propertiesFactoryBean.setResources(resource.getResource());
        propertiesFactoryBean.afterPropertiesSet();

        Properties properties = propertiesFactoryBean.getObject();

        String sourceName  = resource.getResource().getFilename();
        return new PropertiesPropertySource(Objects.requireNonNull(sourceName), Objects.requireNonNull(properties));
    }
}

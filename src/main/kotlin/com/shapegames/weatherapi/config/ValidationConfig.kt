package com.shapegames.weatherapi.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor

//@Configuration
class ValidationConfig {

    @Bean
    @ConditionalOnMissingBean
    fun validator(): LocalValidatorFactoryBean {
        return LocalValidatorFactoryBean()
    }

    @Bean
    @ConditionalOnMissingBean
    fun methodValidationPostProcessor(): MethodValidationPostProcessor {
        val processor = MethodValidationPostProcessor()
        processor.setValidator(validator())
        return processor
    }
}

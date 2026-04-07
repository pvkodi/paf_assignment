package com.sliitreserve.api.config;

import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Configuration for Bean Validation (Jakarta Validation/Hibernate Validator).
 * Configures:
 * - Custom message source for i18n validation messages
 * - LocalValidatorFactoryBean for Spring integration
 * - MethodValidationPostProcessor for method-level validation
 *
 * Messages are loaded from messages.properties in the classpath.
 */
@Slf4j
@Configuration
public class ValidationConfig {

    /**
     * Configures message source for validation messages.
     * Sources messages from classpath:messages.properties for i18n support.
     * Allows custom validation error messages per field/constraint.
     *
     * @return ResourceBundleMessageSource configured for validation messages
     */
    @Bean
    public ResourceBundleMessageSource messageSource() {
        log.info("Configuring Bean Validation message source");
        
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600); // Cache for 1 hour
        
        return messageSource;
    }

    /**
     * Integrates Bean Validation with Spring's Validator.
     * Provides a Validator bean that uses Jakarta Validation with custom messages.
     *
     * @return LocalValidatorFactoryBean configured with custom message source
     */
    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean(ResourceBundleMessageSource messageSource) {
        log.info("Configuring LocalValidatorFactoryBean with custom message source");
        
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        
        return bean;
    }

    /**
     * Enables method-level validation using @Validated on service/controller classes.
     * Allows validation of method parameters using @Valid and constraint annotations.
     * Throws MethodArgumentNotValidException to GlobalExceptionHandler on validation failure.
     *
     * @return MethodValidationPostProcessor for method parameter validation
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        log.info("Configuring MethodValidationPostProcessor for method-level validation");
        
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        // Validator bean will be auto-wired from LocalValidatorFactoryBean
        
        return processor;
    }

    /**
     * Provides a Validator bean for manual validation in services.
     * This bean is injected into services that need programmatic validation.
     *
     * Example:
     * <pre>
     * @Service
     * public class FacilityService {
     *     @Autowired private Validator validator;
     *
     *     public void validateFacility(Facility facility) {
     *         Set<ConstraintViolation<Facility>> violations = validator.validate(facility);
     *         if (!violations.isEmpty()) {
     *             throw new ValidationException("Invalid facility", extractMessages(violations));
     *         }
     *     }
     * }
     * </pre>
     *
     * @param factoryBean LocalValidatorFactoryBean configured with message source
     * @return Validator bean for manual validation
     */
    @Bean
    public Validator validator(LocalValidatorFactoryBean factoryBean) {
        return factoryBean.getValidator();
    }
}

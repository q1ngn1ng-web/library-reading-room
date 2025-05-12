package com.example.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.SimpleDateFormat;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // Define date formats to handle
        SimpleDateFormat localDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Configure mapper for flexibility
        objectMapper.setDateFormat(localDateFormat);
        objectMapper.setTimeZone(TimeZone.getDefault());

        // Prevent writing dates as timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // For more flexible date parsing
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        // Register modules
        objectMapper.registerModule(new JavaTimeModule());

        // Configure custom date deserializer with multiple formats
        SimpleModule dateModule = new SimpleModule();
        dateModule.addDeserializer(Date.class, new MultiFormatDateDeserializer());
        objectMapper.registerModule(dateModule);

        return objectMapper;
    }

    /**
     * Custom date deserializer that tries multiple date formats
     */
    public static class MultiFormatDateDeserializer extends DateDeserializers.DateDeserializer {
        private static final long serialVersionUID = 1L;

        @Override
        public Date deserialize(com.fasterxml.jackson.core.JsonParser jp,
                com.fasterxml.jackson.databind.DeserializationContext ctxt)
                throws java.io.IOException {
            try {
                // First try the standard ISO-8601 deserializer
                return super.deserialize(jp, ctxt);
            } catch (Exception e) {
                try {
                    // Then try the "yyyy-MM-dd HH:mm:ss" format
                    String dateStr = jp.getText();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    return format.parse(dateStr);
                } catch (Exception e2) {
                    // If that fails, rethrow the original exception
                    throw new com.fasterxml.jackson.databind.JsonMappingException(jp,
                            "Could not parse date: " + e.getMessage()
                                    + ". Tried formats: ISO-8601, yyyy-MM-dd HH:mm:ss");
                }
            }
        }
    }
}
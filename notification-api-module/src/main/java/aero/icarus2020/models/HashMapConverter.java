package aero.icarus2020.models;

import java.io.IOException;
import java.util.HashMap;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HashMapConverter implements AttributeConverter<HashMap<String, Object>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(HashMap<String, Object> metaInfo) {

        String metaInfoJson = null;
        try {
            metaInfoJson = objectMapper.writeValueAsString(metaInfo);
        } catch (final JsonProcessingException e) {
           System.out.println("JSON writing error");
        }

        return metaInfoJson;
    }

    @Override
    public HashMap<String, Object> convertToEntityAttribute(String metaInfoJSON) {

        HashMap<String, Object> metaInfo = null;
        try {
            metaInfo = objectMapper.readValue(metaInfoJSON, HashMap.class);
        } catch (final IOException e) {
            System.out.println("JSON reading error");
        }

        return metaInfo;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
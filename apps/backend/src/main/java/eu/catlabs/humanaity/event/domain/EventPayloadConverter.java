package eu.catlabs.humanaity.event.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;
import java.util.TreeMap;

@Converter
public class EventPayloadConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, String> payload) {
        Map<String, String> normalized = payload == null ? Map.of() : new TreeMap<>(payload);
        try {
            return OBJECT_MAPPER.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize event payload", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Map.of();
        }
        try {
            return new TreeMap<>(OBJECT_MAPPER.readValue(dbData, MAP_TYPE));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize event payload", e);
        }
    }
}

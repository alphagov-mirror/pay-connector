package uk.gov.pay.connector.gatewayaccount.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@Converter
public class CredentialsConverter implements AttributeConverter<Map<String,String>, PGobject> {
    
    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PGobject convertToDatabaseColumn(Map<String,String> credentials) {
        PGobject pgCredentials = new PGobject();
        pgCredentials.setType("json");
        try {
            pgCredentials.setValue(objectMapper.writeValueAsString(credentials));
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return pgCredentials;
    }

    @Override
    public Map<String,String> convertToEntityAttribute(PGobject dbCredentials) {
        try {
            return objectMapper.readValue(dbCredentials.toString(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

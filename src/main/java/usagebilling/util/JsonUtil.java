package usagebilling.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {

    private static final ObjectMapper MAPPER =
            createMapper();

    private JsonUtil() {
    }

    private static ObjectMapper createMapper() {

        ObjectMapper mapper =
                new ObjectMapper();

        mapper.registerModule(
                new JavaTimeModule()
        );
        mapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        return mapper;
    }

    public static <T> T fromJson(
            String json,
            Class<T> clazz) throws Exception {

        return MAPPER.readValue(
                json,
                clazz
        );
    }

    public static String toJson(
            Object object) throws Exception {

        return MAPPER.writeValueAsString(
                object
        );
    }
}
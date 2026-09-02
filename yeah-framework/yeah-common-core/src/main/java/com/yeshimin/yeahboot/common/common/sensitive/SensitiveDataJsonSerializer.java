package com.yeshimin.yeahboot.common.common.sensitive;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * 敏感数据响应序列化器
 */
public class SensitiveDataJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final SensitiveData sensitiveData;

    public SensitiveDataJsonSerializer() {
        this(null);
    }

    private SensitiveDataJsonSerializer(SensitiveData sensitiveData) {
        this.sensitiveData = sensitiveData;
    }

    @Override
    public void serialize(String value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        if (sensitiveData != null && SensitiveDataUtils.hasScene(sensitiveData, SensitiveScene.RESP)) {
            generator.writeString(SensitiveDataUtils.mask(value, sensitiveData.type()));
            return;
        }
        generator.writeString(value);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializers, BeanProperty property)
            throws JsonMappingException {
        SensitiveData annotation = property == null ? null : property.getAnnotation(SensitiveData.class);
        return new SensitiveDataJsonSerializer(annotation);
    }
}

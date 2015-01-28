package io.innerloop.neo4j.ogm.metadata;

import java.lang.reflect.Field;

import io.innerloop.neo4j.ogm.metadata.converters.Converter;

/**
 * Created by markangrish on 11/11/2014.
 */
public class PropertyMetadata
{
    private final String name;

    private final String fieldName;

    private final Class<?> type;

    private Converter converter;

    private final Field field;

    public PropertyMetadata(String name, String fieldName, Class<?> type, Field field)
    {
        this.name = name;
        this.fieldName = fieldName;
        this.type = type;
        this.field = field;
        this.field.setAccessible(true);
    }
}

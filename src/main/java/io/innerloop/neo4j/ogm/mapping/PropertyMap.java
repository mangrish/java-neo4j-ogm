package io.innerloop.neo4j.ogm.mapping;


import io.innerloop.neo4j.ogm.mapping.converters.Converter;

import java.lang.reflect.Field;

/**
 * Created by markangrish on 11/11/2014.
 */
public class PropertyMap
{
    private final String name;

    private final String fieldName;

    private final Class<?> type;

    private Converter converter;

    private final Field field;

    public PropertyMap(String name, String fieldName, Class<?> type, Field field)
    {
        this.name = name;
        this.fieldName = fieldName;
        this.type = type;
        this.field = field;
        this.field.setAccessible(true);
    }
}

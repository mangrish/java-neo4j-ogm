package io.innerloop.neo4j.ogm.metadata;

import io.innerloop.neo4j.ogm.Utils;
import io.innerloop.neo4j.ogm.metadata.converters.Converter;

import java.lang.reflect.Field;

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


    public PropertyMetadata(String name, Field field)
    {
        this.name = name;
        this.fieldName = field.getName();
        this.type = field.getType();
        this.field = field;
        this.field.setAccessible(true);
    }

    public PropertyMetadata(Field field)
    {
        this(field.getName(), field);
    }

    public String getName()
    {
        return name;
    }

    public Object toJson(Object entity)
    {
        Object o;
        try
        {
            o = field.get(entity);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could not access this field. Set this field to accessible");
        }
        return o;
    }

    public void setValue(Object value, Object instance)
    {
        try
        {
            field.set(instance, value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could not access this field. Set this field to accessible");
        }

    }
}

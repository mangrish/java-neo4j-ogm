package io.innerloop.neo4j.ogm.impl.metadata;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.converters.Converter;

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

        if (field.isAnnotationPresent(Convert.class))
        {
            Class<?> converterCls = field.getAnnotation(Convert.class).value();
            try
            {
                this.converter = (Converter) converterCls.newInstance();
            }
            catch (InstantiationException | IllegalAccessException e)
            {
                throw new RuntimeException("Could not find converter class: " + converterCls.getName(), e);
            }
        }

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
            if (converter != null)
            {
                o = converter.serialize(entity);
            }
            else {
                o = field.get(entity);
            }
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
            Object val = value;
            if (converter != null)
            {
                val = converter.deserialize(value);
            }
            field.set(instance, val);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could not access this field. Set this field to accessible");
        }

    }

    public Object getValue(Object ref)
    {
        Object o;
        try
        {
            o = field.get(ref);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could not access this field. Set this field to accessible");
        }
        return o;
    }
}

package io.innerloop.neo4j.ogm.impl.metadata;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Indexed;
import io.innerloop.neo4j.ogm.converters.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Created by markangrish on 11/11/2014.
 */
public class PropertyMetadata
{
    private static final Logger LOG = LoggerFactory.getLogger(PropertyMetadata.class);

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
        LOG.trace("Field [{}] with name: [{}] of type: [{}] added as a property.",
                  name,
                  fieldName,
                  type.getSimpleName());
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

            if (converter != null && o != null)
            {
                o = converter.serialize(o);
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
            else if (type.isEnum())
            {
                val = Enum.valueOf((Class<Enum>) type, (String) value);
            }
            else if (Long.class.isAssignableFrom(type) && value != null)
            {
                val = ((Number) value).longValue();
            }
            LOG.debug("Field [{}] of type: [{}] SET with value: [{}] of type [{}].",
                      field.getName(),
                      field.getType().getSimpleName(),
                      val,
                      val.getClass().getSimpleName());
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
            LOG.debug("Field [{}] of type: [{}] RETRIEVED with value: [{}].",
                      field.getName(),
                      field.getType().getSimpleName(),
                      o);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could not access this field. Set this field to accessible");
        }
        return o;
    }
}

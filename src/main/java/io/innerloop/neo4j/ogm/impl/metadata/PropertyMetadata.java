package io.innerloop.neo4j.ogm.impl.metadata;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.converters.Converter;
import io.innerloop.neo4j.ogm.impl.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by markangrish on 11/11/2014.
 */
public class PropertyMetadata
{
    private static final Logger LOG = LoggerFactory.getLogger(PropertyMetadata.class);

    private final String name;

    private final String fieldName;

    private final Class<?> type;

    private Class<?> paramterizedType;

    private Converter converter;

    private final Field field;


    public PropertyMetadata(String name, Field field)
    {
        this.name = name;
        this.fieldName = field.getName();
        this.type = field.getType();
        this.field = field;
        if (Iterable.class.isAssignableFrom(type))
        {
            paramterizedType = ReflectionUtils.getParameterizedType(field);
        }
        if (field.isAnnotationPresent(Convert.class))
        {
            Class<?> converterCls = field.getAnnotation(Convert.class).value();
            try
            {
                this.converter = (Converter) converterCls.newInstance();
            }
            catch (InstantiationException | IllegalAccessException e)
            {
                throw new RuntimeException("Could not instantiate converter class: [" + converterCls.getName() +
                                           "] for field: [" + fieldName + "] on class: [" + field.getDeclaringClass() +
                                           "]", e);
            }
        }

        this.field.setAccessible(true);
        LOG.trace("Field [{}] with name: [{}] of type: [{}] added as a property to class [{}]",
                  name,
                  fieldName,
                  type.getSimpleName(),
                  field.getDeclaringClass());
    }

    public PropertyMetadata(Field field)
    {
        this(field.getName(), field);
    }

    public String getName()
    {
        return name;
    }

    public Class<?> getParamterizedType()
    {
        return paramterizedType;
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
            throw new RuntimeException("Could not access field: [" + fieldName + "] on class: [" +
                                       field.getDeclaringClass() +
                                       "]. Does this field exist and is it accessible?", e);
        }
        return o;
    }

    public void setValue(Object value, Object instance)
    {
        try
        {
            Object val = value;
            if (val != null)
            {
                if (converter != null)
                {
                    val = converter.deserialize(value);
                }
                else if (type.isEnum())
                {
                    val = Enum.valueOf((Class<Enum>) type, (String) value);
                }
                else if (List.class.isAssignableFrom(type))
                {
                    val = new ArrayList<>();
                }
                else if (Set.class.isAssignableFrom(type))
                {
                    val = new HashSet<>();
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
            }
            else {
                LOG.debug("Field [{}] of type: [{}] SET with null value.",
                          field.getName(),
                          field.getType().getSimpleName());
            }
            field.set(instance, val);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could set the value of field: [" + fieldName + "] on class: [" +
                                       field.getDeclaringClass() +
                                       "] for object [" + instance + "] with value: [" + value + "]", e);
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
            throw new RuntimeException("Could get the value of field: [" + fieldName + "] on class: [" +
                                       field.getDeclaringClass() +
                                       "] for object [" + ref + "]", e);
        }
        return o;
    }
}

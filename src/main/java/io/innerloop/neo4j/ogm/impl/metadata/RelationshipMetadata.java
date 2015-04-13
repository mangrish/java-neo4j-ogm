package io.innerloop.neo4j.ogm.impl.metadata;

import io.innerloop.neo4j.ogm.annotations.Fetch;
import io.innerloop.neo4j.ogm.annotations.Relationship;
import io.innerloop.neo4j.ogm.impl.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Created by markangrish on 28/01/2015.
 */
public class RelationshipMetadata
{
    private final Field field;

    private final Relationship.Direction direction;

    private final String fieldName;

    private final Class<?> type;

    private boolean collection;

    private Class<?> paramterizedType;

    private boolean fetchEnabled;

    private String name;


    public RelationshipMetadata(String name, Relationship.Direction direction, Field field)
    {
        this.name = name;
        this.fieldName = field.getName();
        this.type = field.getType();
        this.field = field;
        this.direction = direction;

        if (field.isAnnotationPresent(Fetch.class))
        {
            this.fetchEnabled = true;
        }

        if (Collection.class.isAssignableFrom(field.getType()))
        {
            collection = true;
            paramterizedType = ReflectionUtils.getParameterizedType(field);
        }

        this.field.setAccessible(true);
    }

    public boolean isCollection()
    {
        return collection;
    }

    public Class<?> getParamterizedType()
    {
        return paramterizedType;
    }

    public boolean isFetchEnabled()
    {
        return fetchEnabled;
    }

    public <T> Object getValue(T entity)
    {
        try
        {
            return field.get(entity);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could get the value of field: [" + field.getName() + "] on class: [" +
                                       field.getDeclaringClass() +
                                       "] for object [" + entity + "]", e);
        }
    }

    public String getName()
    {
        return name;
    }

    public Relationship.Direction getDirection()
    {
        return direction;
    }

    public void setValue(Object value, Object instance)
    {
        try
        {
            field.set(instance, value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could set the value of field: [" + fieldName + "] on class: [" +
                                       field.getDeclaringClass() +
                                       "] for object [" + instance + "] with value: [" + value + "]", e);
        }
    }

    public Class<?> getType()
    {
        return type;
    }
}

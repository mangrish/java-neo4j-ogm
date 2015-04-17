package io.innerloop.neo4j.ogm.impl.metadata;

import io.innerloop.neo4j.ogm.annotations.Fetch;
import io.innerloop.neo4j.ogm.annotations.Relationship;
import io.innerloop.neo4j.ogm.impl.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by markangrish on 28/01/2015.
 */
public class RelationshipMetadata
{
    private final Field field;

    private final Relationship.Direction direction;

    private final String fieldName;

    private final Class<?> type;

    private boolean map;

    private boolean collection;

    private Class<?>[] paramterizedTypes;

    private boolean fetchEnabled;

    private String name;

    private Map<String, Object> properties;


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
            paramterizedTypes = ReflectionUtils.getParameterizedTypes(field);
        }
        if (Map.class.isAssignableFrom(field.getType()))
        {
            map = true;
            paramterizedTypes = ReflectionUtils.getParameterizedTypes(field);
            properties = new HashMap<>();
        }

        this.field.setAccessible(true);
    }

    public boolean isCollection()
    {
        return collection;
    }

    public boolean isMap()
    {
        return map;
    }

    public Class<?>[] getParamterizedTypes()
    {
        return paramterizedTypes;
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

    public Map<String, Object> getProperties()
    {
        return properties;
    }
}

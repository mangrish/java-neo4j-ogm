package io.innerloop.neo4j.ogm.impl.metadata;

import io.innerloop.neo4j.ogm.annotations.Relationship;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Created by markangrish on 28/01/2015.
 */
public class RelationshipMetadata
{
    private final Field field;

    private final Relationship.Direction  direction;

    private String type;

    private final boolean collection;

    public RelationshipMetadata(String type, Relationship.Direction direction, Field field)
    {
        this.type = type;
        this.collection = Collection.class.isAssignableFrom(field.getType());
        this.field = field;
        this.field.setAccessible(true);
        this.direction = direction;
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

    public String getType()
    {
        return type;
    }

    public Relationship.Direction getDirection()
    {
        return direction;
    }

    public boolean isCollection()
    {
        return collection;
    }

    public Field getField()
    {
        return field;
    }

    public void setValue(Object value, Object instance)
    {
        try
        {
            field.set(instance, value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could set the value of field: [" + field.getName() + "] on class: [" +
                                       field.getDeclaringClass() +
                                       "] for object [" + instance + "] with value: [" + value + "]", e);
        }
    }
}

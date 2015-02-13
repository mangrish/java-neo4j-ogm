package io.innerloop.neo4j.ogm.metadata;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Created by markangrish on 28/01/2015.
 */
public class RelationshipMetadata
{
    private final Field field;
    private String type;
    private final boolean collection;
    public RelationshipMetadata(String type, Field field)
    {
        this.type = type;
        this.collection = Collection.class.isAssignableFrom(field.getType());
        this.field = field;
        this.field.setAccessible(true);

    }

    public <T> Object getValue(T entity)
    {
        try
        {
            return field.get(entity);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Field is not accessible.", e);
        }
    }

    public String getType()
    {
        return type;
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
            throw new RuntimeException("Field is not accessible.", e);
        }
    }
}

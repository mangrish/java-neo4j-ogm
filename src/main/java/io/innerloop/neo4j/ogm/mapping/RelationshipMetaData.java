package io.innerloop.neo4j.ogm.mapping;


import io.innerloop.neo4j.ogm.mapping.converters.Converter;

/**
 * Created by markangrish on 07/11/2014.
 */
public class RelationshipMetaData<T>
{
    private final Class<T> cls;

    public RelationshipMetaData(Class<T> cls)
    {

        this.cls = cls;
    }

    public Class<T> getCls()
    {
        return cls;
    }

    public void registerConverter(Class<?> aClass, Converter converter)
    {

    }
}

package io.innerloop.neo4j.ogm.mapping;


import io.innerloop.neo4j.ogm.mapping.converters.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by markangrish on 07/11/2014.
 */
public class NodeMetaData<T>
{
    private final Class<? super T> superType;

    /**
     * This acts as a unique key. labels must be in sorted (alphabetically) order for this to work.
     */
    private final MetaDataLabelKey key;

    private final Class<T> type;

    private final Map<String, PropertyMap> fields;

    public NodeMetaData(MetaDataLabelKey key, Class<T> type)
    {
        this.key = key;
        this.type = type;
        this.superType = type.getSuperclass();
        this.fields = new HashMap<>();
    }

    public Class<T> getType()
    {
        return type;
    }


    public PropertyMap getField(String name)
    {
        return fields.get(name);
    }

    public Class<? super T> getSuperClass()
    {
        return superType;
    }

    private void registerConverter(Class<?> aClass, Converter converter)
    {

    }
}

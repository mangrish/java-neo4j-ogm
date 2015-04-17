package io.innerloop.neo4j.ogm.impl.metadata;

import io.innerloop.neo4j.client.spi.impl.rest.json.JSONObject;
import io.innerloop.neo4j.ogm.impl.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by markangrish on 28/01/2015.
 */
public class RelationshipPropertiesClassMetadata<T>
{
    private static final Logger LOG = LoggerFactory.getLogger(RelationshipPropertiesClassMetadata.class);

    private static final long HASH_SEED = 0xDEADBEEF / (11 * 257);

    private static long hash(String string)
    {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++)
        {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }

    private final Class<T> type;

    private final Map<String, PropertyMetadata> propertyMetadata;

    public RelationshipPropertiesClassMetadata(Class<T> type)
    {
        this.type = type;
        this.propertyMetadata = new HashMap<>();

        for (Field field : ReflectionUtils.getAllFields(type))
        {
            if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
            {
                continue;
            }

            String fieldName = field.getName();
            PropertyMetadata pm = new PropertyMetadata(field);
            propertyMetadata.put(fieldName, pm);
        }

        LOG.debug("Relationship Properties Class [{}] added.", type.getSimpleName());
    }

    public JSONObject toJsonObject(Object entity)
    {
        JSONObject result = new JSONObject();
        propertyMetadata.values().forEach(pm -> result.put(pm.getName(), pm.getValue(entity)));
        LOG.trace("Converted object of type: [{}] to JSON: {}", type.getSimpleName(), result);
        return result;
    }


    public T createInstance(Map<String, Object> properties)
    {
        try
        {
            LOG.debug("Instantiating new instance of: [{}]", type.getSimpleName());
            T instance = type.newInstance();

            for (Map.Entry<String, Object> entry : properties.entrySet())
            {
                PropertyMetadata pm = propertyMetadata.get(entry.getKey());
                if (pm != null)
                {
                    pm.setValue(entry.getValue(), instance);
                }
            }

            return instance;
        }
        catch (InstantiationException ie)
        {
            throw new RuntimeException("Could not instantiate class class due to missing default constructor on class: " +
                                       type.getName(), ie);
        }
        catch (IllegalAccessException iae)
        {
            throw new RuntimeException("OGM does not have access to instantiate the class: " + type.getName(), iae);
        }
    }

    public long hash(T object)
    {
        long hash = HASH_SEED;
        for (PropertyMetadata metadata : propertyMetadata.values())
        {
            Object value = metadata.getValue(object);
            if (value != null)
            {
                hash = hash * 31L + hash(value.toString());
            }
        }

        return hash;
    }

    public PropertyMetadata getProperty(String key)
    {
        return propertyMetadata.get(key);
    }
}

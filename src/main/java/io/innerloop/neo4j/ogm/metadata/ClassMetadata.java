package io.innerloop.neo4j.ogm.metadata;

import com.google.common.base.CaseFormat;
import io.innerloop.neo4j.client.json.JSONObject;
import io.innerloop.neo4j.ogm.Utils;
import io.innerloop.neo4j.ogm.annotations.Property;
import io.innerloop.neo4j.ogm.annotations.Relationship;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by markangrish on 28/01/2015.
 */
public class ClassMetadata<T>
{
    private final Class<T> type;

    private final String primaryLabel;

    private final SortedMultiLabel key;

    private final Map<String, PropertyMetadata> propertyMetadata;

    private final Map<String, RelationshipMetadata> relationshipMetadata;

    public ClassMetadata(Class<T> type, MetadataMap metadataMap, String primaryLabel, SortedMultiLabel key)
    {
        this.type = type;
        this.primaryLabel = primaryLabel;
        this.key = key;
        this.propertyMetadata = new HashMap<>();
        this.relationshipMetadata = new HashMap<>();

        for (Field field : Utils.getFields(type))
        {
            if (Modifier.isTransient(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
            {
                continue;
            }
            else if (field.isAnnotationPresent(Property.class) &&
                Utils.isNotEmpty(field.getAnnotation(Property.class).name()))
            {
                PropertyMetadata pm = new PropertyMetadata(field.getAnnotation(Property.class).name(), field);
                propertyMetadata.put(pm.getName(), pm);
            }
            else if (field.isAnnotationPresent(Relationship.class) &&
                Utils.isNotEmpty(field.getAnnotation(Relationship.class).type()))
            {
                RelationshipMetadata rm = new RelationshipMetadata(field.getAnnotation(Relationship.class).type(), field);
                relationshipMetadata.put(rm.getType(), rm);
            }
            else
            {
                Class cls = field.getType();
                ClassMetadata classMetadata = metadataMap.get(cls);

                if (classMetadata == null)
                {
                    propertyMetadata.put(field.getName(), new PropertyMetadata(field));
                }
                else
                {
                    String relType = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, field.getName());
                    relationshipMetadata.put(relType, new RelationshipMetadata(relType, field));
                }
            }
        }
    }


    public SortedMultiLabel getLabelKey()
    {
        return key;
    }

    public JSONObject toJsonObject(T entity)
    {
        JSONObject result = new JSONObject();

        for (PropertyMetadata pm : propertyMetadata.values())
        {
            result.put(pm.getName(), pm.toJson(entity));
        }

        return result;
    }

    public T createInstance(Map<String, Object> properties)
    {
        try
        {
            T instance = type.newInstance();

            for (Map.Entry<String, Object> entry : properties.entrySet())
            {
                PropertyMetadata pm = propertyMetadata.get(entry.getKey());
                pm.setValue(entry.getValue(), instance);
            }

            return instance;
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            throw new RuntimeException("Could not instantiate class class");
        }
    }

    public Iterable<RelationshipMetadata> getRelationships()
    {
        return relationshipMetadata.values();
    }

    public RelationshipMetadata getRelationship(String relationshipType)
    {
        return relationshipMetadata.get(relationshipType);
    }
}

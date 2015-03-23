package io.innerloop.neo4j.ogm.impl.metadata;

import com.google.common.base.CaseFormat;
import io.innerloop.neo4j.client.json.JSONObject;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.annotations.Property;
import io.innerloop.neo4j.ogm.annotations.Relationship;
import io.innerloop.neo4j.ogm.impl.util.ReflectionUtils;
import io.innerloop.neo4j.ogm.impl.util.StringUtils;

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

    private PropertyMetadata primaryField;

    private final String primaryLabel;

    private final SortedMultiLabel labelKey;

    private final Map<String, PropertyMetadata> propertyMetadata;

    private final Map<String, RelationshipMetadata> relationshipMetadata;

    private PropertyMetadata neo4jIdField;

    public ClassMetadata(Class<T> type, List<Class<?>> metadataMap, String primaryLabel, SortedMultiLabel labelKey)
    {
        this.type = type;
        this.primaryLabel = primaryLabel;
        this.labelKey = labelKey;
        this.propertyMetadata = new HashMap<>();
        this.relationshipMetadata = new HashMap<>();

        for (Field field : ReflectionUtils.getFields(type))
        {
            if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
            {
                continue;
            }
            else if (field.getName().equals("id"))
            {
                if (!field.getType().equals(Long.class))
                {
                    throw new IllegalStateException("No id object with type Long found on object.");
                }
                PropertyMetadata pm = new PropertyMetadata(field);
                propertyMetadata.put("id", pm);
                this.neo4jIdField = pm;
            }
            else if (field.isAnnotationPresent(Property.class) &&
                     StringUtils.isNotEmpty(field.getAnnotation(Property.class).name()))
            {
                PropertyMetadata pm = new PropertyMetadata(field.getAnnotation(Property.class).name(), field);
                propertyMetadata.put(pm.getName(), pm);
            }
            else if (field.isAnnotationPresent(Relationship.class) &&
                     StringUtils.isNotEmpty(field.getAnnotation(Relationship.class).type()))
            {
                RelationshipMetadata rm = new RelationshipMetadata(field.getAnnotation(Relationship.class).type(),
                                                                   field);
                relationshipMetadata.put(rm.getType(), rm);
            }
            else
            {
                Class cls = field.getType();
                if (!Iterable.class.isAssignableFrom(cls) && !metadataMap.contains(cls))
                {
                    String fieldName = field.getName();
                    PropertyMetadata pm = new PropertyMetadata(field);
                    propertyMetadata.put(fieldName, pm);

                    if (fieldName.equals("uuid") || field.isAnnotationPresent(Id.class))
                    {
                        this.primaryField = pm;
                    }
                }
                else
                {
                    String relType = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, field.getName());
                    relationshipMetadata.put(relType, new RelationshipMetadata(relType, field));
                }
            }
        }

        if (primaryField == null)
        {
            throw new IllegalStateException("No Primary Field was detected. A field called uuid or " +
                                            "annotated with @Id is required");
        }

        if (neo4jIdField == null)
        {
            throw new IllegalStateException("No Neo4j Id was detected. A field called id of type Long is required");
        }
    }


    public SortedMultiLabel getLabelKey()
    {
        return labelKey;
    }

    public JSONObject toJsonObject(Object entity)
    {
        JSONObject result = new JSONObject();

        for (PropertyMetadata pm : propertyMetadata.values())
        {
            result.put(pm.getName(), pm.toJson(entity));
        }

        return result;
    }

    public PropertyMetadata getPrimaryField()
    {
        return primaryField;
    }

    public T createInstance(Long id, Map<String, Object> properties)
    {
        try
        {
            T instance = type.newInstance();

            //TODO: could get rid of this if dirty updates dont need it
            PropertyMetadata idPm = propertyMetadata.get("id");
            idPm.setValue(id, instance);

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

    public PropertyMetadata getNeo4jIdField()
    {
        return neo4jIdField;
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
}

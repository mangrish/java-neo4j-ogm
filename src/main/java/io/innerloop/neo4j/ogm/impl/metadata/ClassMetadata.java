package io.innerloop.neo4j.ogm.impl.metadata;

import com.google.common.base.CaseFormat;
import io.innerloop.neo4j.client.spi.impl.rest.json.JSONObject;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.annotations.Indexed;
import io.innerloop.neo4j.ogm.annotations.Property;
import io.innerloop.neo4j.ogm.annotations.Relationship;
import io.innerloop.neo4j.ogm.impl.index.Index;
import io.innerloop.neo4j.ogm.impl.util.ReflectionUtils;
import io.innerloop.neo4j.ogm.impl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by markangrish on 28/01/2015.
 */
public class ClassMetadata<T>
{
    private static final Logger LOG = LoggerFactory.getLogger(ClassMetadata.class);

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

    private final Map<String, Index> indexes;

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
        this.indexes = new HashMap<>();

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
                    throw new IllegalStateException("No object called 'id' with type Long found on class [" +
                                                    type.getName() + "]");
                }
                PropertyMetadata pm = new PropertyMetadata(field);
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
                Relationship relationship = field.getAnnotation(Relationship.class);
                RelationshipMetadata rm = new RelationshipMetadata(relationship.type(),
                                                                   relationship.direction(),
                                                                   field);
                relationshipMetadata.put(rm.getType(), rm);
            }
            else
            {
                Class cls = field.getType();
                boolean isRelationshipClass = false;
                for (Class<?> c : metadataMap)
                {
                    if (cls.isAssignableFrom(c))
                    {
                        isRelationshipClass = true;
                    }
                }

                if (!Iterable.class.isAssignableFrom(cls) && !isRelationshipClass)
                {
                    String fieldName = field.getName();
                    PropertyMetadata pm = new PropertyMetadata(field);
                    propertyMetadata.put(fieldName, pm);

                    if (fieldName.equals("uuid") || field.isAnnotationPresent(Id.class))
                    {
                        this.primaryField = pm;
                        this.indexes.put(fieldName, new Index(primaryLabel, fieldName, true));
                    }
                }
                else
                {
                    String relType = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, field.getName());
                    relationshipMetadata.put(relType,
                                             new RelationshipMetadata(relType,
                                                                      Relationship.Direction.UNDIRECTED,
                                                                      field));
                }
            }

            Indexed indexed = field.getAnnotation(Indexed.class);
            if (indexed != null)
            {
                this.indexes.put(field.getName(), new Index(primaryLabel, field.getName(), indexed.unique()));
            }
        }

        if (primaryField == null)
        {
            throw new IllegalStateException("No Primary Field was detected for class: [" + type.getName() +
                                            "]. A field called 'uuid' or annotated with @Id is required");
        }

        if (neo4jIdField == null)
        {
            throw new IllegalStateException("No Neo4j Id was detected for class: [" + type.getName() +
                                            "]. A field called id of type Long is required");
        }

        LOG.debug("Class [{}] with labels: [{}] added. Primary key is: [{}].",
                  type.getSimpleName(),
                  labelKey.asCypher(),
                  primaryField.getName());
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
        LOG.trace("Converted object of type: [{}] to JSON: {}", type.getSimpleName(), result);
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
            LOG.debug("Instantiating new instance of: [{}]", type.getSimpleName());
            T instance = type.newInstance();

            //TODO: could get rid of this if dirty updates dont need it
            neo4jIdField.setValue(id, instance);

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

    public Collection<Index> getIndexes()
    {
        return indexes.values();
    }
}

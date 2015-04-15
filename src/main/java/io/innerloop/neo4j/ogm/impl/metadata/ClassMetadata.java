package io.innerloop.neo4j.ogm.impl.metadata;

import com.google.common.base.CaseFormat;
import com.google.common.primitives.Primitives;
import io.innerloop.neo4j.client.spi.impl.rest.json.JSONObject;
import io.innerloop.neo4j.ogm.annotations.Aggregate;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.annotations.Indexed;
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

    private PropertyMetadata primaryIdField;

    private PropertyMetadata neo4jIdField;

    private final NodeLabel nodeLabel;

    private final boolean aggregate;

    private final Map<String, Index> indexes;

    private final Map<String, PropertyMetadata> propertyMetadata;

    private final Map<String, RelationshipMetadata> relationshipMetadata;

    public ClassMetadata(Class<T> type, List<Class<?>> managedClasses, String primaryLabel, NodeLabel nodeLabel)
    {
        this.type = type;
        this.nodeLabel = nodeLabel;
        this.propertyMetadata = new HashMap<>();
        this.relationshipMetadata = new HashMap<>();
        this.indexes = new HashMap<>();
        this.aggregate = type.isAnnotationPresent(Aggregate.class);

        for (Field field : ReflectionUtils.getAllFields(type))
        {
            if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
            {
                continue;
            }

            Class<?> fieldClass = field.getType();
            String fieldName = field.getName();
            boolean isCollectionType = Iterable.class.isAssignableFrom(fieldClass);

            if (fieldName.equals("id") && fieldClass.equals(Long.class))
            {
                this.neo4jIdField = new PropertyMetadata(field);
                continue;
            }

            if (field.isAnnotationPresent(Id.class))
            {
                this.primaryIdField = new PropertyMetadata(field);
                this.propertyMetadata.put(fieldName, primaryIdField);
                this.indexes.put(fieldName, new Index(primaryLabel, fieldName, true));
                continue;
            }

            Relationship relationship = field.getAnnotation(Relationship.class);
            if (relationship != null)
            {
                String relationshipType = StringUtils.isNotEmpty(relationship.type()) ?
                                                  relationship.type() :
                                                  CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, fieldName);
                RelationshipMetadata rm = new RelationshipMetadata(relationshipType, relationship.direction(), field);
                relationshipMetadata.put(rm.getName(), rm);
                continue;
            }

            Class parametrizedCls = null;
            if (isCollectionType)
            {
                parametrizedCls = ReflectionUtils.getParameterizedType(field);
            }

            boolean isRelationshipClass = false;
            for (Class<?> c : managedClasses)
            {
                if (fieldClass.isAssignableFrom(c))
                {
                    isRelationshipClass = true;
                }
            }

            if ((isCollectionType && (parametrizedCls != null && !(Primitives.isWrapperType(parametrizedCls) ||
                                                                   String.class.isAssignableFrom(parametrizedCls) || parametrizedCls.isEnum()))) ||
                isRelationshipClass)
            {
                String relType = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, fieldName);
                RelationshipMetadata rm = new RelationshipMetadata(relType, Relationship.Direction.UNDIRECTED, field);
                relationshipMetadata.put(rm.getName(), rm);
            }
            else
            {
                PropertyMetadata pm = new PropertyMetadata(field);
                propertyMetadata.put(fieldName, pm);
            }

            Indexed indexed = field.getAnnotation(Indexed.class);
            if (indexed != null)
            {
                this.indexes.put(fieldName, new Index(primaryLabel, fieldName, indexed.unique()));
            }
        }

        if (primaryIdField == null)
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
                  nodeLabel.asCypher(),
                  primaryIdField.getName());
    }


    public NodeLabel getNodeLabel()
    {
        return nodeLabel;
    }

    public JSONObject toJsonObject(Object entity)
    {
        JSONObject result = new JSONObject();
        propertyMetadata.values().forEach(pm -> result.put(pm.getName(), pm.getValue(entity)));
        LOG.trace("Converted object of type: [{}] to JSON: {}", type.getSimpleName(), result);
        return result;
    }

    public PropertyMetadata getPrimaryIdField()
    {
        return primaryIdField;
    }

    public T createInstance(Long id, Map<String, Object> properties)
    {
        try
        {
            LOG.debug("Instantiating new instance of: [{}]", type.getSimpleName());
            T instance = type.newInstance();
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

    public boolean isAggregate()
    {
        return aggregate;
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

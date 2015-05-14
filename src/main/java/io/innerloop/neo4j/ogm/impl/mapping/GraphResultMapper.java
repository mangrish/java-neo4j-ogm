package io.innerloop.neo4j.ogm.impl.mapping;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.Node;
import io.innerloop.neo4j.client.Relationship;
import io.innerloop.neo4j.ogm.impl.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.MetadataMap;
import io.innerloop.neo4j.ogm.impl.metadata.NodeLabel;
import io.innerloop.neo4j.ogm.impl.metadata.PropertyMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.RelationshipMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.RelationshipPropertiesClassMetadata;
import io.innerloop.neo4j.ogm.impl.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by markangrish on 28/01/2015.
 */
public class GraphResultMapper
{
    private static final Logger LOG = LoggerFactory.getLogger(GraphResultMapper.class);

    private final IdentityMap identityMap;

    private final MetadataMap metadataMap;

    public GraphResultMapper(IdentityMap identityMap, MetadataMap metadataMap)
    {
        this.identityMap = identityMap;
        this.metadataMap = metadataMap;
    }

    public <T> List<T> map(Class<T> type, Graph graph, Map<String, Object> params)
    {
        LOG.trace("Mapping type: [{}] with [{}] nodes and [{}] relationships",
                  type.getSimpleName(),
                  graph.getNodes().size(),
                  graph.getRelationships().size());
        StopWatch sw = new StopWatch("Graph Result Mapping", LOG);
        sw.start();

        Map<Long, Object> objects = new HashMap<>();
        Map<Long, T> results = new HashMap<>();

        for (Node node : graph.getNodes())
        {
            String[] labels = node.getLabels();

            if (labels.length > 1)
            {
                Arrays.sort(labels);
            }

            NodeLabel key = new NodeLabel(labels);
            ClassMetadata clsMetadata = metadataMap.get(key);

            if (clsMetadata == null)
            {
                throw new RuntimeException("No Metadata available for this label/s: [" + key + "]");
            }

            Object instance = objects.get(node.getId());

            if (instance == null)
            {
                Map<String, Object> properties = node.getProperties();
                instance = clsMetadata.createInstance(node.getId(), properties);
                objects.put(node.getId(), instance);
            }

            if (type.isAssignableFrom(instance.getClass()))
            {
                if (params == null) // This means it's a load(Class) call.. this is a pretty bad semantic.
                {
                    results.put(node.getId(), (T) instance);
                }
                else
                {
                    for (Map.Entry<String, Object> e : params.entrySet())
                    {
                        PropertyMetadata property = clsMetadata.getProperty(e.getKey());

                        if (property == null) // this means it's from a cypher query.. again a bad semantic
                        {
                            results.put(node.getId(), (T) instance);
                        }
                        else
                        {
                            if (property.getRawValue(instance).equals(e.getValue()))
                            {
                                results.put(node.getId(), (T) instance);
                            }
                        }
                    }
                }
            }

        }
        sw.split("Nodes completed");
        for (Relationship relationship : graph.getRelationships())
        {
            Object start = objects.get(relationship.getStartNodeId());
            Object end = objects.get(relationship.getEndNodeId());
            connectRelationship(relationship, start, end);
            connectRelationship(relationship, end, start);
        }
        sw.split("Relationships done");
        for (Map.Entry<Long, Object> e : objects.entrySet())
        {
            Object existing = identityMap.get(e.getKey());

            if (existing == null)
            {
                identityMap.put(e.getKey(), e.getValue());
            }
        }

        List<T> filteredResults = new ArrayList<>();

        for (Map.Entry<Long, T> e : results.entrySet())
        {
            filteredResults.add((T) identityMap.get(e.getKey()));
        }

        sw.stop();
        return filteredResults;
    }

    private void connectRelationship(Relationship relationship, Object start, Object end)
    {
        String relationshipType = relationship.getType();
        ClassMetadata clsMetadata = metadataMap.get(start);
        RelationshipMetadata rm = clsMetadata.getRelationship(relationshipType);

        if (rm == null)
        {
            return;
        }

        if (rm.isCollection())
        {
            Collection collection = (Collection) rm.getValue(start);
            if (collection == null)
            {
                if (Set.class.isAssignableFrom(rm.getType()))
                {
                    collection = new HashSet<>();
                }
                else if (List.class.isAssignableFrom(rm.getType()))
                {
                    collection = new ArrayList<>();
                }
                else
                {
                    throw new RuntimeException("Unsupported Collection type [" + rm.getType().getName() + "]");
                }

                rm.setValue(collection, start);
            }
            collection.add(end);
        }
        else if (rm.isMap())
        {
            Map map = (Map) rm.getValue(start);
            if (map == null)
            {
                map = new HashMap<>();
            }
            Class<?> propertiesClass = rm.getParamterizedTypes()[1];
            RelationshipPropertiesClassMetadata rpcm = metadataMap.getRelationshipPropertiesClassMetadata(propertiesClass);
            Object relationshipProperties = rpcm.createInstance(relationship.getProperties());
            map.put(end, relationshipProperties);
            rm.setValue(map, start);
        }
        else
        {
            rm.setValue(end, start);
        }
    }
}

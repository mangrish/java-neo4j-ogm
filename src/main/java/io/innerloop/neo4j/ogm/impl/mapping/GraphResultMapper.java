package io.innerloop.neo4j.ogm.impl.mapping;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.Node;
import io.innerloop.neo4j.client.Relationship;
import io.innerloop.neo4j.ogm.impl.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.MetadataMap;
import io.innerloop.neo4j.ogm.impl.metadata.NodeLabel;
import io.innerloop.neo4j.ogm.impl.metadata.RelationshipMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.RelationshipPropertiesClassMetadata;

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
    private final IdentityMap identityMap;

    private final MetadataMap metadataMap;

    public GraphResultMapper(IdentityMap identityMap, MetadataMap metadataMap)
    {
        this.identityMap = identityMap;
        this.metadataMap = metadataMap;
    }

    public <T> List<T> map(Class<T> type, Graph graph, Map<String, Object> params)
    {
        Map<Long, Object> createRelationshipsFor = new HashMap<>();
        List<T> results = new ArrayList<>();

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

            Object instance = identityMap.get(node.getId());

            if (instance == null)
            {
                Map<String, Object> properties = node.getProperties();
                instance = clsMetadata.createInstance(node.getId(), properties);
                identityMap.put(node.getId(), instance);
                createRelationshipsFor.put(node.getId(), instance);
            }

            if (type.isAssignableFrom(instance.getClass()))
            {
                if (params == null) // This means it's a load(Class) call.. this is a pretty bad semantic.
                {
                    results.add((T) instance);
                }
                else
                {
                    final Object finalInstance = instance;
                    long count = params.entrySet()
                                         .stream()
                                         .filter(e -> clsMetadata.getProperty(e.getKey())
                                                              .getValue(finalInstance)
                                                              .equals(e.getValue()))
                                         .count();
                    if (count == 1)
                    {
                        results.add((T) instance);
                    }
                }
            }

        }
        for (Relationship relationship : graph.getRelationships())
        {
            Object start = identityMap.get(relationship.getStartNodeId());
            Object end = identityMap.get(relationship.getEndNodeId());

            boolean createRelationship = createRelationshipsFor.containsKey(relationship.getStartNodeId());

            if (start == null || !createRelationship)
            {
                continue;
            }

            String relationshipType = relationship.getType();
            ClassMetadata clsMetadata = metadataMap.get(start);
            RelationshipMetadata rm = clsMetadata.getRelationship(relationshipType);

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
                    rm.setValue(map, start);
                }
                Class<?> propertiesClass = rm.getParamterizedTypes()[1];
                RelationshipPropertiesClassMetadata rpcm = metadataMap.getRelationshipPropertiesClassMetadata(propertiesClass);
                Object relationshipProperties = rpcm.createInstance(relationship.getProperties());
                map.put(end, relationshipProperties);
            }
            else
            {
                rm.setValue(end, start);
            }

        }
        return results;
    }
}

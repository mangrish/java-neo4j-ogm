package io.innerloop.neo4j.ogm.impl.mapping;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.Node;
import io.innerloop.neo4j.client.Relationship;
import io.innerloop.neo4j.ogm.impl.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.MetadataMap;
import io.innerloop.neo4j.ogm.impl.metadata.RelationshipMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.SortedMultiLabel;

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
    private final Map<Long, Object> identityMap;

    private final MetadataMap metadataMap;

    public GraphResultMapper(Map<Long, Object> identityMap, MetadataMap metadataMap)
    {
        this.identityMap = identityMap;
        this.metadataMap = metadataMap;
    }

    public <T> List<T> map(Class<T> type, Graph graph)
    {
        List<T> results = new ArrayList<>();

        for (Node node : graph.getNodes())
        {
            String[] labels = node.getLabels();

            if (labels.length > 1)
            {
                Arrays.sort(labels);
            }

            SortedMultiLabel key = new SortedMultiLabel(labels);
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
            }


            if (type.isAssignableFrom(instance.getClass()))
            {
                results.add((T) instance);
            }

        }
        for (Relationship relationship : graph.getRelationships())
        {
            Object start = identityMap.get(relationship.getStartNodeId());
            Object end = identityMap.get(relationship.getEndNodeId());

            try
            {
                String relationshipType = relationship.getType();
                ClassMetadata clsMetadata = metadataMap.get(start);
                RelationshipMetadata rm = clsMetadata.getRelationship(relationshipType);

                if (rm.isCollection())
                {
                    Collection collection = (Collection) rm.getField().get(start);
                    if (collection == null)
                    {
                        if (Set.class.isAssignableFrom(rm.getField().getType()))
                        {
                            collection = new HashSet<>();
                        }
                        else if (List.class.isAssignableFrom(rm.getField().getType()))
                        {
                            collection = new ArrayList<>();
                        }
                        rm.setValue(collection, start);
                    }
                    collection.add(end);
                }
                else
                {
                    rm.setValue(end, start);
                }

            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException("No field found on object", e);
            }
        }
        return results;
    }
}

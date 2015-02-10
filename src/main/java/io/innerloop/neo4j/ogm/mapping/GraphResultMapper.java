package io.innerloop.neo4j.ogm.mapping;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.Node;
import io.innerloop.neo4j.client.Relationship;
import io.innerloop.neo4j.ogm.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.metadata.MetadataMap;
import io.innerloop.neo4j.ogm.metadata.RelationshipMetadata;
import io.innerloop.neo4j.ogm.metadata.SortedMultiLabel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by markangrish on 28/01/2015.
 */
public class GraphResultMapper
{
    private final MetadataMap metadataMap;

    public GraphResultMapper(MetadataMap metadataMap)
    {
        this.metadataMap = metadataMap;
    }

    public <T> List<T> map(Class<T> type, Graph graph)
    {
        Map<Long, Object> seenObjects = new HashMap<>();
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

            Map<String, Object> properties = node.getProperties();
            Object instance = clsMetadata.createInstance(properties);
            seenObjects.put(node.getId(), instance);

            if (type.isAssignableFrom(instance.getClass()))
            {
                results.add((T) instance);
            }

        }
        for (Relationship relationship : graph.getRelationships())
        {
            Object start = seenObjects.get(relationship.getStartNodeId());
            Object end = seenObjects.get(relationship.getEndNodeId());

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
                        collection = new HashSet<>();
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

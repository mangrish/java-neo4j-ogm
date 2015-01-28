package io.innerloop.neo4j.ogm.mapping;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.ogm.metadata.MetadataMap;

import java.util.List;

/**
 * Created by markangrish on 28/01/2015.
 */
public class GraphResultMapper
{
    public GraphResultMapper(MetadataMap metadataMap)
    {

    }

    public <T> List<T> map(Class<T> type, Graph graph)
    {
        return null;
    }
}

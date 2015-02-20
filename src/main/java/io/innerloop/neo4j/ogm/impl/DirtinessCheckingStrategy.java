package io.innerloop.neo4j.ogm.impl;

import io.innerloop.neo4j.ogm.impl.metadata.MetadataMap;

import java.util.Map;

/**
 * Created by markangrish on 20/02/2015.
 */
public class DirtinessCheckingStrategy
{
    private final Map<Long, Object> identityMap;

    private final MetadataMap metadataMap;

    public DirtinessCheckingStrategy(Map<Long, Object> identityMap, MetadataMap metadataMap)
    {

        this.identityMap = identityMap;
        this.metadataMap = metadataMap;
    }
}

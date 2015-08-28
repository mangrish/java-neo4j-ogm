package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.ogm.impl.metadata.ClassMetadata;

/**
 * Created by markangrish on 20/05/2015.
 */
public class Query
{
    private final ClassMetadata<?> metadata;

    Query(ClassMetadata<?> metadata)
    {
        this.metadata = metadata;
    }
}

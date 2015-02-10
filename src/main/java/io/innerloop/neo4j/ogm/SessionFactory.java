package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.ogm.mapping.CypherQueryMapper;
import io.innerloop.neo4j.ogm.mapping.GraphResultMapper;
import io.innerloop.neo4j.ogm.metadata.MetadataMap;

/**
 * Created by markangrish on 18/12/2014.
 */
public class SessionFactory
{
    private final Neo4jClient client;

    private final CypherQueryMapper cypherMapper;

    private final GraphResultMapper graphResultMapper;

    public SessionFactory(Neo4jClient client, String... packages)
    {
        MetadataMap metadataMap = new MetadataMap(packages);
        this.client = client;
        this.cypherMapper = new CypherQueryMapper(metadataMap);
        this.graphResultMapper = new GraphResultMapper(metadataMap);
    }

    public Session openSession()
    {
        return Session.getSession(client, cypherMapper, graphResultMapper);
    }
}

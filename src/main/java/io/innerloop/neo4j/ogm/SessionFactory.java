package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.ogm.mapping.ObjectMapper;

/**
 * Created by markangrish on 18/12/2014.
 */
public class SessionFactory
{
    private final Neo4jClient client;

    private ObjectMapper objectMapper;

    public SessionFactory(Neo4jClient client, String... packages)
    {
        this.client = client;
        this.objectMapper = new ObjectMapper(packages);
    }

    public Session openSession()
    {
        return Session.getSession(client, objectMapper);
    }
}

package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Connection;
import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.client.Neo4jClientException;
import io.innerloop.neo4j.ogm.impl.index.Index;
import io.innerloop.neo4j.ogm.impl.metadata.MetadataMap;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

/**
 * Created by markangrish on 18/12/2014.
 */
public class SessionFactory
{
    private final Neo4jClient client;

    private final MetadataMap metadataMap;

    public SessionFactory(Neo4jClient client, String... packages)
    {
        this.metadataMap = new MetadataMap(packages);
        this.client = client;
        buildIndexes();
    }

    private void buildIndexes()
    {
        for (Index index : metadataMap.getIndexes())
        {
            try
            {
                Connection connection = client.getConnection();
                connection.add(index.drop());
                connection.commit();
            }
            catch (Neo4jClientException n4jce)
            {
                // do nothing...
            }
            try
            {
                Connection connection = client.getConnection();
                connection.add(index.create());
                connection.commit();
            }
            catch (Neo4jClientException n4jce)
            {
                // do nothing...
            }
        }
    }

    public Session getCurrentSession()
    {
        return Session.getSession(client, metadataMap);
    }
}

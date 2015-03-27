package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Connection;
import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.client.Statement;

/**
 * Created by markangrish on 26/03/2015.
 */
public class Transaction
{
    private final Connection connection;

    private boolean committed;


    public Transaction(Neo4jClient client)
    {
        this.connection = client.getConnection();
    }

    public void add(Statement statement)
    {
        connection.add(statement);
    }

    public void flush()
    {
        connection.flush();
    }

    public void begin()
    {
        // do nothing for now.
    }

    public void commit()
    {
        connection.commit();
        this.committed = true;
    }


    public void close()
    {
        connection.commit();
        this.committed = true;
    }

    public boolean isOpen()
    {
        return !committed;
    }

    public boolean isClosed()
    {
        return committed;
    }
}

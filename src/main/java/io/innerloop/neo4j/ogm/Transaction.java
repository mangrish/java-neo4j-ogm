package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Connection;
import io.innerloop.neo4j.client.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by markangrish on 26/03/2015.
 */
public class Transaction
{
    private static final Logger LOG = LoggerFactory.getLogger(Transaction.class);

    private final Connection connection;

    private final Session session;

    private boolean committed;


    public Transaction(Session session)
    {
        this.session = session;
        this.connection = session.client.getConnection();
    }

    public void add(Statement statement)
    {
        connection.add(statement);
    }

    public void flush()
    {
        LOG.debug("Flushing Transaction.");
        connection.flush();
    }

    public void begin()
    {
        // do nothing for now.
    }

    public void commit()
    {
        session.flush();
        connection.commit();
        this.committed = true;
    }

    public void rollback()
    {
        connection.rollback();
        this.committed = false;
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
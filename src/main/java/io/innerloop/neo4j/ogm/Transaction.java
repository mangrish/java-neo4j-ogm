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

    private boolean rolledBack;

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
        if (isClosed())
        {
            throw new RuntimeException("Transaction is already completed. Cannot commit.");
        }
        session.flush();
        connection.commit();
        this.committed = true;
        session.completeTransaction();
    }

    public void rollback()
    {
        if (isClosed())
        {
            LOG.warn("Transaction is already completed. Cannot rollback.");
        }
        connection.rollback();
        this.rolledBack = true;
        session.completeTransaction();
    }

    public boolean isOpen()
    {
        return !committed && !rolledBack;
    }

    public boolean isActive()
    {
        return !committed && !rolledBack;
    }

    public boolean isClosed()
    {
        return committed || rolledBack;
    }
}

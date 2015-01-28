package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.GraphStatement;
import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.client.Neo4jClientException;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.client.Transaction;
import io.innerloop.neo4j.ogm.mapping.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by markangrish on 18/12/2014.
 */
public class Session
{
    private static final Logger LOG = LoggerFactory.getLogger(Session.class);

    private static final Pattern WRITE_CYPHER_KEYWORDS = Pattern.compile("\\b(CREATE|MERGE|SET|DELETE|REMOVE)\\b");

    private static ThreadLocal<Session> sessions = new ThreadLocal<>();

    static Session getSession(Neo4jClient client, ObjectMapper objectMapper)
    {
        LOG.debug("Retrieving session for thread: [{}]", Thread.currentThread().getName());
        Session session = sessions.get();

        if (session == null)
        {
            LOG.debug("No session found for thread [{}]. Creating new session.", Thread.currentThread().getName());
            session = new Session(client, objectMapper);
            sessions.set(session);
        }

        return session;
    }

    private final Map<Long, Object> identityMap;

    private final Neo4jClient client;

    private final ObjectMapper objectMapper;

    private Transaction activeTransaction;

    public Session(Neo4jClient client, ObjectMapper objectMapper)
    {
        this.client = client;
        this.objectMapper = objectMapper;
        this.identityMap = new HashMap<>();
    }

    public void close()
    {
        LOG.debug("Closing session on thread: [{}]", Thread.currentThread().getName());
        activeTransaction = null;
        sessions.remove();
    }

    //    public void commit()
    //    {
    //        LOG.debug("Committing transaction on thread [{}].", Thread.currentThread().getName());
    //        Transaction txn = getTransaction();
    //        try
    //        {
    //            txn.commit();
    //        }
    //        catch (Neo4jClientException e)
    //        {
    //            try
    //            {
    //                LOG.warn("Could not commit transaction on thread [{}]. Rolling back...",
    //                         Thread.currentThread().getName());
    //                txn.rollback();
    //            }
    //            catch (Neo4jClientException e1)
    //            {
    //                LOG.error("Could not rollback transaction on thread [{}].", Thread.currentThread().getName());
    //            }
    //        }
    //        finally
    //        {
    //            txn.close();
    //            activeTransaction = null;
    //        }
    //    }

    public Transaction getTransaction()
    {
        if (activeTransaction == null)
        {
            this.activeTransaction = client.getLongTransaction();
        }
        return activeTransaction;
    }

    public void flush()
    {
        Transaction txn = getTransaction();

        try
        {
            txn.flush();
        }
        catch (Neo4jClientException e)
        {
            throw new RuntimeException(e);
        }
    }


    public <T> List<T> query(Class<T> type, String cypher, Map<String, Object> parameters)
    {
        if (Utils.isEmpty(cypher))
        {
            throw new RuntimeException("Supplied cypher statement must not be null or empty.");
        }

        if (parameters == null)
        {
            throw new RuntimeException("Supplied Parameters cannot be null.");
        }

        assertReadOnly(cypher);

        Transaction txn = getTransaction();

        Statement<Graph> statement = new GraphStatement(cypher);

        for (Map.Entry<String, Object> entry : parameters.entrySet())
        {
            statement.setParam(entry.getKey(), entry.getValue());
        }

        txn.add(statement);
        flush();

        Graph graph = statement.getResult();

        return objectMapper.loadAll(type, graph);
    }

    private void assertReadOnly(String cypher)
    {
        Matcher matcher = WRITE_CYPHER_KEYWORDS.matcher(cypher.toUpperCase());

        if (matcher.find())
        {
            throw new RuntimeException("query() only allows read only cypher. To make modifications use execute()");
        }
    }

    public <T> T queryForObject(Class<T> type, String cypher, Map<String, Object> parameters)
    {
        Iterable<T> results = query(type, cypher, parameters);

        int resultSize = Utils.size(results);

        if (resultSize < 1)
        {
            return null;
        }

        if (resultSize < 1)
        {
            throw new RuntimeException("Result not of expected size. Expected 1 row but found " + resultSize);
        }

        return results.iterator().next();
    }

    public void execute(String cypher, Map<String, Object> parameters)
    {
        Transaction txn = getTransaction();

        Statement<Graph> statement = new GraphStatement(cypher);

        for (Map.Entry<String, Object> entry : parameters.entrySet())
        {
            Object value = objectMapper.dump(entry.getValue());
            statement.setParam(entry.getKey(), value);
        }

        txn.add(statement);
    }


    public <T> List<T> loadAll(Class<T> type)
    {
        return loadAll(type, null);
    }

    public <T> List<T> loadAll(Class<T> type, String property, Object value)
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(property, value);
        return loadAll(type, parameters);
    }

    public <T> List<T> loadAll(Class<T> type, Map<String, Object> properties)
    {
        return null;
    }

    public <T> T load(Class<T> type, Map<String, Object> properties)
    {
        return null;
    }

    public <T> T load(Class<T> type, String property, Object value)
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(property, value);
        return load(type, parameters);
    }

    public <T> void save(T entity)
    {
        if (entity.getClass().isArray() || Iterable.class.isAssignableFrom(entity.getClass()))
        {
//            saveAll(entity);
        }
        else
        {
//            save(entity, -1); // default : full tree of changed objects
        }
    }

    public <T> void delete(T entity)
    {
    }
}

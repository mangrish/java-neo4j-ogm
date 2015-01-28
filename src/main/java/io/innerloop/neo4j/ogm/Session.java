package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.client.Neo4jClientException;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.client.Transaction;
import io.innerloop.neo4j.ogm.mapping.CypherMapper;
import io.innerloop.neo4j.ogm.mapping.GraphResultMapper;
import io.innerloop.neo4j.ogm.metadata.MetadataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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

    static Session getSession(Neo4jClient client, CypherMapper cypherMapper, GraphResultMapper graphResultMapper)
    {
        LOG.debug("Retrieving session for thread: [{}]", Thread.currentThread().getName());
        Session session = sessions.get();

        if (session == null)
        {
            LOG.debug("No session found for thread [{}]. Creating new session.", Thread.currentThread().getName());
            session = new Session(client, cypherMapper, graphResultMapper);
            sessions.set(session);
        }

        return session;
    }

    private final Map<Long, Object> identityMap;

    private final Neo4jClient client;

    private final CypherMapper cypherMapper;

    private final GraphResultMapper graphResultMapper;

    private Transaction activeTransaction;

    public Session(Neo4jClient client, CypherMapper cypherMapper, GraphResultMapper graphResultMapper)
    {
        this.client = client;
        this.cypherMapper = cypherMapper;
        this.graphResultMapper = graphResultMapper;
        this.identityMap = new HashMap<>();
    }


    public void close()
    {
        LOG.debug("Closing session on thread: [{}]", Thread.currentThread().getName());
        sessions.remove();
        activeTransaction = null;
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
        catch (Neo4jClientException nce)
        {
            throw new RuntimeException("Encountered error when trying to flush database.", nce);
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

        Statement<Graph> statement = cypherMapper.query(cypher, parameters);
        Transaction txn = getTransaction();

        txn.add(statement);
        flush();
        Graph graph = statement.getResult();
        return graphResultMapper.map(type, graph);
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
        Statement<Graph> statement = cypherMapper.execute(cypher, parameters);
        Transaction txn = getTransaction();
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
        Statement<Graph> statement = cypherMapper.match(type, properties);
        Transaction txn = getTransaction();
        txn.add(statement);
        flush();
        Graph graph = statement.getResult();

        return graphResultMapper.map(type, graph);

    }

    public <T> T load(Class<T> type, Map<String, Object> properties)
    {
        Iterable<T> results = loadAll(type, properties);

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

    public <T> T load(Class<T> type, String property, Object value)
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(property, value);
        return load(type, parameters);
    }

    public <T> void save(T entity)
    {
        if (entity.getClass().isArray())
        {
            saveAll(Arrays.asList(entity));
        }
        else if (Iterable.class.isAssignableFrom(entity.getClass()))
        {
            saveAll((Iterable<T>) entity);
        }
        else
        {
            List<Statement> statements = cypherMapper.merge(entity);
            Transaction txn = getTransaction();
            statements.forEach(txn::add);
        }
    }

    private <T> void saveAll(Iterable<T> elements)
    {
        for (T element : elements)
        {
            save(element);
        }
    }

    public <T> void delete(T entity)
    {
        if (entity.getClass().isArray())
        {
            deleteAll(Arrays.asList(entity));
        }
        else if (Iterable.class.isAssignableFrom(entity.getClass()))
        {
            deleteAll((Iterable<T>) entity);
        }
        else
        {
            List<Statement> statements = cypherMapper.delete(entity);
            Transaction txn = getTransaction();
            statements.forEach(txn::add);
        }
    }

    private <T> void deleteAll(Iterable<T> elements)
    {
        for (T element : elements)
        {
            delete(element);
        }
    }
}

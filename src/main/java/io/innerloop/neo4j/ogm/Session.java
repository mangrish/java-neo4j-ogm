package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.client.Transaction;
import io.innerloop.neo4j.ogm.impl.mapping.CypherQueryMapper;
import io.innerloop.neo4j.ogm.impl.mapping.GraphResultMapper;
import io.innerloop.neo4j.ogm.impl.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.MetadataMap;
import io.innerloop.neo4j.ogm.impl.util.CollectionUtils;
import io.innerloop.neo4j.ogm.impl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

    static Session getSession(Neo4jClient client, MetadataMap metadataMap)
    {
        LOG.trace("Retrieving session for thread: [{}]", Thread.currentThread().getName());
        Session session = sessions.get();

        if (session == null)
        {
            LOG.debug("No session found for thread [{}]. Creating new session for this thread.", Thread.currentThread().getName());
            session = new Session(client, metadataMap);
            sessions.set(session);
        }

        return session;
    }

    private final Map<Long, Object> identityMap;

    private final List<Object> deletableObjects;

    private final List<Object> dirtyObjects;

    private final List<Object> newObjects;

    private final Neo4jClient client;

    private final MetadataMap metadataMap;

    private final CypherQueryMapper cypherMapper;

    private final GraphResultMapper graphResultMapper;


    public Session(Neo4jClient client, MetadataMap metadataMap)
    {
        this.client = client;
        this.metadataMap = metadataMap;
        this.identityMap = new HashMap<>();
        this.cypherMapper = new CypherQueryMapper(metadataMap);
        this.graphResultMapper = new GraphResultMapper(identityMap, metadataMap);
        this.deletableObjects = new ArrayList<>();
        this.dirtyObjects = new ArrayList<>();
        this.newObjects = new ArrayList<>();
    }


    public void close()
    {
        LOG.debug("Closing session on thread: [{}]", Thread.currentThread().getName());
        sessions.remove();
    }

    public Transaction getTransaction()
    {
        return client.getLongTransaction();
    }

    public void flush()
    {
        Transaction txn = getTransaction();
        txn.flush();
        newObjects.clear();
        dirtyObjects.clear();
        deletableObjects.clear();
    }

    private List<Statement> prepareStatements()
    {
        List<Statement> statements = new ArrayList<>();

        for (Object newObject : newObjects)
        {
            statements.addAll(cypherMapper.merge(newObject));
        }
        for (Object dirtyObject : dirtyObjects)
        {
            statements.addAll(cypherMapper.merge(dirtyObject));
        }

        for (Object deletableObject : deletableObjects)
        {
            statements.addAll(cypherMapper.delete(deletableObject));
        }

        return statements;
    }

    public <T> List<T> query(Class<T> type, String cypher, Map<String, Object> parameters)
    {
        if (StringUtils.isEmpty(cypher))
        {
            throw new RuntimeException("Supplied cypher statement must not be null or empty.");
        }

        if (parameters == null)
        {
            throw new RuntimeException("Supplied Parameters cannot be null.");
        }

        assertReadOnly(cypher);
        Statement<Graph> statement = cypherMapper.execute(cypher, parameters);
        Transaction txn = getTransaction();
        prepareStatements().forEach(txn::add);
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

        int resultSize = CollectionUtils.size(results);

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
        prepareStatements().forEach(txn::add);
        txn.add(statement);
        flush();
        Graph graph = statement.getResult();

        return graphResultMapper.map(type, graph);

    }

    public <T> T load(Class<T> type, Map<String, Object> properties)
    {
        Iterable<T> results = loadAll(type, properties);

        int resultSize = CollectionUtils.size(results);

        if (resultSize < 1)
        {
            return null;
        }

        if (resultSize > 1)
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
            ClassMetadata<T> metadata = metadataMap.get(entity);
            Object id = metadata.getNeo4jIdField().getValue(entity);

            if (id == null)
            {
                newObjects.add(entity);
            }
            else
            {
                dirtyObjects.add(entity);
            }
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
            ClassMetadata<T> metadata = metadataMap.get(entity);
            Object id = metadata.getNeo4jIdField().getValue(entity);

            if (id == null)
            {
                newObjects.remove(entity);
            }
            else
            {
                deletableObjects.add(entity);
            }
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

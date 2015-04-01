package io.innerloop.neo4j.ogm;

import com.google.common.primitives.Primitives;
import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.GraphStatement;
import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.client.RowSet;
import io.innerloop.neo4j.client.RowStatement;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.ogm.impl.mapping.CypherQueryMapper;
import io.innerloop.neo4j.ogm.impl.mapping.GraphResultMapper;
import io.innerloop.neo4j.ogm.impl.mapping.IdentityMap;
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
            LOG.debug("No session found for thread [{}]. Creating new session for this thread.",
                      Thread.currentThread().getName());
            session = new Session(client, metadataMap);
            sessions.set(session);
        }

        return session;
    }

    private final IdentityMap identityMap;

    private final List<Object> newObjects;

    private final List<Object> deletedObjects;

    final Neo4jClient client;

    private final MetadataMap metadataMap;

    private final CypherQueryMapper cypherMapper;

    private final GraphResultMapper graphResultMapper;

    private Transaction activeTransaction;

    public Session(Neo4jClient client, MetadataMap metadataMap)
    {
        this.client = client;
        this.metadataMap = metadataMap;
        this.identityMap = new IdentityMap(metadataMap);
        this.cypherMapper = new CypherQueryMapper(identityMap, metadataMap);
        this.graphResultMapper = new GraphResultMapper(identityMap, metadataMap);
        this.newObjects = new ArrayList<>();
        this.deletedObjects = new ArrayList<>();
    }


    public void close()
    {
        LOG.debug("Closing session on thread: [{}]", Thread.currentThread().getName());
        sessions.remove();
    }


    public void flush()
    {
        flush(null);
    }

    private void flush(Statement statement)
    {
        Transaction txn = getTransaction();
        List<Statement> statements = new ArrayList<>();

        for (Object newObject : newObjects)
        {
            List<Statement> newStatements = cypherMapper.merge(newObject);
            statements.addAll(newStatements);
        }

        for (Object dirtyObject : identityMap.getDirtyObjects())
        {
            statements.addAll(cypherMapper.merge(dirtyObject));
        }

        for (Object deletedObject : deletedObjects)
        {
            statements.addAll(cypherMapper.delete(deletedObject));
        }

        if (statement != null)
        {
            statements.add(statement);
        }

        statements.forEach(txn::add);
        txn.flush();
        identityMap.refresh();
        newObjects.clear();
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


        if (Primitives.isWrapperType(type))
        {
            RowStatement statement = cypherMapper.executeRowSet(cypher, parameters);
            flush(statement);
            RowSet rs = statement.getResult();
            ArrayList<T> result = new ArrayList<>();
            while (rs.hasNext())
            {
                result.add((T) rs.next()[0]);
            }
            return result;
        }
        else
        {
            GraphStatement statement = cypherMapper.executeGraph(cypher, parameters);
            flush(statement);
            Graph graph = statement.getResult();
            return graphResultMapper.map(type, graph);
        }

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
            if (byte.class.isAssignableFrom(type))
            {
                return (T) Byte.valueOf((byte) 0);
            }
            else if (short.class.isAssignableFrom(type))
            {
                return (T) Short.valueOf((short) 0);
            }
            else if (int.class.isAssignableFrom(type))
            {
                return (T) Integer.valueOf(0);
            }
            else if (long.class.isAssignableFrom(type))
            {
                return (T) Long.valueOf(0L);
            }
            else if (float.class.isAssignableFrom(type))
            {
                return (T) Float.valueOf(0F);
            }
            else if (double.class.isAssignableFrom(type))
            {
                return (T) Double.valueOf(0D);
            }
            else if (char.class.isAssignableFrom(type))
            {
                return (T) Character.valueOf('\u0000');
            }
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
        GraphStatement statement = cypherMapper.match(type, properties);
        flush(statement);
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
            Object neo4jId = metadata.getNeo4jIdField().getValue(entity);
            if (neo4jId == null)
            {
                newObjects.add(entity);
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
                deletedObjects.add(entity);
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

    public Transaction getTransaction()
    {
        if (activeTransaction == null || activeTransaction.isClosed())
        {
            activeTransaction = new Transaction(this);
        }
        return activeTransaction;
    }
}

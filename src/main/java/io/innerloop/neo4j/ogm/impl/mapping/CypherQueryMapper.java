package io.innerloop.neo4j.ogm.impl.mapping;

import io.innerloop.neo4j.client.GraphStatement;
import io.innerloop.neo4j.client.RowStatement;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.ogm.annotations.Aggregate;
import io.innerloop.neo4j.ogm.annotations.Relationship;
import io.innerloop.neo4j.ogm.impl.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.MetadataMap;
import io.innerloop.neo4j.ogm.impl.metadata.RelationshipMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Created by markangrish on 28/01/2015.
 */
public class CypherQueryMapper
{
    private static final Logger LOG = LoggerFactory.getLogger(CypherQueryMapper.class);

    private final IdentityMap identityMap;

    private final MetadataMap metadataMap;

    public CypherQueryMapper(IdentityMap identityMap, MetadataMap metadataMap)
    {
        this.identityMap = identityMap;
        this.metadataMap = metadataMap;
    }

    /**
     * This method will replace match(Class, Map).
     * <p>
     * The intent of this method is to recursively traverse classes looking for aggregate annotations and including them
     * in the match statement. If a class is marked with aggregate and includes an Include annotation on a field this
     * method will continue to traverse until it hits a leaf or does not see another aggregate annotation.
     * <p>
     * TODO: As this is an expensive operation I will probably introduce a cache as these don't change after they are
     * fired once.
     */
    public <T> GraphStatement match(Class<T> type, Map<String, Object> parameters)
    {
        ClassMetadata<T> first = metadataMap.get(type);
        AlphaGenerator generator = new AlphaGenerator();

        String currentAlpha = generator.nextAlpha();
        // Get the first match
        String query = "MATCH (" + currentAlpha + first.getNodeLabel().asCypher() + ")";
        int i = 1;

        Stack<Class<?>> toVisit = new Stack<>();

        if (type != null)
        {
            toVisit.push(type);
        }

        while (!toVisit.isEmpty())
        {
            Class<?> ref = toVisit.pop();
            ClassMetadata<?> classMetadata = metadataMap.get(ref);

            if (classMetadata == null)
            {
                LOG.debug("No metadata class found for: [{}]", ref);
                continue;
            }
            for (RelationshipMetadata rm : classMetadata.getRelationships())
            {
                Class<?> cls = rm.getType();
                if (rm.isCollection())
                {
                    cls = rm.getParamterizedType();
                }

                if (!cls.isAnnotationPresent(Aggregate.class) && !rm.isFetchEnabled())
                {
                    continue;
                }

                if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers()))
                {
                    LOG.debug("Encountered an interface/abstract class [{}]. Expecting no metadata to be found", cls);
                }

                query += " OPTIONAL MATCH (" + currentAlpha + ")-[r" + i + ":" + rm.getName() + "]-() ";
                query += "WITH " + currentAlpha + ", COLLECT(r" + i + ") as r" + i;
                for (int j = 1; j < i; j++)
                {
                    query += ", r" + j;
                }
                i++;

                toVisit.push(cls);
            }
            currentAlpha = generator.nextAlpha();
        }

        // this part is ok...
        if (parameters != null)
        {
            query += " WHERE ";
            int numParams = parameters.size();
            for (String key : parameters.keySet())
            {
                query += "a." + key + " = {" + key + "}";

                if (--numParams > 0)
                {
                    query += ", ";
                }
            }
        }

        // TODO: fix this part
        query += " RETURN " + generator.printCurrent();

        for (int j = 1; j < i; j++)
        {
            query += ", r" + j;
        }

        // this part is ok...
        GraphStatement statement = new GraphStatement(query);

        if (parameters != null)
        {
            for (Map.Entry<String, Object> entry : parameters.entrySet())
            {
                statement.setParam(entry.getKey(), entry.getValue());
            }
        }

        return statement;
    }

    public <T> List<Statement> merge(T entity)
    {
        List<Statement> nodeStatements = new ArrayList<>();
        List<Statement> relationshipStatements = new ArrayList<>();

        Stack<Object> toVisit = new Stack<>();
        IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

        if (entity != null)
        {
            toVisit.push(entity);
        }

        while (!toVisit.isEmpty())
        {
            Object ref = toVisit.pop();

            if (visited.containsKey(ref))
            {
                continue;
            }

            // add a merge statement for this object.
            ClassMetadata<?> classMetadata = metadataMap.get(ref);

            RowStatement nodeStatement = new RowStatement("MERGE (e" + classMetadata.getNodeLabel().asCypher() +
                                                          "{" + classMetadata.getPrimaryIdField().getName() +
                                                          ":{0}}) SET e = {1} RETURN id(e)");
            nodeStatement.setParam("0", classMetadata.getPrimaryIdField().getValue(ref));
            nodeStatement.setParam("1", classMetadata.toJsonObject(ref));
            nodeStatements.add(nodeStatement);

            Object neo4jIdVal = classMetadata.getNeo4jIdField().getValue(ref);

            if (neo4jIdVal == null)
            {
                identityMap.addNew(ref, nodeStatement);
            }

            visited.put(ref, ref);

            for (RelationshipMetadata rm : classMetadata.getRelationships())
            {
                Object edge = rm.getValue(ref);

                if (edge != null)
                {
                    // add a relationship statement for the ref and edge objects.
                    ClassMetadata<?> edgeClassMetadata;
                    if (edge instanceof Iterable)
                    {
                        edgeClassMetadata = metadataMap.get((Class) rm.getParamterizedType());

                        for (Object o : (Iterable) edge)
                        {
                            if (edgeClassMetadata == null)
                            {
                                edgeClassMetadata = metadataMap.get((Class) o.getClass());
                            }

                            addRelationshipStatement(relationshipStatements,
                                                     toVisit,
                                                     visited,
                                                     ref,
                                                     classMetadata,
                                                     rm,
                                                     o,
                                                     edgeClassMetadata);
                        }
                    }
                    else
                    {
                        edgeClassMetadata = metadataMap.get(edge);
                        addRelationshipStatement(relationshipStatements,
                                                 toVisit,
                                                 visited,
                                                 ref,
                                                 classMetadata,
                                                 rm,
                                                 edge,
                                                 edgeClassMetadata);
                    }
                }
            }
        }
        List<Statement> results = new ArrayList<>();
        results.addAll(nodeStatements);
        results.addAll(relationshipStatements);
        return results;
    }

    private void addRelationshipStatement(List<Statement> relationshipStatements,
                                          Stack<Object> toVisit,
                                          IdentityHashMap<Object, Object> visited,
                                          Object ref,
                                          ClassMetadata<?> classMetadata,
                                          RelationshipMetadata rm,
                                          Object edge,
                                          ClassMetadata<?> edgeClassMetadata)
    {
        RowStatement relationshipStatement = new RowStatement("MATCH (a" +
                                                              classMetadata.getNodeLabel().asCypher() +
                                                              "{" +
                                                              classMetadata.getPrimaryIdField().getName() +
                                                              ":{0}}), (b" +
                                                              edgeClassMetadata.getNodeLabel().asCypher() +
                                                              "{" +
                                                              edgeClassMetadata.getPrimaryIdField().getName() +
                                                              ":{1}}) MERGE (a)" +
                                                              (rm.getDirection()
                                                                       .equals(Relationship.Direction.INCOMING) ?
                                                                       "<" :
                                                                       "") +
                                                              "-" +
                                                              "[r:" +
                                                              rm.getName() +
                                                              "]" +
                                                              "-" +
                                                              (rm.getDirection()
                                                                       .equals(Relationship.Direction.OUTGOING) ?
                                                                       ">" :
                                                                       "") +
                                                              "(b)");
        relationshipStatement.setParam("0", classMetadata.getPrimaryIdField().getValue(ref));
        relationshipStatement.setParam("1", edgeClassMetadata.getPrimaryIdField().getValue(edge));
        relationshipStatements.add(relationshipStatement);

        if (!visited.containsKey(edge))
        {
            toVisit.push(edge);
        }
    }


    public GraphStatement executeGraph(String cypher, Map<String, Object> parameters)
    {
        GraphStatement statement = new GraphStatement(cypher);

        if (parameters != null)
        {
            for (Map.Entry<String, Object> entry : parameters.entrySet())
            {
                statement.setParam(entry.getKey(), entry.getValue());
            }
        }

        return statement;
    }

    public RowStatement executeRowSet(String cypher, Map<String, Object> parameters)
    {
        RowStatement statement = new RowStatement(cypher);

        if (parameters != null)
        {
            for (Map.Entry<String, Object> entry : parameters.entrySet())
            {
                statement.setParam(entry.getKey(), entry.getValue());
            }
        }

        return statement;
    }

    public <T> List<Statement> delete(T entity)
    {
        List<Statement> results = new ArrayList<>();

        // add a merge statement for this object.
        ClassMetadata<?> classMetadata = metadataMap.get(entity);

        GraphStatement statement = new GraphStatement("MATCH (e" + classMetadata.getNodeLabel().asCypher() +
                                                      "{" + classMetadata.getPrimaryIdField().getName() +
                                                      ":{0}})-[r]-() DELETE e, r");
        statement.setParam("0", classMetadata.getPrimaryIdField().getValue(entity));
        results.add(statement);

        return results;
    }

    private static class AlphaGenerator
    {

        public static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

        private static final String[] CHARS = ALPHABET.split("(?!^)");

        private int currentPointer = 0;

        public String nextAlpha()
        {
            return CHARS[currentPointer++];
        }

        public String printCurrent()
        {
            String result = CHARS[0];
            for (int i = 1; i < currentPointer - 2; i++)
            {
                result += ", " + CHARS[currentPointer - 2];
            }

            return result;
        }
    }
}

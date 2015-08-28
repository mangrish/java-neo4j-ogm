package io.innerloop.neo4j.ogm.impl.mapping;

import com.google.common.collect.AbstractIterator;
import io.innerloop.neo4j.client.GraphStatement;
import io.innerloop.neo4j.client.RowStatement;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.ogm.annotations.Relationship;
import io.innerloop.neo4j.ogm.impl.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.MetadataMap;
import io.innerloop.neo4j.ogm.impl.metadata.RelationshipMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.RelationshipPropertiesClassMetadata;
import io.innerloop.neo4j.ogm.impl.util.StopWatch;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by markangrish on 28/01/2015.
 */
public class CypherQueryMapper
{
    private static final Logger LOG = LoggerFactory.getLogger(CypherQueryMapper.class);

    private static ConcurrentHashMap<MatchStatementKey, GraphStatement> matchStatements = new ConcurrentHashMap<>();

    private static String alphaUsed(Map<Pair<Class<?>, Integer>, String> usage)
    {
        final String comma = ", ";
        String result = "";

        boolean first = true;
        for (String entry : new HashSet<>(usage.values()))
        {
            if (!first)
            {
                result += comma;
            }
            first = false;
            result += entry;
        }

        return result;
    }

    private final IdentityMap identityMap;

    private final MetadataMap metadataMap;

    public CypherQueryMapper(IdentityMap identityMap, MetadataMap metadataMap)
    {
        this.identityMap = identityMap;
        this.metadataMap = metadataMap;
    }

    /**
     * The intent of this method is to iteratively traverse classes looking for aggregate annotations and including them
     * in the match statement. If a class is marked with aggregate and includes an Fetch annotation on a field this
     * method will continue to traverse until it hits a leaf or does not see another aggregate annotation.
     * <p>
     * TODO: As this is an expensive operation I will probably introduce a cache as these don't change after they are
     * fired once.
     */
    public <T> GraphStatement match(Class<T> type, Map<String, Object> parameters)
    {
        if (type == null)
        {
            throw new RuntimeException("Type to match must not be null");
        }
        LOG.trace("Building match statement for type: [{}] with  params: [{}]", type.getSimpleName(), parameters);

        MatchStatementKey msKey = new MatchStatementKey(type, parameters!= null ? parameters.keySet(): null);

        GraphStatement statement = matchStatements.get(msKey);

        if (statement == null)
        {
            StopWatch sw = new StopWatch("MATCH Statement Builder", LOG);
            sw.start();

            Queue<Class<?>> toVisit = new LinkedList<>();
            Map<Pair<Class<?>, Integer>, String> usage = new HashMap<>();
            Sequence sequence = new Sequence();
            String query;

            ClassMetadata<T> first = metadataMap.get(type);

            if (first == null)
            {
                if (type.isInterface() || Modifier.isAbstract(type.getModifiers()))
                {
                    LOG.debug("Type to match is an interface/abstract class [{}]. Pushing subtypes on to stack.", type);
                    query = "MATCH (a:" + type.getSimpleName() + ")";
                }
                else
                {
                    throw new RuntimeException("Could not find a type to match on for: [" + type.getName() + "]");
                }
            }
            else
            {
                query = "MATCH (a" + first.getNodeLabel().asCypher() + ")";
            }

            toVisit.offer(type);

            int relationshipCount = 1;
            int currentDepth = 0;
            int elementsToDepthIncrease = 1;
            int nextElementsToDepthIncrease = 0;

            final Pair<Class<?>, Integer> parentKey = new Pair<>(type, currentDepth);
            usage.put(parentKey, sequence.computeNext());


                //            while (!toVisit.isEmpty())
                //            {
                //                Class<?> cls = toVisit.poll();
                //                Pair<Class<?>, Integer> key = new Pair<>(cls, currentDepth);
                //
                //                if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers()))
                //                {
                //                    Set<? extends Class<?>> subTypesOf = metadataMap.findSubTypesOf(cls);
                //                    subTypesOf.forEach(k -> {
                //                        if (!k.isInterface() && !Modifier.isAbstract(k.getModifiers()))
                //                        {
                //                            toVisit.offer(k);
                //                        }
                //                    });
                //                    continue;
                //                }
                //
                //                ClassMetadata<?> classMetadata = metadataMap.get(cls);
                //
                //                for (RelationshipMetadata rm : classMetadata.getRelationships())
                //                {
                //                    Class<?> relationshipCls = rm.getType();
                //                    if (rm.isCollection())
                //                    {
                //                        relationshipCls = rm.getParamterizedTypes()[0];
                //                    }
                //                    if (rm.isMap())
                //                    {
                //                        relationshipCls = rm.getParamterizedTypes()[0];
                //                    }
                //
                //                    LOG.debug("depth is [{}]", currentDepth);
                //                    String lhs = usage.get(key);
                //                    LOG.debug("LHS KEY: [{}]", key);
                //                    Pair<Class<?>, Integer> rhsKey = new Pair<>(relationshipCls, currentDepth + 1);
                //                    LOG.debug("RHS KEY: [{}]", rhsKey);
                //
                //                    String rhs = usage.get(rhsKey);
                //                    if (rhs == null)
                //                    {
                //                        rhs = sequence.computeNext();
                //                        if (relationshipCls.isInterface() || Modifier.isAbstract(relationshipCls.getModifiers()))
                //                        {
                //                            Set<? extends Class<?>> subTypesOf = metadataMap.findSubTypesOf(relationshipCls);
                //                            final int finalCurrentDepth = currentDepth + 1;
                //                            final String finalRhs = rhs;
                //                            subTypesOf.forEach(k -> {
                //                                if (!k.isInterface() && !Modifier.isAbstract(k.getModifiers()))
                //                                {
                //                                    usage.put(new Pair<>(k, finalCurrentDepth), finalRhs);
                //                                }
                //                            });
                //                            nextElementsToDepthIncrease += subTypesOf.size();
                //                        }
                //                        else
                //                        {
                //                            usage.put(new Pair<>(relationshipCls, currentDepth + 1), rhs);
                //                            nextElementsToDepthIncrease++;
                //                        }
                //
                //                    }
                //
                //                    query += " OPTIONAL MATCH (" + lhs + ")-[r" + relationshipCount + ":" + rm.getName() + "]-(" + rhs +
                //                             ") ";
                //                    query += "WITH " + alphaUsed(usage) + ", COLLECT(DISTINCT r" + relationshipCount + ") as r" +
                //                             relationshipCount;
                //                    for (int i = 1; i < relationshipCount; i++)
                //                    {
                //                        query += ", r" + i;
                //                    }
                //                    relationshipCount++;
                //
                //                    Pair<Class<?>, Integer> prevKey = new Pair<>(relationshipCls, currentDepth - 1);
                //                    String prev = usage.get(prevKey);
                //                    if (prev != null && relationshipCls.equals(cls))
                //                    {
                //                        LOG.debug("CYCLE DETECTED. Preventing loading of next cycle.");
                //                        continue;
                //                    }
                //
                //                    toVisit.offer(relationshipCls);
                //                }
                //
                //                if (--elementsToDepthIncrease == 0)
                //                {
                //                    elementsToDepthIncrease = nextElementsToDepthIncrease;
                //                    nextElementsToDepthIncrease = 0;
                //                    currentDepth++;
                //                }
                //            }
            sw.split("Completed statement.");

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

            query += " RETURN " + alphaUsed(usage);

            for (int j = 1; j < relationshipCount; j++)
            {
                query += ", r" + j;
            }

            statement = new GraphStatement(query);
            sw.stop();
            // No need for put if absent type/checking semantics.. doesn't matter if this gets overwritten once or so.
            matchStatements.put(msKey, statement);
        }

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
        if (entity == null)
        {
            throw new RuntimeException("Type to match must not be null");
        }

        LOG.trace("Building merge statement for entity: [{}]", entity.getClass().getSimpleName());
        StopWatch sw = new StopWatch("MERGE Statement Builder", LOG);
        sw.start();

        LinkedHashSet<Statement> nodeStatements = new LinkedHashSet<>();
        LinkedHashSet<Statement> relationshipStatements = new LinkedHashSet<>();

        Stack<Object> toVisit = new Stack<>();
        IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

        toVisit.push(entity);

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
                        edgeClassMetadata = metadataMap.get(rm.getParamterizedTypes()[0]);

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
                    else if (edge instanceof Map)
                    {
                        edgeClassMetadata = metadataMap.get(rm.getParamterizedTypes()[0]);
                        RelationshipPropertiesClassMetadata relationshipPropertiesClassMetadata = metadataMap.getRelationshipPropertiesClassMetadata(rm.getParamterizedTypes()[1]);

                        Map map = (Map) edge;
                        for (Object o : map.keySet())
                        {
                            if (edgeClassMetadata == null)
                            {
                                edgeClassMetadata = metadataMap.get((Class) o.getClass());
                            }
                            Object v = map.get(o);
                            addMappedRelationshipStatement(relationshipStatements,
                                                           toVisit,
                                                           visited,
                                                           ref,
                                                           classMetadata,
                                                           rm,
                                                           o,
                                                           edgeClassMetadata,
                                                           relationshipPropertiesClassMetadata,
                                                           v);
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
        LinkedHashSet<Statement> results = new LinkedHashSet<>();
        results.addAll(nodeStatements);
        results.addAll(relationshipStatements);

        sw.stop();

        return new ArrayList<>(results);
    }

    private void addRelationshipStatement(LinkedHashSet<Statement> relationshipStatements,
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

    private void addMappedRelationshipStatement(LinkedHashSet<Statement> relationshipStatements,
                                                Stack<Object> toVisit,
                                                IdentityHashMap<Object, Object> visited,
                                                Object ref,
                                                ClassMetadata<?> classMetadata,
                                                RelationshipMetadata rm,
                                                Object edge,
                                                ClassMetadata<?> edgeClassMetadata,
                                                RelationshipPropertiesClassMetadata relationshipPropertiesClassMetadata,
                                                Object v)
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
                                                              "(b) SET r = {2}");
        relationshipStatement.setParam("0", classMetadata.getPrimaryIdField().getValue(ref));
        relationshipStatement.setParam("1", edgeClassMetadata.getPrimaryIdField().getValue(edge));
        relationshipStatement.setParam("2", relationshipPropertiesClassMetadata.toJsonObject(v));
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

    private static class Sequence extends AbstractIterator<String>
    {
        private int now;

        private static char[] vs;

        static
        {
            vs = new char['z' - 'a' + 1];
            for (char i = 'a'; i <= 'z'; i++)
                vs[i - 'a'] = i;
        }

        private StringBuilder alpha(int i)
        {
            assert i > 0;
            char r = vs[--i % vs.length];
            int n = i / vs.length;
            return n == 0 ? new StringBuilder().append(r) : alpha(n).append(r);
        }

        @Override
        protected String computeNext()
        {
            return alpha(++now).toString();
        }
    }

    private static class MatchStatementKey
    {
        final Class<?> type;

        final Set<String> parameters;

        public MatchStatementKey(Class<?> type, Set<String> parameters)
        {
            this.type = type;
            this.parameters = parameters;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            MatchStatementKey that = (MatchStatementKey) o;

            return !(type != null ? !type.equals(that.type) : that.type != null) &&
                   !(parameters != null ? !parameters.equals(that.parameters) : that.parameters != null);

        }

        @Override
        public int hashCode()
        {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
            return result;
        }
    }
}

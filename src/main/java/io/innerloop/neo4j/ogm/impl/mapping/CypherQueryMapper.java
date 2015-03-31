package io.innerloop.neo4j.ogm.impl.mapping;

import io.innerloop.neo4j.client.GraphStatement;
import io.innerloop.neo4j.client.RowStatement;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.ogm.impl.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.MetadataMap;
import io.innerloop.neo4j.ogm.impl.metadata.RelationshipMetadata;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
    private final IdentityMap identityMap;

    private final MetadataMap metadataMap;

    public CypherQueryMapper(IdentityMap identityMap, MetadataMap metadataMap)
    {
        this.identityMap = identityMap;
        this.metadataMap = metadataMap;
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

            RowStatement nodeStatement = new RowStatement("MERGE (e" + classMetadata.getLabelKey().asCypher() +
                                                          "{" + classMetadata.getPrimaryField().getName() +
                                                          ":{0}}) SET e = {1} RETURN id(e)");
            nodeStatement.setParam("0", classMetadata.getPrimaryField().getValue(ref));
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
                        Type type = rm.getField().getGenericType();
                        if (!(type instanceof ParameterizedType))
                        {
                            throw new RuntimeException("Collection of type: [" + type.getTypeName() + "] on field: [" +
                                                       rm.getField().getName() + "] of class: [" +
                                                       rm.getField().getDeclaringClass() + "] must be parameterised.");
                        }
                        ParameterizedType returnType = (ParameterizedType) type;
                        Type componentType = returnType.getActualTypeArguments()[0];

                        edgeClassMetadata = metadataMap.get((Class) componentType);

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
                                                              classMetadata.getLabelKey().asCypher() +
                                                              "{" +
                                                              classMetadata.getPrimaryField().getName() +
                                                              ":{0}}), (b" +
                                                              edgeClassMetadata.getLabelKey().asCypher() +
                                                              "{" +
                                                              edgeClassMetadata.getPrimaryField().getName() +
                                                              ":{1}}) MERGE (a)-[r:" +
                                                              rm.getType() +
                                                              "]->(b)");
        relationshipStatement.setParam("0", classMetadata.getPrimaryField().getValue(ref));
        relationshipStatement.setParam("1", edgeClassMetadata.getPrimaryField().getValue(edge));
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

    public <T> GraphStatement match(Class<T> type, Map<String, Object> parameters)
    {
        ClassMetadata<T> classMetadata = metadataMap.get(type);

        // Add the parent thing to save
        String query = "MATCH (a" + classMetadata.getLabelKey().asCypher() + ")";

        int i = 1;
        for (RelationshipMetadata rm : classMetadata.getRelationships())
        {
            query += " OPTIONAL MATCH (a)-[r" + i + ":" + rm.getType() + "]-() ";
            query += "WITH a, COLLECT(r" + i + ") as r" + i;
            for (int j = 1; j < i; j++)
            {
                query += ", r" + j;
            }
            i++;
        }


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

        query += " RETURN a";

        for (int j = 1; j < i; j++)
        {
            query += ", r" + j;
        }

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

    public <T> List<Statement> delete(T entity)
    {
        List<Statement> results = new ArrayList<>();

        // add a merge statement for this object.
        ClassMetadata<?> classMetadata = metadataMap.get(entity);

        GraphStatement statement = new GraphStatement("MATCH (e" + classMetadata.getLabelKey().asCypher() +
                                                      "{" + classMetadata.getPrimaryField().getName() +
                                                      ":{0}})-[r]-() DELETE e, r");
        statement.setParam("0", classMetadata.getPrimaryField().getValue(entity));
        results.add(statement);

        return results;
    }
}

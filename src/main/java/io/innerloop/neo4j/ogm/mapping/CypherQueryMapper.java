package io.innerloop.neo4j.ogm.mapping;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.GraphStatement;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.ogm.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.metadata.MetadataMap;
import io.innerloop.neo4j.ogm.metadata.RelationshipMetadata;

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
    private final MetadataMap metadataMap;

    public CypherQueryMapper(MetadataMap metadataMap)
    {
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

            Statement<Graph> nodeStatement = new GraphStatement("MERGE (e" + classMetadata.getLabelKey().asCypher() +
                                                                "{" + classMetadata.getPrimaryField().getName() +
                                                                ":{0}}) SET e = {1}");
            nodeStatement.setParam("0", classMetadata.getPrimaryField().getValue(ref));
            nodeStatement.setParam("1", classMetadata.toJsonObject(ref));
            nodeStatements.add(nodeStatement);

            for (RelationshipMetadata rm : classMetadata.getRelationships())
            {
                Object edge = rm.getValue(ref);

                if (edge != null)
                {
                    // add a relationship statement for the ref and edge objects.
                    ClassMetadata<?> edgeClassMetadata = metadataMap.get(edge);

                    Statement<Graph> relationshipStatement = new GraphStatement("MATCH (a" +
                                                                                classMetadata.getLabelKey().asCypher() +
                                                                                "{" + classMetadata.getPrimaryField()
                                                                                              .getName() +
                                                                                ":{0}}), (b" +
                                                                                edgeClassMetadata.getLabelKey()
                                                                                        .asCypher() +
                                                                                "{" +
                                                                                edgeClassMetadata.getPrimaryField()
                                                                                        .getName() +
                                                                                ":{1}}) MERGE (a)-[r:" + rm.getType() +
                                                                                "]->(b)");
                    relationshipStatement.setParam("0", classMetadata.getPrimaryField().getValue(ref));
                    relationshipStatement.setParam("1", edgeClassMetadata.getPrimaryField().getValue(ref));
                    relationshipStatements.add(relationshipStatement);

                    if (!visited.containsKey(edge))
                    {
                        toVisit.push(edge);
                    }
                }
            }
        }
        List<Statement> results = new ArrayList<>();
        results.addAll(nodeStatements);
        results.addAll(relationshipStatements);
        return results;
    }


    public void visit(Object root)
    {


    }

    private static void nullSafeAdd(final Stack<Object> toVisit, final Object o)
    {

    }

    public Statement<Graph> query(String cypher, Map<String, Object> parameters)
    {
        return null;
    }

    public Statement<Graph> execute(String cypher, Map<String, Object> parameters)
    {
        return null;
    }

    public <T> Statement<Graph> match(Class<T> type, Map<String, Object> properties)
    {
        ClassMetadata<T> classMetadata = metadataMap.get(type);

        // Add the parent thing to save
        String query = "MATCH (e" + classMetadata.getLabelKey().asCypher() + ")";

        if (properties != null)
        {
            query += "WHERE ";
            for (String key : properties.keySet())
            {
                query += "e." + key + " = {" + key + "} ";
            }
        }

        query += " RETURN e";
        Statement<Graph> statement = new GraphStatement(query);

        if (properties != null)
        {
            for (Map.Entry<String, Object> entry : properties.entrySet())
            {
                statement.setParam(entry.getKey(), entry.getValue());
            }
        }

        return statement;
    }

    //
    //    merge.setParam("0", "abcd1234");
    //    merge.setParam("1", new JSONObject(entity));
    //    return merge;

    public <T> List<Statement> delete(T entity)
    {
        return null;
    }
}

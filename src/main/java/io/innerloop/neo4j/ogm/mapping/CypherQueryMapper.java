package io.innerloop.neo4j.ogm.mapping;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.GraphStatement;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.client.Transaction;
import io.innerloop.neo4j.ogm.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.metadata.MetadataMap;
import io.innerloop.neo4j.ogm.metadata.RelationshipMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        List<Statement> result = new ArrayList<>();
        ClassMetadata<T> classMetadata = metadataMap.get(entity);

        // Add the parent thing to save
        Statement<Graph> mergeStatement = new GraphStatement("MERGE (e" + classMetadata.getLabelKey().asCypher() +
                                                             "{uuid:{0}}) SET e = {1}");
        mergeStatement.setParam("0", entity);
        mergeStatement.setParam("1", classMetadata.toJsonObject(entity));
        result.add(mergeStatement);

        // recurse through it's relationships
        for (RelationshipMetadata rm : classMetadata.getRelationships())
        {
            Object value = rm.getValue(entity);

            if (value != null)
            {
                return merge(value);
            }
        }

        return result;
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
        String query = "MATCH (e" + classMetadata.getLabelKey().asCypher() + ") WHERE ";
        for (String key : properties.keySet())
        {
            query += "e." + key + " = {" + key + "} ";
        }
        query += " RETURN e";
        Statement<Graph> statement = new GraphStatement(query);

        for (Map.Entry<String, Object> entry: properties.entrySet())
        {
            statement.setParam(entry.getKey(), entry.getValue());
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

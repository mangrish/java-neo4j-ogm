package io.innerloop.neo4j.ogm.mapping;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.ogm.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.metadata.MetadataMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by markangrish on 28/01/2015.
 */
public class CypherMapper
{
    private final MetadataMap metadataMap;

    public CypherMapper(MetadataMap metadataMap)
    {
        this.metadataMap = metadataMap;
    }


    public <T> List<Statement> merge(T entity)
    {
        List<Statement> result = new ArrayList<>();
        ClassMetadata<T> classMetadata = metadataMap.get(entity);
        Statement<Graph> mergeStatement = classMetadata.getMergeStatement(entity);
        result.add(mergeStatement);
        //TODO: have to walk relationships on entity;
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

        return classMetadata.getMatchStatement();
    }

    public <T> List<Statement> delete(T entity)
    {
        return null;
    }
}

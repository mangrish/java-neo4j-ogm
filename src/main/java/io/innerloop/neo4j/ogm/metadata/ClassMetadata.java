package io.innerloop.neo4j.ogm.metadata;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.GraphStatement;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.client.json.JSONObject;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by markangrish on 28/01/2015.
 */
public class ClassMetadata<T>
{
    private final Class<T> type;

    private final String primaryLabel;

    private final SortedMultiLabel key;

    private final List<PropertyMetadata> fieldMetadata;

    private final boolean isAbstract;

    public ClassMetadata(Class<T> type,  String primaryLabel, SortedMultiLabel key)
    {
        this.type = type;
        this.primaryLabel = primaryLabel;
        this.key = key;
        this.fieldMetadata = new ArrayList<>();
        this.isAbstract = Modifier.isAbstract( type.getModifiers());
    }

    public void addPropertyMetadata(PropertyMetadata propertyMetadata)
    {
        fieldMetadata.add(propertyMetadata);
    }

    public Statement<Graph> getMatchStatement()
    {
        return null;
    }

    public Statement<Graph> getMergeStatement(T entity)
    {
        Statement<Graph> merge = new GraphStatement("MERGE (e" + key.asCypher() + "{uuid:{0}}) SET e = {1}");
        merge.setParam("0", "abcd1234");
        merge.setParam("1", new JSONObject(entity));
        return merge;
    }
}

package io.innerloop.neo4j.ogm.impl.mapping;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.innerloop.neo4j.client.RowSet;
import io.innerloop.neo4j.client.Statement;
import io.innerloop.neo4j.ogm.impl.metadata.ClassMetadata;
import io.innerloop.neo4j.ogm.impl.metadata.MetadataMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by markangrish on 23/03/2015.
 */
public class IdentityMap
{
    private final Map<Long, Object> objects;

    private final Map<Long, Long> objectHashes;

    private final MetadataMap metadataMap;

    private final Multimap<Object, Object> newObjects;

    public IdentityMap(MetadataMap metadataMap)
    {
        this.metadataMap = metadataMap;
        this.objects = new HashMap<>();
        this.objectHashes = new HashMap<>();
        this.newObjects = LinkedHashMultimap.create();
    }

    public Object get(Long id)
    {
        return objects.get(id);
    }

    public void put(Long id, Object instance)
    {
        if (!objects.containsKey(id))
        {
            objectHashes.put(id, hash(instance));
            objects.put(id, instance);
        }
    }

    public List<Object> getDirtyObjects()
    {
        List<Object> dirtyObjects = new ArrayList<>();
        for (Map.Entry<Long, Object> entry : objects.entrySet())
        {
            long hash = hash(entry.getValue());
            Long originalHash = objectHashes.get(entry.getKey());

            if (hash != originalHash)
            {
                dirtyObjects.add(entry.getValue());
            }
        }

        return dirtyObjects;
    }

    private long hash(Object object)
    {
        ClassMetadata<Object> metaData = metadataMap.get(object);
        return metaData.hash(object);
    }

    public void refresh()
    {
        for (Collection<Object> c : newObjects.asMap().values())
        {
            Iterator<Object> iterator = c.iterator();
            Object o1 = iterator.next();
            Object o2 = iterator.next();
            Statement s;
            if (o1 instanceof Statement)
            {
                s = (Statement) o1;
                long id = ((RowSet) s.getResult()).getLong(0);
                metadataMap.get(o2).getNeo4jIdField().setValue(id, o2);
                put(id, o2);
            }
            else
            {
                s = (Statement) o2;
                long id = ((RowSet) s.getResult()).getLong(0);
                metadataMap.get(o1).getNeo4jIdField().setValue(id, o1);
                put(id, o1);
            }
        }
    }

    public void addNew(Object ref, Statement<RowSet> statement)
    {
        ClassMetadata<Object> metaData = metadataMap.get(ref);
        Object primaryKey = metaData.getPrimaryField().getValue(ref);
        String key = Arrays.toString(metaData.getLabelKey().getLabels()) + ":" + primaryKey;

        if (!newObjects.containsKey(key))
        {
            newObjects.put(key, ref);
            newObjects.put(key, statement);
        }
    }
}

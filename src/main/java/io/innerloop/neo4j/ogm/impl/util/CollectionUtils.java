package io.innerloop.neo4j.ogm.impl.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by markangrish on 20/02/2015.
 */
public class CollectionUtils
{
    public static int size(Iterable<?> iterable)
    {
        return (iterable instanceof Collection) ? ((Collection<?>) iterable).size() : size(iterable.iterator());
    }

    public static int size(Iterator<?> iterator)
    {
        int count = 0;
        while (iterator.hasNext())
        {
            iterator.next();
            count++;
        }
        return count;
    }
}

package io.innerloop.neo4j.ogm;

import java.util.Collection;
import java.util.Iterator;

public class Utils
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

    public static boolean isEmpty(String string)
    {
        return string == null || string.length() == 0;
    }
}

package io.innerloop.neo4j.ogm.impl.util;

/**
 * Created by markangrish on 20/02/2015.
 */
public class StringUtils
{
    public static boolean isEmpty(String string)
    {
        return string == null || string.length() == 0;
    }

    public static boolean isNotEmpty(String string)
    {
        return string != null && string.length() > 0;
    }

}

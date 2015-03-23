package io.innerloop.neo4j.ogm.converters;


import java.time.YearMonth;

/**
 * Created by markangrish on 09/06/2014.
 */

public class YearMonthConverter implements Converter<YearMonth, String>
{
    @Override
    public String serialize(YearMonth yearMonth)
    {
        return yearMonth.toString();
    }

    @Override
    public YearMonth deserialize(String target)
    {
        return YearMonth.parse(target);
    }
}

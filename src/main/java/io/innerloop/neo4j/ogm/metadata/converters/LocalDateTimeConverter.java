package io.innerloop.neo4j.ogm.metadata.converters;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Created by markangrish on 09/06/2014.
 */

public class LocalDateTimeConverter implements Converter<LocalDateTime, Long>
{
    @Override
    public Long serialize(LocalDateTime dateTime)
    {
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    @Override
    public LocalDateTime deserialize(Long millis)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    }
}

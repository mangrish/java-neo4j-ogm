package io.innerloop.neo4j.ogm.converters;


import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Created by markangrish on 09/06/2014.
 */

public class LocalDateConverter implements Converter<LocalDate, Long>
{
    @Override
    public Long serialize(LocalDate date)
    {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    @Override
    public LocalDate deserialize(Long millis)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC).toLocalDate();
    }
}

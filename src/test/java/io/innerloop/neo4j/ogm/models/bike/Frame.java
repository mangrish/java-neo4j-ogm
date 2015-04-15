package io.innerloop.neo4j.ogm.models.bike;

import io.innerloop.neo4j.ogm.annotations.Aggregate;
import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.converters.UUIDConverter;
import io.innerloop.neo4j.ogm.models.utils.UuidGenerator;

import java.util.UUID;

@Aggregate
public class Frame
{
    public Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    private Integer size;

    public Frame()
    {
        this.uuid = UuidGenerator.generate();
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public Integer getSize()
    {
        return size;
    }

    public void setSize(Integer size)
    {
        this.size = size;
    }

}

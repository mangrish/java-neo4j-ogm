package io.innerloop.neo4j.ogm.models.bike;

import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.generators.UuidGenerator;

import java.util.UUID;

public class Wheel
{
    public Long id;

    @Id
    private UUID uuid;

    private Integer spokes;

    public Wheel()
    {
        this.uuid = UuidGenerator.generate();
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public void setUuid(UUID uuid)
    {
        this.uuid = uuid;
    }

    public Integer getSpokes()
    {
        return spokes;
    }

    public void setSpokes(Integer spokes)
    {
        this.spokes = spokes;
    }
}

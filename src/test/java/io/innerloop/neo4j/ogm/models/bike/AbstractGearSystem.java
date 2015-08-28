package io.innerloop.neo4j.ogm.models.bike;

import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.generators.UuidGenerator;

import java.util.UUID;

/**
 * Created by markangrish on 11/04/2015.
 */
public abstract class AbstractGearSystem implements GearSystem
{
    private Long id;

    @Id
    private UUID uuid;

    public AbstractGearSystem()
    {
        this.uuid = UuidGenerator.generate();
    }
}

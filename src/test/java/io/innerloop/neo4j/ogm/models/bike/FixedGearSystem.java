package io.innerloop.neo4j.ogm.models.bike;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.converters.UUIDConverter;
import io.innerloop.neo4j.ogm.models.utils.UuidGenerator;

import java.util.UUID;

/**
 * Created by markangrish on 03/04/2015.
 */
public class FixedGearSystem implements GearSystem
{
    private Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    public FixedGearSystem()
    {
        this.uuid = UuidGenerator.generate();
    }

    @Override
    public int getNumberOfGears()
    {
        return 1;
    }
}

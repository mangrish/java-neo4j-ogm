package io.innerloop.neo4j.ogm.models.bike;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.converters.UUIDConverter;
import io.innerloop.neo4j.ogm.models.utils.UuidGenerator;

import java.util.UUID;

/**
 * Created by markangrish on 03/04/2015.
 */
public class MultiRangeGearSystem implements GearSystem
{
    private Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    private int numFrontWheelGears;

    private int numBackWheelGears;

    public MultiRangeGearSystem()
    {
        this.uuid = UuidGenerator.generate();
    }

    public MultiRangeGearSystem(int numFrontWheelGears, int numBackWheelGears)
    {
        this.numFrontWheelGears = numFrontWheelGears;
        this.numBackWheelGears = numBackWheelGears;
    }

    @Override
    public int getNumberOfGears()
    {
        return numFrontWheelGears * numBackWheelGears;
    }
}

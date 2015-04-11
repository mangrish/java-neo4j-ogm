package io.innerloop.neo4j.ogm.models.bike;

/**
 * Created by markangrish on 03/04/2015.
 */
public class MultiRangeGearSystem extends AbstractGearSystem
{
    private int numFrontWheelGears;

    private int numBackWheelGears;

    public MultiRangeGearSystem()
    {

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

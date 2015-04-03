package io.innerloop.neo4j.ogm.models.bike;

/**
 * Created by markangrish on 03/04/2015.
 */
public class SpeedFrame extends Frame
{
    private String material;

    private double weight;

    public SpeedFrame()
    {
    }

    public SpeedFrame(String material, double weight)
    {

        this.material = material;
        this.weight = weight;
    }

    public String getMaterial()
    {

        return material;
    }

    public double getWeight()
    {
        return weight;
    }
}

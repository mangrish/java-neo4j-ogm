package io.innerloop.neo4j.ogm.models.bike;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by markangrish on 03/04/2015.
 */
public class SpeedFrame extends Frame
{
    private String material;

    private List<String> gearRatios;

    private double weight;

    public SpeedFrame()
    {
    }

    public SpeedFrame(String material, double weight)
    {
        this.material = material;
        this.weight = weight;
        this.gearRatios = new ArrayList<>();
    }

    public String getMaterial()
    {

        return material;
    }

    public double getWeight()
    {
        return weight;
    }

    public void addGearRatio(String ratio)
    {
        this.gearRatios.add(ratio);
    }

    public List<String> getGearRatios()
    {
        return gearRatios;
    }
}

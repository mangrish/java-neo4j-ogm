package io.innerloop.neo4j.ogm.models.bike;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.converters.UUIDConverter;
import io.innerloop.neo4j.ogm.models.utils.UuidGenerator;

import java.util.List;
import java.util.UUID;

public class Bike
{

    public Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    private GearSystem gearSystem;

    private String[] colours;

    private List<Wheel> wheels;

    private Frame frame;

    private Saddle saddle;

    private String brand;

    public Bike()
    {
        this.uuid = UuidGenerator.generate();
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public String getBrand()
    {
        return brand;
    }

    public void setBrand(String brand)
    {
        this.brand = brand;
    }

    public String[] getColours()
    {
        return colours;
    }

    public List<Wheel> getWheels()
    {
        return wheels;
    }

    public void setWheels(List<Wheel> wheels)
    {
        this.wheels = wheels;
    }

    public void setSaddle(Saddle saddle)
    {
        this.saddle = saddle;
    }

    public Frame getFrame()
    {
        return frame;
    }

    public Saddle getSaddle()
    {
        return saddle;
    }

    public void setColours(String[] colours)
    {
        this.colours = colours;
    }

    public void setFrame(Frame frame)
    {
        this.frame = frame;
    }

    public GearSystem getGearSystem()
    {
        return gearSystem;
    }

    public void setGearSystem(GearSystem gearSystem)
    {
        this.gearSystem = gearSystem;
    }
}

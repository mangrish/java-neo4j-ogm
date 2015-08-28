package io.innerloop.neo4j.ogm.models.bike;

import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.generators.UuidGenerator;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Bike
{
    public enum Logo
    {
        LOGO_1,
        LOGO_2,
        LOGO_3,
        LOGO_4,
        LOGO_5
    }

    public Long id;

    @Id
    private UUID uuid;

    private GearSystem gearSystem;

    private List<String> colours;

    private Set<Logo> logos;

    private List<Wheel> wheels;

    private Frame frame;

    private Saddle saddle;

    private String brand;

    public Bike()
    {
        this.uuid = UuidGenerator.generate();
    }

    public Set<Logo> getLogos()
    {
        return logos;
    }

    public void setLogos(Set<Logo> logos)
    {
        this.logos = logos;
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

    public List<String> getColours()
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

    public void setColours(List<String> colours)
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

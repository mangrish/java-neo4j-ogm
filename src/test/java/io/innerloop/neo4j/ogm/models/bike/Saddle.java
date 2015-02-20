package io.innerloop.neo4j.ogm.models.bike;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.metadata.converters.UUIDConverter;
import io.innerloop.neo4j.ogm.models.utils.UuidGenerator;

import java.util.UUID;

public class Saddle
{
    private Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    private Double price;

    private String material;

    public Saddle()
    {
        this.uuid = UuidGenerator.generate();
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public Double getPrice()
    {
        return price;
    }

    public void setPrice(Double price)
    {
        this.price = price;
    }

    public String getMaterial()
    {
        return material;
    }

    public void setMaterial(String material)
    {
        this.material = material;
    }
}

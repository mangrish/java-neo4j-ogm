package io.innerloop.neo4j.ogm.models.complex;

import io.innerloop.neo4j.ogm.annotations.Aggregate;
import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.converters.UUIDConverter;
import io.innerloop.neo4j.ogm.models.utils.UuidGenerator;

import java.util.UUID;

/**
 * Created by markangrish on 30/03/2015.
 */
@Aggregate
public class Category
{
    private Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    private String name;

    public Category()
    {
    }

    public Category(String name)
    {
        this.uuid = UuidGenerator.generate();
        this.name = name;
    }
}

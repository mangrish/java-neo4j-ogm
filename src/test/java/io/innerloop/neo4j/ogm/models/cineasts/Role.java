package io.innerloop.neo4j.ogm.models.cineasts;

import io.innerloop.neo4j.ogm.annotations.RelationshipProperties;

/**
 * Created by markangrish on 07/05/2015.
 */
@RelationshipProperties
public class Role
{
    private String name;

    public Role(String name)
    {
        this.name = name;
    }

    public Role()
    {
    }

    public String getName()
    {
        return name;
    }
}

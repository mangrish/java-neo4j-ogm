package io.innerloop.neo4j.ogm.models.cineasts;

/**
 * Created by markangrish on 17/12/2014.
 */


import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.converters.UUIDConverter;
import io.innerloop.neo4j.ogm.models.utils.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Actor
{
    private Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    private String name;

    private Set<Role> roles;

    public Actor()
    {
        // do nothing...
    }

    public Actor(String name)
    {
        this.uuid = UuidGenerator.generate();
        this.name = name;
        this.roles = new HashSet<>();
    }

    public Set<Role> getRoles()
    {
        return roles;
    }

    public Role playedIn(Movie movie, String roleName)
    {
        final Role role = new Role(this, movie, roleName);
        roles.add(role);
        return role;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}

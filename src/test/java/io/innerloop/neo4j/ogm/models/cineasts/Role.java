package io.innerloop.neo4j.ogm.models.cineasts;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.metadata.converters.UUIDConverter;
import io.innerloop.neo4j.ogm.models.utils.UuidGenerator;

import java.util.UUID;

/**
 * Created by markangrish on 17/12/2014.
 */

public class Role
{
    private Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    private Movie movie;

    private Actor actor;

    private String name;

    public Role()
    {
        // Do nothing...
    }

    public Role(Actor actor, Movie movie, String name)
    {
        uuid = UuidGenerator.generate();
        this.actor = actor;
        this.movie = movie;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Movie getMovie() {
        return movie;
    }

    public Actor getActor() {
        return actor;
    }

    @Override
    public String toString() {
        return String.format("%s acts as %s in %s", actor, name, movie);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Role role = (Role) o;
        if (uuid == null) return super.equals(o);
        return uuid.equals(role.uuid);

    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : super.hashCode();
    }


}

package io.innerloop.neo4j.ogm.models.cineasts;

/**
 * Created by markangrish on 17/12/2014.
 */

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.metadata.converters.UUIDConverter;
import io.innerloop.neo4j.ogm.metadata.converters.YearConverter;
import io.innerloop.neo4j.ogm.models.utils.UuidGenerator;

import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Movie
{
    private Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    String title;

    String description;

    Set<Actor> actors;

    List<Role> roles;

    @Convert(YearConverter.class)
    private Year releaseDate;

    public Movie()
    {
        // do nothing..
    }

    public Movie(String title, int releaseDate)
    {
        this.uuid = UuidGenerator.generate();
        this.title = title;
        this.releaseDate = Year.of(releaseDate);
    }

    public Collection<Actor> getActors()
    {
        return actors;
    }

    public Iterable<Role> getRoles()
    {
        return roles;
    }

    public int getYear()
    {
        return releaseDate.getValue();
    }

    public String getTitle()
    {
        return title;
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s) [%s]", title, releaseDate, uuid);
    }

    public String getDescription()
    {
        return description;
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Movie movie = (Movie) o;
        if (uuid == null)
            return super.equals(o);
        return uuid.equals(movie.uuid);

    }

    @Override
    public int hashCode()
    {
        return uuid != null ? uuid.hashCode() : super.hashCode();
    }
}

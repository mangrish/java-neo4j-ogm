package io.innerloop.neo4j.ogm.models.cineasts;

import io.innerloop.neo4j.ogm.annotations.Relationship;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by markangrish on 11/05/2015.
 */
public class Director extends Person
{
    @Relationship(type = "DIRECTED")
    private Set<Movie> directedMovies = new HashSet<>();

    public Director(int tmdbId,
                    String name,
                    LocalDate birthday,
                    String birthplace,
                    String biography,
                    String profileImageUrl)
    {
        super(tmdbId, name, birthday, birthplace, biography, profileImageUrl);
    }

    public Director()
    {
    }

    public Set<Movie> getDirectedMovies()
    {
        return directedMovies;
    }

    public void directed(Movie movie)
    {
        directedMovies.add(movie);
        movie.addDirector(this);
    }

}

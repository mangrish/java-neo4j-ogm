package io.innerloop.neo4j.ogm.models.cineasts;

import io.innerloop.neo4j.ogm.annotations.Relationship;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by markangrish on 07/05/2015.
 */
public class Actor extends Person
{
    @Relationship(type = "ACTS_IN")
    private Map<Movie, Role> roles = new HashMap<>();

    public Actor()
    {
    }

    public Actor(int tmdbId,
                 String name,
                 LocalDate birthday,
                 String birthplace,
                 String biography,
                 String profileImageUrl)
    {
        super(tmdbId, name, birthday, birthplace, biography, profileImageUrl);
    }


    public Role playedIn(Movie movie, String roleName)
    {
        final Role role = new Role(roleName);
        roles.put(movie, role);
        movie.addRole(this, role);
        return role;
    }

    public Map<Movie, Role> getRoles()
    {
        return roles;
    }
}

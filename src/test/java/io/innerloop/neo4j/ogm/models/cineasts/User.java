package io.innerloop.neo4j.ogm.models.cineasts;

import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.annotations.Relationship;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by markangrish on 07/05/2015.
 */
public class User implements Principal
{
    public enum SecurityRole
    {
        ROLE_USER,
        ROLE_ADMIN
    }

    private Long id;

    @Id
    private String login;

    private String name;

    private String password;

    private String info;

    @Relationship(type = "FRIEND", direction = Relationship.Direction.UNDIRECTED)
    private Set<User> friends = new HashSet<>();

    private List<SecurityRole> roles = new ArrayList<>();

    @Relationship(type = "RATED")
    private Set<Movie> favorites = new HashSet<>();

    @Relationship(type = "RATED")
    private Map<Movie, Rating> ratings = new HashMap<>();

    public User(String login, String name, String password)
    {
        this.login = login;
        this.name = name;
        this.password = password;
    }

    public User(String login, String name, String password, SecurityRole... roles)
    {
        this.login = login;
        this.name = name;
        this.password = password;
        this.roles = Arrays.asList(roles);
    }

    public User()
    {
    }


    public void updatePassword(String old, String newPass1, String newPass2)
    {
        if (!checkPassword(old))
        {
            throw new IllegalArgumentException("Existing Password invalid");
        }
        if (!newPass1.equals(newPass2))
        {
            throw new IllegalArgumentException("New Passwords don't match");
        }
        this.password = newPass1;
    }

    public boolean checkPassword(String password)
    {
        return this.password.equals(password);
    }

    public boolean isInRole(String role)
    {
        return false;
    }


    public Rating rate(Movie movie, int stars, String comment)
    {
        Rating rating = new Rating(stars, comment);
        ratings.put(movie, rating);
        movie.addRating(this, rating);
        return rating;
    }

    public List<SecurityRole> getRole()
    {
        return roles;
    }


    public void addFriend(User friend)
    {
        this.friends.add(friend);
    }

    public boolean isFriend(User other)
    {
        return other != null && friends.contains(other);
    }

    public String getLogin()
    {
        return login;
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s)", name, login);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof User))
        {
            return false;
        }

        User user = (User) o;

        return !(login != null ? !login.equals(user.login) : user.login != null);

    }

    @Override
    public int hashCode()
    {
        return login != null ? login.hashCode() : 0;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public String getInfo()
    {
        return info;
    }

    public void setInfo(String info)
    {
        this.info = info;
    }

    public Set<User> getFriends()
    {
        return friends;
    }

    public List<SecurityRole> getRoles()
    {
        return roles;
    }

    public Map<Movie, Rating> getRatings()
    {
        return ratings;
    }

    public Rating getRatingFor(Movie movie)
    {
        return ratings.get(movie);
    }
}

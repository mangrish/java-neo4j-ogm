package io.innerloop.neo4j.ogm.models.cineasts;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.annotations.Relationship;
import io.innerloop.neo4j.ogm.converters.LocalDateConverter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by markangrish on 07/05/2015.
 */
public class Movie
{
    private Long id;

    @Id
    private Integer tmdbId;

    private String title;

    private String description;

    @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
    private Set<Director> directors = new HashSet<>();

    @Relationship(type = "ACTS_IN", direction = Relationship.Direction.INCOMING)
    private Map<Actor, Role> roles = new HashMap<>();

    @Relationship(type = "RATED", direction = Relationship.Direction.INCOMING)
    private Map<User, Rating> ratings = new HashMap<>();

    private String language;

    private String imdbId;

    private String tagline;

    @Convert(LocalDateConverter.class)
    private LocalDate releaseDate;

    private Integer runtime;

    private String homepage;

    private String trailer;

    private String genre;

    private String studio;

    private String imageUrl;


    public Movie()
    {
    }

    public Movie(int tmdbId,
                 String title,
                 String description,
                 String imdbId,
                 String language,
                 String tagline,
                 LocalDate releaseDate,
                 Integer runtime,
                 String trailer,
                 String homepage,
                 String studio,
                 String imageUrl,
                 String genre)
    {
        this.tmdbId = tmdbId;
        this.title = title;
        this.description = description;
        this.imdbId = imdbId;
        this.language = language;
        this.tagline = tagline;
        this.releaseDate = releaseDate;
        this.runtime = runtime;
        this.trailer = trailer;
        this.homepage = homepage;
        this.studio = studio;
        this.imageUrl = imageUrl;
        this.genre = genre;
    }

    public void addRole(Actor actor, Role role)
    {
        roles.put(actor, role);
    }

    public int getStars()
    {
        Iterable<Rating> allRatings = ratings.values();

        int stars = 0, count = 0;
        for (Rating rating : allRatings)
        {
            stars += rating.getStars();
            count++;
        }
        return count == 0 ? 0 : stars / count;
    }


    public void addDirector(Director director)
    {
        directors.add(director);
    }


    public void addRating(User user, Rating rating)
    {
        ratings.put(user, rating);
    }

    public String getYoutubeId()
    {
        String trailerUrl = trailer;
        if (trailerUrl == null || !trailerUrl.contains("youtu"))
        {
            return null;
        }
        String[] parts = trailerUrl.split("[=/]");
        int numberOfParts = parts.length;
        return numberOfParts > 0 ? parts[numberOfParts - 1] : null;
    }


    public String getTitle()
    {
        return title;
    }

    public String getDescription()
    {
        return description;
    }

    public Set<Director> getDirectors()
    {
        return directors;
    }

    public Map<Actor, Role> getRoles()
    {
        return roles;
    }


    public Map<User, Rating> getRatings()
    {
        return ratings;
    }

    public String getLanguage()
    {
        return language;
    }

    public String getImdbId()
    {
        return imdbId;
    }

    public String getTagline()
    {
        return tagline;
    }

    public LocalDate getReleaseDate()
    {
        return releaseDate;
    }

    public Integer getRuntime()
    {
        return runtime;
    }

    public String getHomepage()
    {
        return homepage;
    }

    public String getTrailer()
    {
        return trailer;
    }

    public String getGenre()
    {
        return genre;
    }

    public String getStudio()
    {
        return studio;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s) [%s]", title, releaseDate, tmdbId);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Movie))
        {
            return false;
        }

        Movie movie = (Movie) o;

        if (tmdbId != null ? !tmdbId.equals(movie.tmdbId) : movie.tmdbId != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return tmdbId != null ? tmdbId.hashCode() : 0;
    }

    public Integer getTmdbId()
    {
        return tmdbId;
    }
}

package io.innerloop.neo4j.ogm.models.cineasts;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.impl.converters.LocalDateConverter;

import java.time.LocalDate;

/**
 * Created by markangrish on 07/05/2015.
 */
public abstract class Person
{
    private Long id;

    @Id
    private Integer tmdbId;

    private String name;

    @Convert(LocalDateConverter.class)
    private LocalDate birthday;

    private String birthplace;

    private String biography;

    private String profileImageUrl;

    public Person(int tmdbId,
                  String name,
                  LocalDate birthday,
                  String birthplace,
                  String biography,
                  String profileImageUrl)
    {
        this.name = name;
        this.tmdbId = tmdbId;
        this.birthday = birthday;
        this.birthplace = birthplace;
        this.biography = biography;
        this.profileImageUrl = profileImageUrl;
    }

    public Person()
    {
    }

    public Integer getTmdbId()
    {
        return tmdbId;
    }

    public String getName()
    {
        return name;
    }

    public LocalDate getBirthday()
    {
        return birthday;
    }

    public String getBirthplace()
    {
        return birthplace;
    }

    public String getBiography()
    {
        return biography;
    }

    public String getProfileImageUrl()
    {
        return profileImageUrl;
    }

    @Override
    public String toString()
    {
        return String.format("%s [%s]", name, tmdbId);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Person))
        {
            return false;
        }

        Person person = (Person) o;

        return !(tmdbId != null ? !tmdbId.equals(person.tmdbId) : person.tmdbId != null);

    }

    @Override
    public int hashCode()
    {
        return tmdbId != null ? tmdbId.hashCode() : 0;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}

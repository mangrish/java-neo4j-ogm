package io.innerloop.neo4j.ogm.models.complex;

import io.innerloop.neo4j.ogm.annotations.Convert;
import io.innerloop.neo4j.ogm.annotations.Fetch;
import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.converters.LocalDateTimeConverter;
import io.innerloop.neo4j.ogm.converters.UUIDConverter;
import io.innerloop.neo4j.ogm.models.utils.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by markangrish on 07/04/2014.
 */
public class Subject
{
    public enum Status
    {
        NEW,
        VERIFYING,
        VERIFIED,
        DELETED,
        CHANGED_TO_ALIAS
    }

    private Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    private String name;

    private Status status;

    @Convert(LocalDateTimeConverter.class)
    private LocalDateTime verifiedDate;

    @Convert(LocalDateTimeConverter.class)
    private LocalDateTime lastUpdatedDate;

    private String description;

    private String website;

    private String imageUrl;

    private String disambiguation;

    @Fetch
    private Map<Subject, Double> requiredKnowledge;

    private Set<Category> categories;

    private Set<Alias> aliases;

    private List<Double> decayCoefficient;


    public Subject(String name)
    {
        this();
        this.uuid = UuidGenerator.generate();
        this.status = Status.NEW;
        this.name = name;
    }

    public Subject()
    {
        this.categories = new HashSet<>();
        this.aliases = new HashSet<>();
        this.requiredKnowledge = new HashMap<>();
        this.decayCoefficient = new ArrayList<>();
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public String getName()
    {
        return name;
    }

    public void addCategory(Category category)
    {
        categories.add(category);
    }

    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }

    public Status getStatus()
    {
        return status;
    }

    public void requires(Subject subject, double correlation)
    {
        requiredKnowledge.put(subject, correlation);
    }

    public Set<Alias> getAliases()
    {
        return aliases;
    }

    public Set<Category> getCategories()
    {
        return categories;
    }
}

package io.innerloop.neo4j.ogm.models.complex;

import io.innerloop.neo4j.ogm.annotations.Id;
import io.innerloop.neo4j.ogm.generators.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private UUID uuid;

    private String name;

    private Status status;

    private LocalDateTime verifiedDate;

    private LocalDateTime lastUpdatedDate;

    private String description;

    private String website;

    private String imageUrl;

    private String disambiguation;

    private Map<Subject, WeightedRelationship> requiredKnowledge;

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
        requiredKnowledge.put(subject, new WeightedRelationship(correlation));
    }

    public Set<Alias> getAliases()
    {
        return aliases;
    }

    public Set<Category> getCategories()
    {
        return categories;
    }

    public Map<Subject, WeightedRelationship> getWeightedRequiredKnowledge()
    {
        return requiredKnowledge;
    }

    public List<Subject> getRequiredKnowledge()
    {
        return requiredKnowledge.entrySet()
                       .stream()
                       .sorted((o1, o2) -> Double.compare(o1.getValue().getWeight(), o2.getValue().getWeight()))
                       .map(Map.Entry::getKey)
                       .collect(Collectors.toList());
    }
}

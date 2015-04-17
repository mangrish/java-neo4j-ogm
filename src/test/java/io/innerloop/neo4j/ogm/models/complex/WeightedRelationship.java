package io.innerloop.neo4j.ogm.models.complex;

import io.innerloop.neo4j.ogm.annotations.RelationshipProperties;

/**
 * Created by markangrish on 17/04/2015.
 */
@RelationshipProperties
public class WeightedRelationship
{
    private double weight;

    public WeightedRelationship(double weight)
    {
        this.weight = weight;
    }

    public WeightedRelationship()
    {
    }

    public double getWeight()
    {
        return weight;
    }
}

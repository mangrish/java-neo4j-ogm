package io.innerloop.neo4j.ogm.models.bike;

import io.innerloop.neo4j.ogm.annotations.Aggregate;

/**
 * Created by markangrish on 03/04/2015.
 */
@Aggregate
public interface GearSystem
{
    int getNumberOfGears();
}

package io.innerloop.neo4j.ogm.impl;

import java.util.List;
import java.util.Map;

/**
 * Created by markangrish on 19/02/2015.
 */
public class UnitOfWork
{
    private Map<Long, Object> identityMap;

    private Map<Long, Object> dirtyObjects;

    private Map<Long, Object> deletedObjects;

    private List<Object> newObjects;


}

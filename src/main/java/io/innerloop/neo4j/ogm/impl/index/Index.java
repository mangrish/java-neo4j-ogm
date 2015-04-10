package io.innerloop.neo4j.ogm.impl.index;

import io.innerloop.neo4j.client.RowStatement;
import io.innerloop.neo4j.client.Statement;

/**
 * Created by markangrish on 28/03/2015.
 */
public class Index
{
    private final String label;

    private final String propertyName;

    private final boolean unique;

    public Index(String label, String propertyName, boolean unique)
    {
        this.label = label;
        this.propertyName = propertyName;
        this.unique = unique;
    }

    public Statement drop()
    {
        if (unique)
        {
            return new RowStatement("DROP CONSTRAINT ON (n:`" + label + "`) ASSERT n.`" + propertyName +
                                    "` IS UNIQUE");
        }
        return new RowStatement("DROP INDEX ON :`" + label + "`(`" + propertyName + "`)");
    }

    public Statement create()
    {
        if (unique)
        {
            return new RowStatement("CREATE CONSTRAINT ON (n:`" + label + "`) ASSERT n.`" + propertyName +
                                    "` IS UNIQUE");
        }
        return new RowStatement("CREATE INDEX ON :`" + label + "`(`" + propertyName + "`)");
    }
}

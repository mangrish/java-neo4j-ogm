package io.innerloop.neo4j.ogm.mapping;

import java.util.Arrays;

/**
 * Created by markangrish on 14/11/2014.
 */
public class MetaDataLabelKey
{
    private final String[] labels;

    public MetaDataLabelKey(String[] labels)
    {
        this.labels = labels;
    }

    public String[] getLabels()
    {
        return labels;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        MetaDataLabelKey that = (MetaDataLabelKey) o;

        return Arrays.deepEquals(labels, that.labels);

    }

    @Override
    public int hashCode()
    {
        return labels != null ? Arrays.deepHashCode(labels) : 0;
    }


    @Override
    public String toString()
    {
        return "MetaDataLabelKey{" +
               "labels=" + Arrays.toString(labels) +
               '}';
    }
}

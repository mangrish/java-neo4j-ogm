package io.innerloop.neo4j.ogm.impl.metadata;

import java.util.Arrays;

/**
 * Created by markangrish on 14/11/2014.
 */
public class NodeLabel
{
    private final String[] labels;

    public NodeLabel(String[] labels)
    {
        Arrays.sort(labels);
        this.labels = labels;
    }

    public String[] getLabels()
    {
        return labels;
    }

    public String asCypher()
    {
        String result = "";
        for (String label : labels)
        {
            result += ":" + label;
        } return result;
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

        NodeLabel that = (NodeLabel) o;

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

package io.innerloop.neo4j.ogm.mapping.converters;

public interface Converter<S, T>
{
    T serialize(S source);

    S deserialize(T target);
}

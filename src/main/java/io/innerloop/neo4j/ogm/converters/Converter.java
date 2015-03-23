package io.innerloop.neo4j.ogm.converters;

public interface Converter<S, T>
{
    T serialize(S source);

    S deserialize(T target);
}

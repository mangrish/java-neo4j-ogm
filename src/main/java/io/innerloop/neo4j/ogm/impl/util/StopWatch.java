package io.innerloop.neo4j.ogm.impl.util;

import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StopWatch
{
    private static TimeUnit selectTimeUnitForDisplay(long durationInNanos)
    {
        if (durationInNanos < 1000L)
        {
            return TimeUnit.NANOSECONDS;
        }
        if (durationInNanos < 1000000L)
        {
            return TimeUnit.MICROSECONDS;
        }
        if (durationInNanos < 1000000000L)
        {
            return TimeUnit.MILLISECONDS;
        }
        if (durationInNanos < 1000000000000L)
        {
            return TimeUnit.SECONDS;
        }
        return TimeUnit.MINUTES;
    }

    private long startTime;

    private long globalElapsedTime;

    private final Logger log;

    private final String name;

    private long previousTime;

    private final LinkedHashMap<String, Long> splits;

    public StopWatch(String name, Logger log)
    {
        this.name = name;
        this.log = log;
        this.splits = new LinkedHashMap<>();
    }

    public void start()
    {
        startTime = System.nanoTime();
        previousTime = startTime;
    }

    public void split(String name)
    {
        long split = System.nanoTime();
        splits.put(name, split - previousTime);
        previousTime = split;
    }

    public void stop()
    {
        globalElapsedTime = System.nanoTime() - startTime;
        log();
    }

    void log()
    {
        for (Map.Entry<String, Long> split : splits.entrySet())
        {
            final TimeUnit timeUnit = selectTimeUnitForDisplay(split.getValue());
            log.debug("{} - {} took [{}] {}",
                      new Object[] {name,
                                    split.getKey(),
                                    timeUnit.convert(split.getValue(), TimeUnit.NANOSECONDS),
                                    timeUnit.toString()});
        }

        final TimeUnit timeUnit = selectTimeUnitForDisplay(globalElapsedTime);
        log.debug("[{}]TOTAL TIME [{}] {}",
                  new Object[] {name, timeUnit.convert(globalElapsedTime, TimeUnit.NANOSECONDS), timeUnit.toString()});
    }
}

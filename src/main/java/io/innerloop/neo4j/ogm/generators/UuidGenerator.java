package io.innerloop.neo4j.ogm.generators;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

import java.util.UUID;

/**
 * Created by markangrish on 07/05/2015.
 */
public class UuidGenerator
{
    private static EthernetAddress nic = EthernetAddress.fromInterface();

    private static TimeBasedGenerator uuidGenerator = Generators.timeBasedGenerator(nic);

    public static UUID generate()
    {
        return uuidGenerator.generate();
    }
}

package io.innerloop.neo4j.ogm;

import static org.junit.Assert.*;

import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.ogm.models.basic.Actor;
import io.innerloop.neo4j.ogm.models.basic.Movie;
import io.innerloop.neo4j.ogm.models.basic.Role;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.helpers.CommunityServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

/**
 * Created by markangrish on 17/12/2014.
 */
public class EndToEndTests
{
    private static final Logger LOG = LoggerFactory.getLogger(EndToEndTests.class);

    private static SessionFactory sessionFactory;

    private CommunityNeoServer server;

    @BeforeClass
    public static void oneTimeSetUp()
    {
        Neo4jClient client = new Neo4jClient("http://localhost:7474/db/data");
        sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.basic");
    }

    @Before
    public void setUp() throws IOException, InterruptedException
    {
        int port = new ServerSocket(0).getLocalPort();
        LOG.info("Starting community Neo4j server on port [{}]", port);
        server = CommunityServerBuilder.server().onPort(port).build();
        server.start();

        while (!server.getDatabase().isRunning())
        {
            // It's ok to spin here.. it's not production code.
            Thread.sleep(250);
        }
        LOG.info("Community Neo4j server started");
    }

    @After
    public void tearDown()
    {
        server.stop();
    }


    @Test
    public void testSaveReachableDomainObjectsAndRelationships()
    {
        Session session = sessionFactory.openSession();
        Actor keanu = new Actor("Keanu Reeves");
        Movie matrix = new Movie("Matrix", 1999);
        Role neo = new Role(keanu, matrix, "Neo");

        session.save(neo);

        List<Actor> actors = session.loadAll(Actor.class);

        assertEquals(1, actors.size());
    }
}

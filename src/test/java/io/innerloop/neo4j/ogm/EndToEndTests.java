package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.client.Neo4jClientException;
import io.innerloop.neo4j.client.Transaction;
import io.innerloop.neo4j.ogm.models.basic.Actor;
import io.innerloop.neo4j.ogm.models.basic.Movie;
import io.innerloop.neo4j.ogm.models.basic.Role;
import org.junit.AfterClass;
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

import static org.junit.Assert.assertEquals;

/**
 * Created by markangrish on 17/12/2014.
 */
public class EndToEndTests
{
    private static final Logger LOG = LoggerFactory.getLogger(EndToEndTests.class);

    private static SessionFactory sessionFactory;

    private static CommunityNeoServer server;

    @BeforeClass
    public static void oneTimeSetUp() throws IOException, InterruptedException
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

        Neo4jClient client = new Neo4jClient("http://localhost:" + port + "/db/data");
        sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.basic");
    }

    @Before
    public void setUp()
    {

    }

    @AfterClass
    public static void oneTimeTearDown()
    {
        server.stop();
    }


    @Test
    public void testSaveReachableDomainObjectsAndRelationships() throws Neo4jClientException
    {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.getTransaction();

        try
        {
            Actor keanu = new Actor("Keanu Reeves");
            Movie matrix = new Movie("Matrix", 1999);
            Role neo = new Role(keanu, matrix, "Neo");

            session.save(neo);
            transaction.commit();
        }
        catch (Neo4jClientException e)
        {
            transaction.rollback();
        }

        List<Actor> actors = session.loadAll(Actor.class);

        assertEquals(1, actors.size());
    }
}

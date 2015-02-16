package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.client.Neo4jClientException;
import io.innerloop.neo4j.client.Transaction;
import io.innerloop.neo4j.ogm.models.basic.Actor;
import io.innerloop.neo4j.ogm.models.basic.Movie;
import io.innerloop.neo4j.ogm.models.basic.Role;
import org.junit.After;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by markangrish on 17/12/2014.
 */
public class EndToEndTests
{
    private static final Logger LOG = LoggerFactory.getLogger(EndToEndTests.class);

    private SessionFactory sessionFactory;

    private CommunityNeoServer server;

    @BeforeClass
    public static void oneTimeSetUp()
    {

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

        Neo4jClient client = new Neo4jClient("http://localhost:" + port + "/db/data");
        sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.basic");
    }

    @After
    public void tearDown()
    {
        server.stop();
    }

    @AfterClass
    public static void oneTimeTearDown()
    {

    }


    @Test
    public void testFindDomainObjectReturnsNull() throws Neo4jClientException
    {
        Session session = sessionFactory.openSession();
        session.getTransaction().begin();
        // don't load anything into the database. Just look for it.
        Actor actor = session.load(Actor.class, "name", "Keanu Reeves");
        assertNull(actor);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testSaveReachableDomainObjectsAndRelationships() throws Neo4jClientException
    {
        Session session = sessionFactory.openSession();
        Transaction txn = session.getTransaction();
        txn.begin();

        Actor keanu = new Actor("Keanu Reeves");
        Movie matrix = new Movie("Matrix", 1999);
        keanu.playedIn(matrix, "Neo");

        session.save(keanu);

        List<Actor> actors = session.loadAll(Actor.class, "name", "Keanu Reeves");
        assertEquals(1, actors.size());
        Actor a = actors.iterator().next();
        assertTrue(a.getName().equals("Keanu Reeves"));
        assertEquals(1, a.getRoles().size());

        List<Movie> movies = session.loadAll(Movie.class);

        assertEquals(1, movies.size());

        List<Role> roles = session.loadAll(Role.class);

        assertEquals(1, roles.size());

        txn.commit();
        session.close();
    }

    @Test
    public void testSaveDomainObjectsThenDelete() throws Neo4jClientException
    {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.getTransaction();
        transaction.begin();

        Actor keanu = new Actor("Keanu Reeves");
        Movie matrix = new Movie("Matrix", 1999);
        keanu.playedIn(matrix,"Neo");
        session.save(keanu);
        session.delete(keanu);

        List<Actor> actors = session.loadAll(Actor.class, "name", "Keanu Reeves");
        assertEquals(0, actors.size());
        List<Movie> movies = session.loadAll(Movie.class);
        assertEquals(1, movies.size());
        List<Role> roles = session.loadAll(Role.class);
        assertEquals(1, roles.size());
        Role role = roles.iterator().next();
        assertNull(role.getActor());

        transaction.commit();
        session.close();

    }

    @Test
    public void testSaveLoadMutateThenSave() throws Neo4jClientException
    {
        Session session = sessionFactory.openSession();
        Transaction txn1 = session.getTransaction();
        txn1.begin();

        Actor keanu = new Actor("Keanu Reeves");
        Movie matrix = new Movie("Matrix", 1999);
        keanu.playedIn(matrix,"Neo");
        session.save(keanu);
        txn1.commit();

        Transaction txn2 = session.getTransaction();
        txn2.begin();

        List<Actor> actors = session.loadAll(Actor.class, "name", "Keanu Reeves");
        assertEquals(1, actors.size());

        Actor newKeanu = actors.iterator().next();
        newKeanu.playedIn(new Movie("Bill and Ted's Excellent Adventure", 1986), "Bill");
        session.save(newKeanu);
        txn2.commit();

        Transaction txn3 = session.getTransaction();
        txn3.begin();
        Actor retrievedKeanu = session.load(Actor.class, "name", "Keanu Reeves");
        assertEquals(2, retrievedKeanu.getRoles().size());
        txn3.commit();
        session.close();
    }
}

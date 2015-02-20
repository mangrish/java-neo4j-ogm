package io.innerloop.neo4j.ogm;

import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.client.Neo4jClientException;
import io.innerloop.neo4j.client.Transaction;
import io.innerloop.neo4j.ogm.models.bike.Bike;
import io.innerloop.neo4j.ogm.models.bike.Frame;
import io.innerloop.neo4j.ogm.models.bike.Saddle;
import io.innerloop.neo4j.ogm.models.bike.Wheel;
import io.innerloop.neo4j.ogm.models.cineasts.Actor;
import io.innerloop.neo4j.ogm.models.cineasts.Movie;
import io.innerloop.neo4j.ogm.models.cineasts.Role;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by markangrish on 17/12/2014.
 */
public class EndToEndTests
{
    private static final Logger LOG = LoggerFactory.getLogger(EndToEndTests.class);

    private Neo4jClient client;

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

        client = new Neo4jClient("http://localhost:" + port + "/db/data");
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
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.cineasts");
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
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.cineasts");
        Session session = sessionFactory.openSession();
        try
        {
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
        }
        finally
        {
            session.close();
        }
    }

    @Test
    public void testSaveDomainObjectsThenDelete() throws Neo4jClientException
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.cineasts");
        Session session = sessionFactory.openSession();
        try
        {

            Transaction transaction = session.getTransaction();
            transaction.begin();

            Actor keanu = new Actor("Keanu Reeves");
            Movie matrix = new Movie("Matrix", 1999);
            keanu.playedIn(matrix, "Neo");
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
        }
        finally
        {
            session.close();

        }

    }

    @Test
    public void testSaveLoadMutateThenSave() throws Neo4jClientException
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.cineasts");
        Session session = sessionFactory.openSession();
        try
        {
            Transaction txn1 = session.getTransaction();
            txn1.begin();

            Actor keanu = new Actor("Keanu Reeves");
            Movie matrix = new Movie("Matrix", 1999);
            keanu.playedIn(matrix, "Neo");
            session.save(keanu);

            List<Actor> actors = session.loadAll(Actor.class, "name", "Keanu Reeves");
            assertEquals(1, actors.size());

            Actor newKeanu = actors.iterator().next();
            newKeanu.playedIn(new Movie("Bill and Ted's Excellent Adventure", 1986), "Bill");
            session.save(newKeanu);
            txn1.commit();

            Transaction txn2 = session.getTransaction();
            txn2.begin();
            Actor retrievedKeanu = session.load(Actor.class, "name", "Keanu Reeves");
            assertEquals(2, retrievedKeanu.getRoles().size());
            txn2.commit();
        }
        finally
        {
            session.close();
        }
    }

    @Test
    public void canSimpleQueryDatabase() throws Neo4jClientException
    {

        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.openSession();
        try
        {
            Transaction transaction = session.getTransaction();
            transaction.begin();
            Saddle expected = new Saddle();
            expected.setPrice(29.95);
            expected.setMaterial("Leather");
            Wheel frontWheel = new Wheel();
            Wheel backWheel = new Wheel();
            Bike bike = new Bike();
            bike.setBrand("Huffy");
            bike.setWheels(Arrays.asList(frontWheel, backWheel));
            bike.setSaddle(expected);
            session.save(bike);

            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("material", "Leather");
            Saddle actual = session.queryForObject(Saddle.class,
                                                   "MATCH (saddle:Saddle{material: {material}}) RETURN saddle",
                                                   parameters);

            assertEquals(expected.getUuid(), actual.getUuid());
            assertEquals(expected.getMaterial(), actual.getMaterial());

            HashMap<String, Object> parameters2 = new HashMap<>();
            parameters2.put("brand", "Huffy");
            Bike actual2 = session.queryForObject(Bike.class,
                                                  "MATCH (bike:Bike{brand: {brand}}) RETURN bike",
                                                  parameters2);

            assertEquals(bike.getUuid(), actual2.getUuid());
            assertEquals(bike.getBrand(), actual2.getBrand());
            assertEquals(bike.getSaddle(), actual2.getSaddle());
            assertEquals(bike.getWheels().size(), actual2.getWheels().size());

            transaction.commit();
        }
        finally
        {
            session.close();
        }
    }


    @Test
    public void canSimpleScalarQueryDatabase() throws Neo4jClientException
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.openSession();
        try
        {
            Transaction transaction = session.getTransaction();
            transaction.begin();

            Saddle expected = new Saddle();
            expected.setPrice(29.95);
            expected.setMaterial("Leather");
            session.save(expected);

            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("material", "Leather");
            int actual = session.queryForObject(Integer.class,
                                                "MATCH (saddle:Saddle{material: {material}}) RETURN COUNT(saddle)",
                                                parameters);

            assertEquals(1, actual);
            transaction.commit();
        }
        finally
        {
            session.close();
        }
    }

    @Test
    public void canComplexQueryDatabase() throws Neo4jClientException
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.openSession();
        try
        {
            Transaction transaction = session.getTransaction();
            transaction.begin();
            Saddle saddle = new Saddle();
            saddle.setPrice(29.95);
            saddle.setMaterial("Leather");
            Wheel frontWheel = new Wheel();
            Wheel backWheel = new Wheel();
            Bike bike = new Bike();
            bike.setBrand("Huffy");
            bike.setWheels(Arrays.asList(frontWheel, backWheel));
            bike.setSaddle(saddle);

            session.save(bike);

            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("brand", "Huffy");
            Bike actual = session.queryForObject(Bike.class,
                                                 "MATCH (bike:Bike{brand:{brand}})-[rels]-() RETURN bike, COLLECT(DISTINCT rels) as rels",
                                                 parameters);

            assertEquals(bike.getUuid(), actual.getUuid());
            assertEquals(bike.getBrand(), actual.getBrand());
            assertEquals(bike.getWheels().size(), actual.getWheels().size());
            assertNotNull(actual.getSaddle());
            transaction.commit();
        }
        finally
        {
            session.close();
        }
    }

    @Test
    public void canComplexExecute() throws Neo4jClientException
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.openSession();
        try
        {
            Transaction transaction = session.getTransaction();
            transaction.begin();
            Saddle saddle = new Saddle();
            saddle.setPrice(29.95);
            saddle.setMaterial("Leather");
            Wheel frontWheel = new Wheel();
            Wheel backWheel = new Wheel();
            Bike bike = new Bike();
            bike.setBrand("Huffy");
            bike.setWheels(Arrays.asList(frontWheel, backWheel));
            bike.setSaddle(saddle);

            session.save(bike);

            Saddle newSaddle = new Saddle();
            newSaddle.setPrice(19.95);
            newSaddle.setMaterial("Vinyl");
            bike.setSaddle(newSaddle);

            session.save(bike);

            HashMap<String, Object> parameters2 = new HashMap<>();
            parameters2.put("brand", "Huffy");
            Bike actual = session.queryForObject(Bike.class,
                                                 "MATCH (bike:Bike{brand:{brand}})-[rels]-() RETURN bike, COLLECT(DISTINCT rels) as rels",
                                                 parameters2);

            assertEquals(bike.getUuid(), actual.getUuid());
            assertEquals(bike.getBrand(), actual.getBrand());
            assertEquals(bike.getWheels().size(), actual.getWheels().size());
            assertEquals("Vinyl", actual.getSaddle().getMaterial());
            transaction.commit();
        }
        finally
        {
            session.close();
        }
    }

    @Test
    public void canSaveNewObjectTreeToDatabase() throws Neo4jClientException
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.openSession();
        try
        {
            Transaction transaction = session.getTransaction();
            transaction.begin();

            Wheel frontWheel = new Wheel();
            Wheel backWheel = new Wheel();
            Bike bike = new Bike();

            bike.setFrame(new Frame());
            bike.setSaddle(new Saddle());
            bike.setWheels(Arrays.asList(frontWheel, backWheel));

            assertNull(frontWheel.getUuid());
            assertNull(backWheel.getUuid());
            assertNull(bike.getUuid());
            assertNull(bike.getFrame().getUuid());
            assertNull(bike.getSaddle().getUuid());

            session.save(bike);

            assertNotNull(frontWheel.getUuid());
            assertNotNull(backWheel.getUuid());
            assertNotNull(bike.getUuid());
            assertNotNull(bike.getFrame().getUuid());
            assertNotNull(bike.getSaddle().getUuid());

            transaction.commit();
        }
        finally
        {
            session.close();
        }
    }
}

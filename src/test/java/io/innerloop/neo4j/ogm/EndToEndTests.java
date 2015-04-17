package io.innerloop.neo4j.ogm;

import ch.qos.logback.classic.LoggerContext;
import io.innerloop.neo4j.client.Neo4jClient;
import io.innerloop.neo4j.client.Neo4jClientException;
import io.innerloop.neo4j.ogm.models.bike.Bike;
import io.innerloop.neo4j.ogm.models.bike.Frame;
import io.innerloop.neo4j.ogm.models.bike.Saddle;
import io.innerloop.neo4j.ogm.models.bike.SpeedFrame;
import io.innerloop.neo4j.ogm.models.bike.Wheel;
import io.innerloop.neo4j.ogm.models.cineasts.Actor;
import io.innerloop.neo4j.ogm.models.cineasts.Movie;
import io.innerloop.neo4j.ogm.models.cineasts.Role;
import io.innerloop.neo4j.ogm.models.complex.Alias;
import io.innerloop.neo4j.ogm.models.complex.Category;
import io.innerloop.neo4j.ogm.models.complex.Subject;
import io.innerloop.neo4j.ogm.models.complex.WeightedRelationship;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.helpers.CommunityServerBuilder;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by markangrish on 17/12/2014.
 */
public class EndToEndTests
{
    private static final Logger LOG = (Logger)LoggerFactory.getLogger(EndToEndTests.class);

    private Neo4jClient client;

    private CommunityNeoServer server;

    @BeforeClass
    public static void oneTimeSetUp()
    {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("io.innerloop.neo4j");
        rootLogger.setLevel(Level.DEBUG);
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
    public void testFindDomainObjectReturnsNull()
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.cineasts");
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();
            // don't load anything into the database. Just look for it.
            Actor actor = session.load(Actor.class, "name", "Keanu Reeves");
            assertNull(actor);
            transaction.commit();
        }
        finally
        {
            session.close();
        }
    }

    @Test
    public void testSaveReachableDomainObjectsAndRelationships()
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.cineasts");
        Session session = sessionFactory.getCurrentSession();
        Transaction txn = session.getTransaction();
        try
        {
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
    public void testSaveDomainObjectsThenDelete()
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.cineasts");
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.getTransaction();
        Actor keanu;
        try
        {
            transaction.begin();

            keanu = new Actor("Keanu Reeves");
            Movie matrix = new Movie("Matrix", 1999);
            keanu.playedIn(matrix, "Neo");
            session.save(keanu);
            transaction.commit();
        }
        finally
        {
            session.close();
        }

        Session session2 = sessionFactory.getCurrentSession();
        Transaction transaction2 = session2.getTransaction();
        try
        {
            transaction2.begin();
            session2.delete(keanu);
            transaction2.flush();

            List<Actor> actors = session2.loadAll(Actor.class, "name", "Keanu Reeves");
            assertEquals(0, actors.size());
            List<Movie> movies = session2.loadAll(Movie.class);
            assertEquals(1, movies.size());
            List<Role> roles = session2.loadAll(Role.class);
            assertEquals(1, roles.size());
            Role role = roles.iterator().next();
            assertNull(role.getActor());
            transaction2.commit();

        }
        finally
        {
            session2.close();
        }

    }

    @Test
    public void testSaveLoadMutateThenSave()
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.cineasts");
        Session session = sessionFactory.getCurrentSession();
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
            txn2.commit();
            assertEquals(2, retrievedKeanu.getRoles().size());
        }
        finally
        {
            session.close();
        }
    }


    @Test
    public void testDirtyUpdate() throws Neo4jClientException
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.cineasts");
        Session session = sessionFactory.getCurrentSession();
        Transaction txn1 = session.getTransaction();
        try
        {
            txn1.begin();

            Actor keanu = new Actor("Keanu Reeves");
            Movie matrix = new Movie("Matrix", 1999);
            keanu.playedIn(matrix, "Neo");
            session.save(keanu);

            List<Actor> actors = session.loadAll(Actor.class, "name", "Keanu Reeves");
            assertEquals(1, actors.size());

            keanu.setName("KeanuNuNu Reeves");
            keanu.playedIn(new Movie("Bill and Ted's Excellent Adventure", 1986), "Bill");
            session.save(keanu);

            Actor fakeKeanu = session.load(Actor.class, "name", "KeanuNuNu Reeves");
            txn1.commit();

            assertEquals(2, fakeKeanu.getRoles().size());
            assertTrue(keanu == fakeKeanu);
        }
        finally
        {
            session.close();
        }
    }

    @Test
    public void canSimpleQueryDatabase()
    {

        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();
            Saddle expected = new Saddle();
            expected.setPrice(29.95);
            expected.setMaterial("Leather");
            Wheel frontWheel = new Wheel();
            Wheel backWheel = new Wheel();
            Bike bike = new Bike();
            SpeedFrame frame = new SpeedFrame("Carbon Composite", 2.2);
            frame.addGearRatio("1");
            frame.addGearRatio("2");
            frame.addGearRatio("3");
            frame.addGearRatio("4");
            Set<Bike.Logo> logos =  new HashSet<>();
            logos.add(Bike.Logo.LOGO_1);
            logos.add(Bike.Logo.LOGO_2);
            bike.setBrand("Huffy");
            bike.setWheels(Arrays.asList(frontWheel, backWheel));
            bike.setSaddle(expected);
            bike.setFrame(frame);
            bike.setLogos(logos);
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
                                                  "MATCH (bike:Bike{brand: {brand}})-[r]-() RETURN bike, r",
                                                  parameters2);
            assertEquals(bike.getUuid(), actual2.getUuid());
            assertEquals(bike.getBrand(), actual2.getBrand());
            assertEquals(bike.getSaddle().getUuid(), actual2.getSaddle().getUuid());
            assertEquals(bike.getFrame().getUuid(), actual2.getFrame().getUuid());

            if (!(actual2.getFrame() instanceof SpeedFrame))
            {
                fail("Frame was expected to be instance of SpeedFrame");
            }
            SpeedFrame sf = (SpeedFrame)actual2.getFrame();
            assertEquals(frame.getGearRatios().size(),sf.getGearRatios().size());
            assertEquals(bike.getWheels().size(), actual2.getWheels().size());
            transaction.commit();
        }
        finally
        {
            session.close();
        }
    }


    @Test
    public void canSimpleScalarQueryDatabase()
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();

            Saddle expected = new Saddle();
            expected.setPrice(29.95);
            expected.setMaterial("Leather");
            session.save(expected);

            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("material", expected.getMaterial());
            Integer actual = session.queryForObject(Integer.class,
                                                "MATCH (saddle:Saddle{material:{material}}) RETURN COUNT(saddle)",
                                                parameters);
            transaction.commit();
            assertEquals(1, actual.intValue());
        }
        finally
        {
            session.close();
        }
    }

    @Test
    public void canComplexQueryDatabase()
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
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
    public void canComplexExecute()
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();
            Saddle saddle = new Saddle();
            saddle.setPrice(29.95);
            saddle.setMaterial("Leather");
            Wheel frontWheel = new Wheel();
            Wheel backWheel = new Wheel();
            Bike bike = new Bike();
            List<String> colours = new ArrayList<>();
            colours.add("blue");
            colours.add("black");
            bike.setColours(colours);
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
            transaction.commit();

            assertEquals(bike.getUuid(), actual.getUuid());
            assertEquals(bike.getBrand(), actual.getBrand());
            assertEquals(bike.getColours(), actual.getColours());
            assertEquals(bike.getWheels().size(), actual.getWheels().size());
            assertEquals("Vinyl", actual.getSaddle().getMaterial());
        }
        finally
        {
            session.close();
        }
    }

    @Test
    public void testSaveDomainObjectsThenUpdate()
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();
            Saddle expected = new Saddle();
            expected.setPrice(29.95);
            expected.setMaterial("Leather");
            Wheel frontWheel = new Wheel();
            Wheel backWheel = new Wheel();
            Bike bike = new Bike();
            SpeedFrame frame = new SpeedFrame("Carbon Composite", 2.2);
            frame.addGearRatio("1");
            frame.addGearRatio("2");
            frame.addGearRatio("3");
            frame.addGearRatio("4");
            Set<Bike.Logo> logos =  new HashSet<>();
            logos.add(Bike.Logo.LOGO_1);
            logos.add(Bike.Logo.LOGO_2);
            bike.setBrand("Huffy");
            bike.setWheels(Arrays.asList(frontWheel, backWheel));
            bike.setSaddle(expected);
            bike.setFrame(frame);
            bike.setLogos(logos);
            session.save(bike);

            session.save(bike);
            transaction.commit();
        }
        finally
        {
            session.close();
        }

        Session session2 = sessionFactory.getCurrentSession();
        Transaction transaction2 = session2.getTransaction();
        try
        {
            transaction2.begin();
            Bike bike = session2.load(Bike.class,"brand", "Huffy");
            Saddle newSaddle = new Saddle();
            newSaddle.setPrice(19.95);
            newSaddle.setMaterial("Vinyl");
            bike.setSaddle(newSaddle);
            ((SpeedFrame)bike.getFrame()).addGearRatio("5");
            Set<Bike.Logo> logos =  new HashSet<>();
            logos.add(Bike.Logo.LOGO_1);
            logos.add(Bike.Logo.LOGO_2);
            logos.add(Bike.Logo.LOGO_3);
            logos.add(Bike.Logo.LOGO_4);
            bike.setLogos(logos);
            session2.save(bike);
            transaction2.commit();
        }
        finally
        {
            session2.close();
        }

        Session session3 = sessionFactory.getCurrentSession();
        Transaction transaction3 = session3.getTransaction();
        try
        {
            transaction3.begin();
            Bike bike = session3.load(Bike.class,"brand", "Huffy");
            assertEquals(bike.getLogos().size(), 4);
            assertEquals(((SpeedFrame)bike.getFrame()).getGearRatios().size(), 5);
            transaction3.commit();
        }
        finally
        {
            session3.close();
        }

    }


    @Test
    public void canSaveNewObjectTreeToDatabase() throws Neo4jClientException
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();

            Wheel frontWheel = new Wheel();
            Wheel backWheel = new Wheel();
            Bike bike = new Bike();

            bike.setFrame(new Frame());
            bike.setSaddle(new Saddle());
            bike.setWheels(Arrays.asList(frontWheel, backWheel));

            assertNull(frontWheel.id);
            assertNull(backWheel.id);
            assertNull(bike.id);
            assertNull(bike.getFrame().id);
            assertNull(bike.getSaddle().id);

            session.save(bike);
            session.flush();

            assertNotNull(frontWheel.id);
            assertNotNull(backWheel.id);
            assertNotNull(bike.id);
            assertNotNull(bike.getFrame().id);
            assertNotNull(bike.getSaddle().id);
            transaction.commit();
        }
        finally
        {
            session.close();
        }
    }

    @Test
    public void canSupportWeightedRelationships() throws Neo4jClientException
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.complex");
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();
            Category c1 = new Category("Languages");
            Category c2 = new Category("Frameworks");
            Category c3 = new Category("APIs");
            Category c4 = new Category("Libraries");
            Category c5 = new Category("Platforms");
            Category c6 = new Category("Concepts");
            Category c7 = new Category("Storage");
            Category c8 = new Category("Paradigms");
            Category c9 = new Category("Practices");
            Category c10 = new Category("Tools");
            session.save(c1);
            session.save(c2);
            session.save(c3);
            session.save(c4);
            session.save(c5);
            session.save(c6);
            session.save(c7);
            session.save(c8);
            session.save(c9);
            session.save(c10);

            Subject s3 = new Subject("Object Oriented Programming");
            s3.addAlias(new Alias("OOP"));
            s3.addCategory(c6);



            Subject s1 = new Subject("Java Programming Language");
            s1.addAlias(new Alias("Java"));
            s1.addAlias(new Alias("J2SE"));
            s1.addAlias(new Alias("JavaSE"));
            s1.addCategory(c1);
            s1.requires(s3, 0.9);

            Subject s2 = new Subject("C# Programming Language");
            s2.addAlias(new Alias("C#"));
            s2.addAlias(new Alias("C Sharp"));
            s2.addCategory(c1);
            s2.requires(s3, 0.9);

            session.save(s1);
            session.save(s2);
            session.save(s3);

            transaction.commit();
        }
        finally
        {
            session.close();
        }

        Session session3 = sessionFactory.getCurrentSession();
        Transaction transaction3 = session3.getTransaction();
        try
        {
            transaction3.begin();
            Category c6 = session3.load(Category.class, "name", "Concepts");
            Subject java = session3.load(Subject.class, "name", "Java Programming Language");
            assertNotNull(java);

            Subject s4 = new Subject("Functional Programming");
            s4.addCategory(c6);
            session3.save(s4);
            java.requires(s4, 0.3);

            transaction3.commit();
        }
        finally
        {
            session3.close();
        }

        Session session2 = sessionFactory.getCurrentSession();
        Transaction transaction2 = session2.getTransaction();
        try
        {
            transaction2.begin();
            Subject java = session2.load(Subject.class, "name", "Java Programming Language");
            assertNotNull(java);
            assertEquals(2, java.getRequiredKnowledge().size());
            assertTrue(Double.compare(0.9,
                                      java.getWeightedRequiredKnowledge().values().iterator().next().getWeight()) == 0);
            assertEquals(java.getAliases().size(), 3);
            assertEquals(java.getCategories().size(), 1);

            transaction2.commit();
        }
        finally
        {
            session2.close();
        }
    }
}

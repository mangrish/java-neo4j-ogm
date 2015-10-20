package io.innerloop.neo4j.ogm;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by markangrish on 17/12/2014.
 */
public class EndToEndTests
{
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(EndToEndTests.class);

    private static final int DEFAULT_NEO_PORT = 7575;

    private Neo4jClient client;

    private int neoServerPort = -1;

    private GraphDatabaseService database;

    private WrappingNeoServerBootstrapper bootstrapper;


    @BeforeClass
    public static void oneTimeSetUp()
    {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("io.innerloop.neo4j");
        rootLogger.setLevel(Level.DEBUG);
    }

    private static int findOpenLocalPort()
    {
        try (ServerSocket socket = new ServerSocket(0))
        {
            return socket.getLocalPort();
        }
        catch (IOException e)
        {
            System.err.println("Unable to establish local port due to IOException: " + e.getMessage() +
                               "\nDefaulting instead to use: " + DEFAULT_NEO_PORT);
            e.printStackTrace(System.err);

            return DEFAULT_NEO_PORT;
        }
    }

    @Before
    public void setUp() throws IOException, InterruptedException
    {
        database = new TestGraphDatabaseFactory().newImpermanentDatabase();
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                database.shutdown();
            }
        });
        ServerConfigurator configurator = new ServerConfigurator((GraphDatabaseAPI) database);
        int port = neoServerPort();
        configurator.configuration().addProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, port);
        configurator.configuration().addProperty("dbms.security.auth_enabled", false);
        bootstrapper = new WrappingNeoServerBootstrapper((GraphDatabaseAPI) database, configurator);
        bootstrapper.start();
        while (!bootstrapper.getServer().getDatabase().isRunning())
        {
            // It's ok to spin here.. it's not production code.
            Thread.sleep(250);
        }
        client = new Neo4jClient("http://localhost:" + port + "/db/data");
    }

    protected int neoServerPort()
    {
        if (neoServerPort < 0)
        {
            neoServerPort = findOpenLocalPort();
        }
        return neoServerPort;
    }

    @After
    public void tearDown() throws IOException, InterruptedException
    {
        bootstrapper.stop();
        database.shutdown();
        client = null;
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
            Actor keanu = new Actor(12,
                                    "Keanu Reeves",
                                    LocalDate.of(1969, 4, 14),
                                    "Somewhere, USA",
                                    "A dude",
                                    "http://url");
            Movie matrix = new Movie(75,
                                     "Matrix",
                                     "a movie",
                                     "imdb",
                                     "en",
                                     "tagline",
                                     LocalDate.of(1999, 5, 17),
                                     117,
                                     "trailer",
                                     "homepage",
                                     "studio",
                                     "imageUrl",
                                     "genre"); keanu.playedIn(matrix, "Neo");

            session.save(keanu);

            List<Actor> actors = session.loadAll(Actor.class, "name", "Keanu Reeves");
            assertEquals(1, actors.size());
            Actor a = actors.iterator().next();
            assertTrue(a.getName().equals("Keanu Reeves"));
            assertEquals(1, a.getRoles().size());

            List<Movie> movies = session.loadAll(Movie.class);
            txn.commit();

            assertEquals(1, movies.size());

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

            keanu = new Actor(12, "Keanu Reeves", LocalDate.of(1969, 4, 14), "Somewhere, USA", "A dude", "http://url");
            Movie matrix = new Movie(75,
                                     "Matrix",
                                     "a movie",
                                     "imdb",
                                     "en",
                                     "tagline",
                                     LocalDate.of(1999, 5, 17),
                                     117,
                                     "trailer",
                                     "homepage",
                                     "studio",
                                     "imageUrl",
                                     "genre");

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
            session2.flush();

            List<Actor> actors = session2.loadAll(Actor.class, "name", "Keanu Reeves");
            assertEquals(0, actors.size());
            List<Movie> movies = session2.loadAll(Movie.class);
            assertEquals(1, movies.size());
            Map<Actor, Role> roles = movies.get(0).getRoles();
            assertEquals(0, roles.size());
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

            Actor keanu = new Actor(12,
                                    "Keanu Reeves",
                                    LocalDate.of(1969, 4, 14),
                                    "Somewhere, USA",
                                    "A dude",
                                    "http://url");
            Movie matrix = new Movie(75,
                                     "Matrix",
                                     "a movie",
                                     "imdb",
                                     "en",
                                     "tagline",
                                     LocalDate.of(1999, 5, 17),
                                     117,
                                     "trailer",
                                     "homepage",
                                     "studio",
                                     "imageUrl",
                                     "genre");

            keanu.playedIn(matrix, "Neo");
            session.save(keanu);

            List<Actor> actors = session.loadAll(Actor.class, "name", "Keanu Reeves");
            assertEquals(1, actors.size());

            Actor newKeanu = actors.iterator().next();

            assertEquals(keanu, newKeanu);

            Movie billAndTeds = new Movie(102,
                                          "Bill and Teds",
                                          "a movie",
                                          "imdb",
                                          "en",
                                          "tagline",
                                          LocalDate.of(1986, 5, 17),
                                          117,
                                          "trailer",
                                          "homepage",
                                          "studio",
                                          "imageUrl",
                                          "genre");

            newKeanu.playedIn(billAndTeds, "Bill");
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

            Actor keanu = new Actor(12,
                                    "Keanu Reeves",
                                    LocalDate.of(1969, 4, 14),
                                    "Somewhere, USA",
                                    "A dude",
                                    "http://url");
            Movie matrix = new Movie(75,
                                     "Matrix",
                                     "a movie",
                                     "imdb",
                                     "en",
                                     "tagline",
                                     LocalDate.of(1999, 5, 17),
                                     117,
                                     "trailer",
                                     "homepage",
                                     "studio",
                                     "imageUrl",
                                     "genre");

            keanu.playedIn(matrix, "Neo");
            session.save(keanu);

            List<Actor> actors = session.loadAll(Actor.class, "name", "Keanu Reeves");
            assertEquals(1, actors.size());

            keanu.setName("KeanuNuNu Reeves");
            Movie billAndTeds = new Movie(102,
                                          "Bill and Teds",
                                          "a movie",
                                          "imdb",
                                          "en",
                                          "tagline",
                                          LocalDate.of(1986, 5, 17),
                                          117,
                                          "trailer",
                                          "homepage",
                                          "studio",
                                          "imageUrl",
                                          "genre"); keanu.playedIn(billAndTeds, "Bill");
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
            Set<Bike.Logo> logos = new HashSet<>();
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
            SpeedFrame sf = (SpeedFrame) actual2.getFrame();
            assertEquals(frame.getGearRatios().size(), sf.getGearRatios().size());
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

            Bike actual = session.loadById(Bike.class, bike.getUuid());
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
    public void testDeleteFromCollectionOfDomainObject()
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
            Set<Bike.Logo> logos = new HashSet<>();
            logos.add(Bike.Logo.LOGO_1);
            logos.add(Bike.Logo.LOGO_2);
            logos.add(Bike.Logo.LOGO_3);
            logos.add(Bike.Logo.LOGO_4);
            bike.setBrand("Huffy");
            bike.setWheels(Arrays.asList(frontWheel, backWheel));
            bike.setSaddle(expected);
            bike.setFrame(frame);
            bike.setLogos(logos);

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
            Map<String,Object> params = new HashMap<>();
            params.put("brand", "Huffy");
            Bike b = session2.queryForObject(Bike.class, "MATCH (b:Bike)-[r]-() WHERE b.brand = {brand} RETURN b, r", params);
            b.getLogos().clear();
            b.getLogos().add(Bike.Logo.LOGO_5);
            session2.save(b);
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
            Map<String,Object> params = new HashMap<>();
            params.put("brand", "Huffy");
            Bike b2 = session3.queryForObject(Bike.class,
                                                "MATCH (b:Bike)-[r]-() WHERE b.brand = {brand} RETURN b, r",
                                                params);
            assertEquals(b2.getLogos().size(), 1);
            transaction3.commit();
        }
        finally
        {
            session3.close();
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
            Set<Bike.Logo> logos = new HashSet<>();
            logos.add(Bike.Logo.LOGO_1);
            logos.add(Bike.Logo.LOGO_2);
            bike.setBrand("Huffy");
            bike.setWheels(Arrays.asList(frontWheel, backWheel));
            bike.setSaddle(expected);
            bike.setFrame(frame);
            bike.setLogos(logos);

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
            Map<String,Object> params = new HashMap<>();
            params.put("brand", "Huffy");
            Bike bike = session2.queryForObject(Bike.class, "MATCH (b:Bike)-[r]-() WHERE b.brand = {brand} RETURN b, r", params);
            Saddle newSaddle = new Saddle();
            newSaddle.setPrice(19.95);
            newSaddle.setMaterial("Vinyl");
            bike.setSaddle(newSaddle);
            ((SpeedFrame) bike.getFrame()).addGearRatio("5");
            Set<Bike.Logo> logos = new HashSet<>();
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
            Map<String,Object> params = new HashMap<>();
            params.put("brand", "Huffy");
            Bike bike = session3.queryForObject(Bike.class,
                                                "MATCH (b:Bike)-[r]-() WHERE b.brand = {brand} RETURN b, r",
                                                params);
            assertEquals(bike.getLogos().size(), 4);
            assertEquals(((SpeedFrame) bike.getFrame()).getGearRatios().size(), 5);
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
            Map<String, Object> params = new HashMap<>();
            params.put("name", "Java Programming Language");
            Subject java = session2.queryForObject(Subject.class, "MATCH (s:Subject)-[r]-() WHERE s.name={name} RETURN s,r", params);
            assertNotNull(java);
            assertEquals(2, java.getRequiredKnowledge().size());
            assertEquals(java.getAliases().size(), 3);
            assertEquals(java.getCategories().size(), 1);

            transaction2.commit();
        }
        finally
        {
            session2.close();
        }
    }

    @Test
    public void testMultipleThreadsInsertingCompoundStatements() throws InterruptedException
    {
        SessionFactory sessionFactory = new SessionFactory(client, "io.innerloop.neo4j.ogm.models.bike");

        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(500);

        for (int i = 1; i <= 50; i++)
        {
            service.execute(new InsertJob(latch, i, sessionFactory));
        }

        latch.await();

        //check if the nodes were inserted
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();
            Integer numberOfBikes = session.queryForObject(Integer.class,
                                                           "MATCH (n:Bike) RETURN count(n) as number_of_bikes",
                                                           new HashMap<>());
            transaction.commit();

            assertNotNull(numberOfBikes);
            assertEquals(500, numberOfBikes.intValue());

        }
        finally
        {
            session.close();
        }
    }

    private class InsertJob implements Runnable
    {
        private final CountDownLatch latch;

        private final int id;

        private final SessionFactory sessionFactory;

        public InsertJob(CountDownLatch latch, int i, SessionFactory sessionFactory)
        {
            this.latch = latch;
            this.id = i;
            this.sessionFactory = sessionFactory;
        }

        @Override
        public void run()
        {
            Session session = sessionFactory.getCurrentSession();
            Transaction transaction = session.getTransaction();
            try
            {
                transaction.begin();
                for (int i = 1; i <= 10; i++)
                {
                    Bike bike = session.load(Bike.class, "brand", "brand [" + id + i + "]");

                    if (bike == null)
                    {
                        bike = new Bike();
                    }
                    bike.setBrand("brand [" + id + i + "]");
                    session.save(bike);
                }
                transaction.commit();
            }
            finally
            {
                session.close();
            }
            for (int i = 0; i < 10; i++)
            {
                latch.countDown();
            }
        }
    }
}

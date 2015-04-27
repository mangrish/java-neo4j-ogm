java-neo4j-ogm
==============

A minimalist Java Object Graph Mapper (OGM) for Neo4J.

[![Build Status](https://travis-ci.org/inner-loop/java-neo4j-ogm.svg?branch=master)](https://travis-ci.org/inner-loop/java-neo4j-ogm)


# Features

1. Mapping of POJO's to Neo4J Nodes & Relationships with minimal use of annotations.
1. First class support for Cypher querying with automatic domain mapping of results.
1. Persistence by reachability. No need to call save() for objects already in the database!
1. Built with Rich Domains in mind!  Support for automatic retrieval of [DDD](http://en.wikipedia.org/wiki/Domain-driven_design) aggregates via the ```@Aggregate``` annotation.
1. Annotation overriding for when defaults aren't good enough.
1. Fast class scanning on startup with [Reflections](https://github.com/ronmamo/reflections).
1. Efficient statement batching thanks to [Java Neo4J Client](https://github.com/inner-loop/java-neo4j-client) Connections.
1. Out of the box support for maps and relationship properties.


# Quick Start

The Java Neo4J OGM is written for and requires:

- Java 8+
- Neo4J 2.2+ Standalone Database.

## Install from Maven

Add the following to your ```<dependencies> .. </dependencies>``` section.

```maven
<dependency>
    <groupId>io.innerloop</groupId>
    <artifactId>java-neo4j-ogm</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Install from Gradle

Add the following to your ```dependencies { .. }``` section.

```gradle
compile group: 'io.innerloop', name: 'java-neo4j-ogm', version: '0.1.0'
```

... or more simply:

```gradle
compile: 'io.innerloop:java-neo4j-ogm:0.1.0'
```

# Usage

## Initialising the SessionFactory.

To intialise the SessionFactory you must supply it with an instance 
of a [Neo4JClient](https://github.com/inner-loop/java-neo4j-client/blob/master/src/main/java/io/innerloop/neo4j/client/Neo4jClient.java)
 as well as the location of the base of the domain POJO's you would like to be mapped (or an array of locations).

Classes must:

1. Have a field of type ```Long``` called ```id```. It may have any visibility modifier. Note that this ID should not
be relied upon to be unique. It is an implementation detail exposed by Neo4J.
1. Have a field marked with the annotation ```@Id```. This field must hold a unique identifier for that Class.

To ignore classes in scanning mark them with the ```@Transient``` annotation.


It's worth noting that this OGM does not support lazy loading by default. This will probably be partially supported in the
next release.

# Examples

#Basic Example

Let's model a Twitter domain. We'll keep it simple and have Users and Tweets.

**User.java**

```java
// Notice we have no need for annotations!
public class User
{
    // This is required to be in every class.
    private Long id;

    // Each class must have exactly one @Id.
    // The next release will have a UUIDGenerator annotation that
    // will replace the need to have a converter and a generator in the constructor.
    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    // We can also specify as many indexes and constraints we we want!
    @Index(unique=true)
    private String username;

    private String passwordHash;

    private String name;
    
    // Again no annotation unless we want to model the relationship differently.
    // @Fetch will force this object to automatically load Tweets eagerly.
    private List<Tweet> posts;

    // You must supply a public empty constructor.
    public User()
    {
    }

    public User(String username, String password, String name)
    {
        this.uuid = UuidGenerator.generate(); // this is just a generator that comes bundled with the OGM.
        this.name = name;
        this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        this.posts = new ArrayList<>();
    }
    
    public String getUsername() 
    {
        return this.username;
    }
    
    public Iterable<Tweet> getPosts()
    {
        return this.posts;
    }
    
    public void post(Tweet tweet)
    {
        this.posts.add(tweet);
    }
}
```

**Tweet.java**

```java
public class Tweet
{
    // This is required to be in every class.
    private Long id;

    @Id
    @Convert(UUIDConverter.class)
    private UUID uuid;

    private String text;

    // You must supply a public empty constructor.
    public Tweet()
    {
    }

    public Tweet(String username, String password, String name)
    {
        this.uuid = UuidGenerator.generate(); // this is just a generator that comes bundled with the OGM.
        this.name = name;
        this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
    }
    
    public String getUsername() 
    {
        return this.username;
    }
}

```


```java
// Create a new Neo4j Client
Neo4jClient client = new Neo4jClient("http://localhost:7474/db/data", "neo4j", "neo4j");

// Obtain a new Session Factory. You should only do this once for a JVM instance.
SessionFactory sessionFactory = new SessionFactory(client, "com.example.domain");

// Obtain a new Session and create a new Transaction.
Session session = sessionFactory.getCurrentSession();
Transaction transaction = session.getTransaction();
try
{
    transaction.begin(); // Start the Transaction.
    
    User mark = new User("@markanrish", "password", "Mark Angrish");
    mark.post(new Tweet("Here is an awesome Tweet!");
    
    session.save(mark);
    
    transaction.flush();
    
    User twitterUser = session.load(User.class, "username", "@markangrish");
    transaction.commit();

    System.out.println("Is the twitterUser Mark? [" + twitterUser.getUsername().equals(mark.getUsername()) + "]");
    System.out.println("Does the twitterUser have 1 post? [" + twitterUser.getPosts().size() + "]");
}
finally
{
    session.close();
}

```


# Spring Support
This is a simple Java OGM for Neo4J. This OGM is designed to be used
agnostic of any other framework, library or middleware.

If you want out of the box Spring support right now check out the 
[Spring Data Neo4J project](http://docs.spring.io/spring-data/neo4j/docs/4.0.0.M1/) (of which I'm also a contributor!)

I plan on adding spring support in a later release.

## Comparison with Spring Data Neo4J.

- This OGM does not support the concept of "Relationship Entities" natively. Relationships in a Neo4J Database provide
metadata about the relationship between two nodes. Instead, relationships with properties can be modelled with defining ordering 
or grouping semantics (e.g. order a list of objects by a relationship property called 'weight' etc.).
- If you only do things "the Spring way" then this project is probably not for you!


#Feature Requests / Roadmap

Do you have a feature request? [Create a new Issue](https://github.com/inner-loop/java-neo4j-ogm/issues/new).

## Backlog

- Automatic registration of default converters
- Fix Session/Transaction behaviour.
- Add @Target annotation which will allow DDD style loading against the target's @Id in the referencing class.
- Add performance tests against SDN 4.x
- Introduce statement caching and 2nd Level Session Caching.
- Add support to weight Lists by a relationship property.
- Switch reflective code to Javassist. Remove required ```Long id``` field from classes.
- Add Spring @Transactional Support
- Add Guice/Transaction support.

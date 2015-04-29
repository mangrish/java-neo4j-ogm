java-neo4j-ogm
==============

A minimalist Java Object Graph Mapper (OGM) for Neo4J.

[![Build Status](https://travis-ci.org/inner-loop/java-neo4j-ogm.svg?branch=master)](https://travis-ci.org/inner-loop/java-neo4j-ogm)

java-neo4j-ogm is a server first, cypher centric Java mapper for Neo4J. 


# Features
1. Mapping of POJO's to Neo4J Nodes & Relationships with minimal use of annotations.
1. First class support for Cypher querying with automatic domain mapping of results.
1. Persistence by reachability. No need to call save() for objects already in the database!
1. Built with Rich Domains in mind!  Support for automatic retrieval of [DDD](http://en.wikipedia.org/wiki/Domain-driven_design) aggregates via the ```@Aggregate``` annotation.
1. Annotation overriding for when defaults aren't good enough.
1. Out of the box support for maps and relationship properties.
1. Designed for performance:
    1. Fast class scanning on startup with [Reflections](https://github.com/ronmamo/reflections).
    1. Efficient unit of work statement batching thanks to [Java Neo4J Client](https://github.com/inner-loop/java-neo4j-client) Connections.
    1. Non uniform depth retrieval. Only load the nodes and relationships you need.


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

```java
Neo4jClient client = new Neo4jClient("http://localhost:7474/db/data", "neo4j", "neo4j");
SessionFactory sessionFactory = new SessionFactory(client, "com.example.domain");
```

You can also supply several packages to the session factory:

```java
SessionFactory sessionFactory = new SessionFactory(client, "com.example.domain.subdomain1", "com.example.domain.subdomain2", "com.example.domain.subdomain3");
```

## Conventions

There are a lot of conventions used by the OGM. These are the most important:

1. Managed classes must have a field of type ```Long``` called ```id```. It may have any visibility modifier. Note that this ID should not be relied upon to be unique. It is an implementation detail exposed by Neo4J.
1. Managed classes need no annotations to become managed. As long as they are in the packages defined when constructing the ```SessionFactory``` they will be managed.
1. Managed classes must have one and only one field annotated with ```@Id```.
1. Relationships don't need to be annotated unless you want to change the relationship name or want to consider directionality.
1. ```Map```s may be used at the field level. To use a map simply define the relationship type in the first parameter and a class annotated with ```@RelationshipProperties``` as the second parameter.
1. To ignore classes in scanning mark them with the ```@Transient``` annotation.
1. To ignore fields simply use the ```transient``` java keyword.

### Annotations
Below is a handy reference of all the annotations used in the OGM.

| Annotation Name | Arguments | May appear on | Description |
| :--- | :--- | :--- | :--- |
| ```@Aggregate``` | None | Class | Indicates this class is an aggregate of whatever class holds a reference to it. |
| ```@Convert``` | **value:** Name of Converter class. | Field | Will apply the specified converter to this field. Converter must implement Converter.java |
| ```@Fetch``` | None | Field | Will eagerly fetch the specified object/collection of objects managed by the OGM |
| ```@Id``` | None | Field | This field must hold a unique identifier for that Class. |
| ```@Indexed``` | **unique:** True if this index should be a constraint and not allow duplicates. | Field | This field will be registered as an index in the database.  |
| ```@Relationship``` | **type:** A unique name that represents this relationship. Will default to the name of the field. **direction:** The Direction of the relationship. Defaults to Undirected. | Field | This field will register the current object and the target as a relationship with the specified properties  |
| ```@RelationshipProperties``` | None | Class | Indicates that this class should be used to define the properties of a relationship. Typically used with Maps. |
| ```@Target```(IN DEVELOPMENT) | **value:** Name of Target class (Must be managed by OGM). | Field | Field must match the @Id type of the Target class. This will keep the rich relationship but not eagerly load the entity. Field can also be a collection of @Id's.|
| ```@Transient``` | None | Class | Indicates that this class should be skipped by the OGM. |

You might notice there is no @Entity or @Property. Typically these are done to overwrite names. Right now there is no real need for these annotations. They may make an appearance in future though!

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
    
    // Save the object. At this point there will be no communication with the DB.
    session.save(mark);
    
    // Flush() force the session to apply all statements held to the database.
    transaction.flush();

    User twitterUser = session.load(User.class, "username", "@markangrish");
    
    // Commit() will not only flush() the transaction but finalise and commit it to make all actions in the transaction
    // permanent in the database.
    transaction.commit();

    System.out.println("Is the twitterUser Mark? [" + twitterUser.getUsername().equals(mark.getUsername()) + "]");
    System.out.println("Does the twitterUser have 1 post? [" + twitterUser.getPosts().size() + "]");
}
finally
{
    // this must be called to ensure session consistency.
    session.close();
}

```


# Spring/Guice Support
This is a simple Java OGM for Neo4J. This OGM is designed to be used
agnostic of any other framework, library or middleware.

If you want out of the box Spring support right now check out the 
[Spring Data Neo4J project](http://docs.spring.io/spring-data/neo4j/docs/4.0.0.M1/) (of which I'm also a contributor!)

I will be adding Guice support very soon (probably in another repo). Spring will follow after that.

#Feature Requests / Roadmap

Do you have a feature request? [Create a new Issue](https://github.com/inner-loop/java-neo4j-ogm/issues/new).

## Upcoming features and fixes

- Automatic registration of default converters
- Fix Session/Transaction behaviour.
- Add @Target annotation which will allow DDD style loading against the target's @Id in the referencing class.
- Add performance tests against SDN 4.x
- Introduce statement caching and 2nd Level Session Caching.
- Add support to weight Lists by a relationship property.
- Switch reflective code to Javassist. Remove required ```Long id``` field from classes.
- Add Spring @Transactional Support
- Add Guice/Transaction support.

java-neo4j-ogm
==============
***NOTE: This project is no longer actively developed supported however, I am an active developer now on the [the official Neo4j OGM](https://github.com/neo4j/neo4j-ogm) and recommend those looking for a Java OGM use that.***

A minimalist Java Object Graph Mapper (OGM) for Neo4J.

[![Build Status](https://travis-ci.org/inner-loop/java-neo4j-ogm.svg?branch=master)](https://travis-ci.org/inner-loop/java-neo4j-ogm)

java-neo4j-ogm is a server first, cypher centric Java mapper for Neo4J. 


It's highly recommended to use this library with [Spring Neo4J OGM](https://github.com/inner-loop/spring-neo4j-ogm) to make use of
the @Transactional annotation.

# Features
1. Mapping of POJO's to Neo4J Nodes & Relationships with minimal use of annotations.
1. First class support for Cypher querying with automatic domain mapping of results.
1. Persistence by reachability. No need to call save() for objects already in the database!
1. Annotation overriding for when defaults aren't good enough.
1. Out of the box support for maps and relationship properties.
1. Designed for performance:
    1. Fast class scanning on startup with [Reflections](https://github.com/ronmamo/reflections).
    1. Efficient unit of work statement batching thanks to [Java Neo4J Client](https://github.com/inner-loop/java-neo4j-client) Connections.


# Quick Start

The Java Neo4J OGM is written for and requires:

- Java 8+
- Neo4J 2.3+ Standalone Database.

## Install from Maven

Add the following to your ```<dependencies> .. </dependencies>``` section.

```maven
<dependency>
    <groupId>io.innerloop</groupId>
    <artifactId>java-neo4j-ogm</artifactId>
    <version>0.3.0</version>
</dependency>
```

## Install from Gradle

Add the following to your ```dependencies { .. }``` section.

```gradle
compile group: 'io.innerloop', name: 'java-neo4j-ogm', version: '0.3.0'
```

... or more simply:

```gradle
compile: 'io.innerloop:java-neo4j-ogm:0.3.0'
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

1. Managed classes must have a field of type ```Long``` called ```id```. It may have any visibility modifier. Note that this ID should not be relied upon to be unique. It is an implementation detail exposed by Neo4J. This will probably be obfuscated in a future release.
1. Managed classes need no annotations to become managed. As long as they are in the packages defined when constructing the ```SessionFactory``` they will be managed.
1. Managed classes must have one and only one field annotated with ```@Id```. Fields of type java.util.UUID are automatically handled.
1. Relationships don't need to be annotated unless you want to change the relationship name or want to consider directionality.
1. ```Map```s may be used at the field level. To use a map simply define the relationship type in the first parameter and a class annotated with ```@RelationshipProperties``` as the second parameter. See the Subject test for an example.
1. To ignore classes in scanning mark them with the ```@Transient``` annotation.
1. To ignore fields simply use the ```transient``` java keyword.

### Annotations
Below is a handy reference of all the annotations used in the OGM.

| Annotation Name | Arguments | May appear on | Description |
| :--- | :--- | :--- | :--- |
| ```@Convert``` | **value:** Name of Converter class. | Field | Will apply the specified converter to this field. Converter must implement Converter.java |
| ```@Id``` | None | Field | This field must hold a unique identifier for that Class. |
| ```@Indexed``` | **unique:** True if this index should be a constraint and not allow duplicates. | Field | This field will be registered as an index in the database.  |
| ```@Relationship``` | **type:** A unique name that represents this relationship. Will default to the name of the field. **direction:** The Direction of the relationship. Defaults to Undirected. | Field | This field will register the current object and the target as a relationship with the specified properties  |
| ```@RelationshipProperties``` | None | Class | Indicates that this class should be used to define the properties of a relationship. Typically used with Maps. |
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
    @Id
    private UUID uuid;

    // We can also specify as many indexes and constraints we we want!
    @Index(unique=true)
    private String username;

    private String passwordHash;

    private String name;
    
    // Again no annotation unless we want to model the relationship differently.
    private List<Tweet> posts;

    // You must supply a public empty constructor.
    public User()
    {
    }

    public User(String username, String password, String name)
    {
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
    private UUID uuid;

    private String text;

    // You must supply a public empty constructor.
    public Tweet()
    {
    }

    public Tweet(String username, String password, String name)
    {
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

By default calling ```session.load()```, ```session.loadAll()```, or ```session.loadById()``` will only result in a retrieval depth of 0.
That is only that object or type of object is loaded. Symmetric loading (where depth is specified) is not supported. Instead this library
prefers non trivial loading to be done with ```session.query()``` or ```session.queryForObject()```. A DSL for basic querying is
currently being developed and will be released in a future version. If you would like to provide som input on the DSL API please raise an issue.


# Spring Support
This is a simple Java OGM for Neo4J. This OGM is designed to be used
agnostic of any other framework, library or middleware.

I have created a separate spring module which adds @Transactional support etc. for spring project.
See the documentation for that module [here](https://github.com/inner-loop/spring-neo4j-ogm): 

#Feature Requests / Roadmap

Do you have a feature request? [Create a new Issue](https://github.com/inner-loop/java-neo4j-ogm/issues/new).

## Known issues
- Delete element in a collection. When an element is deleted in a collection the save is not propagated properly.

## Roadmap

- Automatic registration of default converters
- Add performance tests against SDN 4.x
- Introduce statement caching and 2nd Level Session Caching.
- Introduce a DSL for basic querying.

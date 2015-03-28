java-neo4j-ogm
==============

A Java Object Graph Mapper (OGM) for standalone Neo4J instances.

This library does not support embedded Neo4J instances.

# Features

1. Mapping of POJO's to Neo4J Nodes without the use of annotations.
1. First class support for Cypher querying with automatic domain mapping of results.
1. Persistence by reachability. No need to call save() for objects already in the database!
1. Support for automatic retrieval of aggregates via the @Aggregate annotation.
1. Annotation overriding for when defaults aren't good enough.
1. Fast class scanning on startup with Google Reflections.
1. Efficient statement batching thanks to [Java Neo4J Client](https://github.com/inner-loop/java-neo4j-client) Connections.
1. Simple mechanism to add database field conversion.
1. Support for collection ordering via relationship properties.

## Installation

The OGM is written for and requires Java 8+ and Neo4J 2.2 Standalone Database.

To install from Maven:

```maven
<dependency>
    <groupId>io.innerloop</groupId>
    <artifactId>java-neo4j-ogm</artifactId>
    <version>0.1.0</version>
</dependency>
```

To install from Gradle:

```gradle
compile group: 'io.innerloop', name: 'java-neo4j-ogm', version: '0.1.0'
```

# Usage

**Note this section is still a work in progress.**

This OGM works on a few conventions in order to keep it light weight. 

To intialise the SessionFactory you must supply it with an instance 
of a [Neo4JClient](https://github.com/inner-loop/java-neo4j-client/blob/master/src/main/java/io/innerloop/neo4j/client/Neo4jClient.java)
 as well as the location of the base of the domain POJO's you would like to be mapped (or an array of locations).

Classes must:

1. Have a field of type ```Long``` called ```id```. It may have any visibility modifier. Note that this ID should not
be relied upon to be unique. It is an implementation detail exposed by Neo4J.
1. Have a field marked with the annotation ```@Id```. This field must hold a unique identifier for that Class.

To ignore classes in scanning mark them with the ```@Transient``` annotation.


# Examples

Coming soon.

# Spring Support
This is a simple Java OGM for Neo4J. This OGM is designed to be used
agnostic of any other framework, library or middleware.

If you want out of the box Spring support right now check out the 
[Spring Data Neo4J project](http://docs.spring.io/spring-data/neo4j/docs/4.0.0.M1/) (of which I'm also a contributor!)

I plan on adding spring support in a later release.

## Differences between Spring Data Neo4J

- This OGM does not support the concept of "Relationship Entities" natively. Relationships in a Neo4J Database provide
metadata about the relationship between two nodes. Instead, relationships with properties can be modelled with defining ordering 
or grouping semantics (e.g. order a list of objects by a relationship property called 'weight' etc.).
- If you only do things "the Spring way" then this project is probably not for you!

#Roadmap

## 0.1.x
- Fix delete semantics
- ~~Add indexing support~~

## 0.2.x
- ~~Fix Transaction behaviour (client)~~
- ~~Add support for indexing and constraints~~
- Look at a way to add @Aggregate support

## 0.3.x
- Remove ```Long id``` implementation detail requirement from classes with javassist.

##0.4.x
- Add list ordering by relationship weight property.
- Add map support by relationship property.
- Add Spring @Transactional/PlatformTransactionManager support
- Add Guice Transaction support

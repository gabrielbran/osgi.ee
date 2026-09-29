Remark: With the release of the [OSGi enterprise edition R7][1], OSGi-CDI integration is part of the specifications (chapter 152). The code here is before this release and should be handled as such. This means that it does not follow the standard although some ideas and solutions are similar.

#### Jakarta EE 11 / GlassFish 8.0.4
Version 2.0.0 of the bundles targets Jakarta EE 11 (the `jakarta.*` namespace) and Java 21. It is built and tested
against the OSGi runtime embedded in [GlassFish 8.0.4][6]:
* CDI 4.1, using the Weld 6.0 bundle of GlassFish.
* Jakarta Persistence 3.2, using EclipseLink 5.0 of GlassFish.
* Jakarta Transactions 2.0.
* Jakarta Faces 4.1 (optional integration in the CDI extender).

| Module | Status |
|---|---|
| `osgi.ee.extender.jpa` | Migrated |
| `osgi.ee.extender.cdi` | Migrated |
| `eclipselink.extender` | Migrated |
| `datasource.factory` | Migrated (Commons DBCP 2) |
| `osgi.ee.sample` | New: sample bundle using the JPA and CDI extenders |
| `glassfish` | New: collects the bundles to deploy into GlassFish |
| `osgi.ee.extender.web` | Not migrated: GlassFish 8 provides no OSGi HTTP service |
| `hibernate.extender` | Not migrated: `hibernate-osgi` does not exist anymore since Hibernate 6 |
| `slf4j-jul-bridge` | Not migrated: GlassFish itself logs via java.util.logging |

Build with `mvn clean install` (Java 21) and see [glassfish/README.md](glassfish/README.md) for deploying and
configuring the bundles in GlassFish. The modules that are not migrated still use `javax.*` and are not part of
the Maven build. The user manual ([jee-extender.odt](jee-extender.odt)) describes version 1 and therefore
still refers to the `javax.*` packages.

#### OSGi JEE7 extender 
Getting JEE7 to work in OSGi can be a real pain: there are a lot of (open source) projects going on to implement more or less the 
[OSGi R5/R6 enterprise standard][1], but to integrate them is problematic. Not the least because of the dependencies that
are required for some projects and mismatches in implementation versions.

The repository here is meant as a one-stop solution for the following JEE7 standards on OSGi:
* CDI 1.2. Implemented using the [Weld][2] CDI reference implementation, that can be downloaded [here][3].
* JPA 2.1. Implemented using the standard JPA 2.1 interfaces only and tested with Eclipselink (although it works with Hibernate as well).
* JTA 1.1. A transaction manager that can be used in local solutions in case of the absence of a genuine transaction manager.
* JSF 2.2. The standard JSF solutions are OSGi aware, but this extender helps in integrating CDI with JSF. For this a 
standard OSGi web extender is needed with some additional functionality for finding Faces configuration files and tag libraries. This is functionality provided by the web extender present in this project although also alternatives can be used, like [PAX-WEB][4].

Documentation about the usage of this extender can be found [here][5].

The extenders are split over multiple bundles, so you are able to use either JPA/JTA only, CDI only, CDI + JSF or the whole package together.

##### How to set-up?
The set-up below applies to version 1 (JEE7, Java 8). For version 2, import the projects as Maven projects instead.
To set-up the environment, perform the following actions:
* Download an eclipse version with plugin development enabled, for example the JEE version.
* Import all the projects from this repository.
* Open the file Runtime/Target.target in eclipse.
* And set it as target environment.

Code is written using Java 8 lambdas and streams, so you need Java 8.

[1]: http://www.osgi.org/Specifications/HomePage
[2]: http://weld.cdi-spec.org/
[3]: http://search.maven.org/#artifactdetails|org.jboss.weld|weld-osgi-bundle|2.2.9.Final|jar
[4]: https://ops4j1.jira.com/wiki/display/paxweb/Pax+Web
[5]: http://www.avineas.org/uploads/jee-extender.pdf
[6]: https://glassfish.org/

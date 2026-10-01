#### Running the extenders on GlassFish 8.0.4

GlassFish 8 runs its modules on an embedded Apache Felix OSGi framework (Felix 7.0.5). The extenders
of this repository are installed as plain OSGi bundles into that framework and use the Jakarta EE 11
bundles that GlassFish itself ships:

| API / implementation | Version in GlassFish 8.0.4 | Used by |
|---|---|---|
| Jakarta Persistence | 3.2.0 | JPA extender, EclipseLink extender |
| Jakarta Transactions | 2.0.1 | JPA extender, EclipseLink extender, data source factory |
| Jakarta CDI | 4.1.0 | CDI extender |
| Weld | 6.0.4.Final | Not used: the CDI extender embeds its own copy of Weld 6.0.4 |
| EclipseLink | 5.0.1 | EclipseLink extender |
| Jakarta Faces (Mojarra) | 4.1.12 | CDI extender (optional) |
| Jakarta Servlet | 6.1.0 | CDI extender, JPA extender (transaction filter) |
| Felix Config Admin, File Install | bundled (`modules/autostart`) | configuration, deployment |

GlassFish 8 does not contain a declarative services runtime (SCR). Apache Felix SCR is therefore
deployed together with the extenders.

The CDI extender embeds its own copy of Weld instead of using the Weld bundle of GlassFish. Weld keeps
its containers in static state (`SingletonProvider`), which GlassFish initializes for its applications
(one container per application class loader). With shared Weld classes, the extender
initialized that state first, after which GlassFish could not deploy any web application anymore
("SingletonProvider is already initialized"). The GlassFish variant does not fit either: it keys the
container on the application class loader, not on the container id used per extended bundle.

##### Building

Java 21 and Maven are required:

    mvn clean install

This produces `glassfish/target/bundles/` and the same content zipped as
`glassfish/target/osgi.ee-glassfish-8.0.4-2.0.0.zip`:

| Bundle | Purpose |
|---|---|
| `osgi.ee.extender.jpa` | JPA extender (`Meta-Persistence`), `EntityManager` services, JTA transaction manager and `UserTransaction` |
| `osgi.ee.extender.cdi` | CDI extender, `@Service`/`@ServiceReference`. Contains Weld 6.0.4 |
| `eclipselink.extender` | Registers EclipseLink as `PersistenceProvider` service |
| `datasource.factory` | `DataSource` services from configuration (Commons DBCP 2, embedded) |
| `org.apache.felix.scr` 2.1.30 | Declarative services runtime |
| `org.osgi.util.promise` 1.1.1, `org.osgi.util.function` 1.1.0 | Required by Felix SCR |

Felix SCR 2.1.30 and the 1.1 versions of promise/function are used on purpose: the newer versions
require the `osgi.ee` capability `JavaSE/compact1`, which GlassFish does not declare
(`org.osgi.framework.system.capabilities` in `glassfish/config/osgi.properties`), so they do not resolve.

##### Deploying

1. Copy the bundles into the `autodeploy/bundles` directory of the domain. Felix File Install picks
   them up, installs and starts them (also after a restart of the domain):

        cp glassfish/target/bundles/*.jar $GLASSFISH_HOME/glassfish/domains/domain1/autodeploy/bundles/

2. Make the JDBC driver available to the data source factory, in one of two ways:
   - deploy the driver as OSGi bundle into the same directory (for example `h2-2.5.252.jar`); the data
     source factory loads the configured driver class via a dynamic import;
   - or, as described in the user manual (section 3.3.1), attach the driver jar with a fragment bundle
     whose host is `datasource.factory`.

3. Create the data sources with configuration files in the same directory. File Install translates a
   file named `<factory pid>-<name>.cfg` into a factory configuration:
   - factory pid `datasource`: plain pooled data source, for `RESOURCE_LOCAL` units;
   - factory pid `XAdatasource`: data source that enlists its connections in the JTA transaction, for `JTA` units.

   For example `XAdatasource-orders.cfg`:

        jdbc.driver=org.h2.Driver
        jdbc.url=jdbc:h2:mem:orders;DB_CLOSE_DELAY=-1
        jdbc.user=sa
        jdbc.password=
        name=orders

   Recognised keys are `jdbc.driver`, `jdbc.url`, `jdbc.user`, `jdbc.password`, `validation.query`,
   `validation.timeout` (seconds), `pool.idle.min`, `pool.idle.max`, `pool.wait` (ms) and
   `pool.active.max`. All other keys (like `name` above) become service properties of the
   `javax.sql.DataSource` service, which the persistence unit uses to select it:

        <jta-data-source>osgi:service/javax.sql.DataSource/(name=orders)</jta-data-source>

4. Deploy the application bundles into the same directory. A persistence bundle has a
   `Meta-Persistence` header; a CDI bundle requires the extender:

        Meta-Persistence: META-INF/persistence.xml
        Require-Capability: osgi.extender;filter:="(osgi.extender=osgi.cdi)"

Check the state of the bundles with `asadmin osgi lb`. All bundles above must be `Active`.

##### Transactions

GlassFish 8 does not publish its own transaction manager in the OSGi service registry, so the
transaction manager of the JPA extender is used. Its timeout (in seconds, default 30) can be set with
a configuration file `osgi.extender.jta.tm.cfg` containing `timeout=<seconds>`. If another
`jakarta.transaction.TransactionManager` service is deployed, the built-in one can be switched off
with the JVM option `-Dosgi.extender.jta.tm=false`
(`asadmin create-jvm-options -Dosgi.extender.jta.tm=false`).

The built-in transaction manager does not write a transaction log, so it cannot recover in-doubt
XA transactions after a crash. Use it with a single database, or deploy a full transaction manager.

##### Things to know

- `persistence.xml` files of all schema versions are accepted: `http://java.sun.com/xml/ns/persistence`
  (1.0, 2.0), `http://xmlns.jcp.org/xml/ns/persistence` (2.1, 2.2) and `https://jakarta.ee/xml/ns/persistence` (3.x).
- Bytecode weaving is not supported. Set `eclipselink.weaving` to `false` or use static weaving.
- For `RESOURCE_LOCAL` units the EclipseLink extender sets `eclipselink.target-server` to `None`
  (unless the unit sets it), since EclipseLink would otherwise select the GlassFish platform, which
  is not usable from OSGi bundles.
- CDI 4 changed the meaning of an absent or empty `beans.xml` into bean discovery mode `annotated`.
  The CDI extender keeps its former behaviour: all classes of an extended bundle are beans, unless
  `beans.xml` sets a `bean-discovery-mode`.

##### Not available on GlassFish 8

- `osgi.ee.extender.web`: built on the OSGi HTTP service (compendium chapter 102, `javax.servlet`),
  which GlassFish 8 does not provide. GlassFish 8 also dropped its own web application bundle (WAB)
  support, so web applications cannot be deployed as OSGi bundles. The Faces integration of the
  CDI extender (`osgi.cdi.faces`) is therefore of no use on GlassFish 8.
- `hibernate.extender`: depends on `hibernate-osgi`, which no longer exists since Hibernate 6.
- `slf4j-jul-bridge`: GlassFish logs via java.util.logging; redirecting that to SLF4J would take
  over the logging of the server.

##### Using the services from Jakarta EE applications

Applications deployed in GlassFish (WAR, EAR) see the packages exported by the bundles installed in
its OSGi framework, including `org.osgi.framework`. An application can therefore look up the OSGi
services, like the entity managers of the JPA extender and the beans exported with `@Service`. The
`osgi.ee.sample.web` module shows how (`OsgiServices`, `ItemServlet`); see the guide in `docs` for details.
Note that the entity manager services take part in the transactions of the transaction manager of the
JPA extender, not in the JTA transactions of GlassFish: demarcate them with the `UserTransaction`
service of the extender.

##### Relation to the user manual

The [user manual](../jee-extender.odt) (v1.0.0) describes the JEE7 version. For version 2.0.0 on
GlassFish 8 the following applies per section:

| Section | On GlassFish 8 |
|---|---|
| 2 Installation | Not applicable: build with Maven and deploy as described above. The `Runtime` target platform contains the JEE7 dependencies. |
| 3 JPA | Applies, with `jakarta.persistence` instead of `javax.persistence`. The data source reference stays `osgi:service/javax.sql.DataSource/<filter>` (`javax.sql` is part of Java). |
| 3.3.1 Data source configuration | Applies. With File Install the configuration is a `.cfg` file instead of the `org.avineas.cm.persister` format. |
| 3.3.3 Pre-processing | The system property read by the code is `osgi.jpa.transform` (the manual says `osgi.jpa.transformer`); its value is the path of the XSL file: `-Dosgi.jpa.transform=/opt/persistence-transformer.xsl`. A transformation written for the `xmlns.jcp.org` namespace does not match `persistence.xml` files in the `jakarta.ee` namespace. |
| 4 JTA | Applies, with `jakarta.transaction`. The transaction manager is disabled with the system property `osgi.extender.jta.tm=false`; the `service.ranking` configuration mentioned in the manual is not implemented. The `TransactionFilter` needs a web container, see below. |
| 5 Web extender | Not available (no OSGi HTTP service). |
| 6.1 - 6.3 CDI, `@ServiceReference`, `@Service` | Apply, with the `jakarta.*` CDI packages. A `beans.xml` is still not needed. |
| 6.4 Request/session scopes, 6.5 `@ViewScoped` | Need the `ScopeListener` in a web application bundle, so not usable. `@ComponentScoped` does not depend on the web layer. |
| 7 JSF | Not available: needs an implementation of chapter 128 (web application bundles). |

##### Sample

The `osgi.ee.sample` bundle uses a `JTA` and a `RESOURCE_LOCAL` persistence unit (both in one
Jakarta Persistence 3.2 `persistence.xml`) from a CDI bean that is exported as OSGi service with
`@Service` and gets its entity managers and `UserTransaction` injected with `@ServiceReference`. To run it,
deploy the bundles above plus the H2 bundle, `osgi.ee.sample/target/osgi.ee.sample-2.0.0.jar` and these
two configuration files:

`XAdatasource-samplejta.cfg`:

    jdbc.driver=org.h2.Driver
    jdbc.url=jdbc:h2:mem:samplejta;DB_CLOSE_DELAY=-1
    jdbc.user=sa
    jdbc.password=
    name=sample-jta

`datasource-samplelocal.cfg`:

    jdbc.driver=org.h2.Driver
    jdbc.url=jdbc:h2:mem:samplelocal;DB_CLOSE_DELAY=-1
    jdbc.user=sa
    jdbc.password=
    name=sample-local

The server log then shows:

    SAMPLE JTA count after 1st insert: 1
    SAMPLE JTA count after 2nd insert: 2
    SAMPLE RESOURCE_LOCAL count after 1st insert: 1
    SAMPLE OK

#### DataSource factory
Bundle that helps in creating `javax.sql.DataSource` instances using a configuration manager only. The
data sources can be both JTA/XA data sources as well as "normal" data sources, depending on the factory to use: 
if the factory pid is set to "XAdatasource", the datasource will work with a transaction manager, if the factory
pid is "datasource" it will be a plain old data source. Usage is further described in the user manual of
this repository.

Since version 2.0.0 the pooling is done by Commons DBCP 2, which is embedded in the bundle together with its
dependencies. DBCP 2 still uses the `javax.transaction` API, so the `XAdatasource` variant adapts the
`jakarta.transaction.TransactionManager` service to it. JDBC driver classes are loaded via a dynamic import,
so the driver must be available as an OSGi bundle that exports its driver package.

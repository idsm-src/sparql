/**
 * Description of a deployment: {@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration} collects the prefixes,
 * datatypes, user IRI classes, virtual tables, quad mappings, procedures, extension functions and federated services
 * the engine translates against, and the {@link cz.iocb.sparql.engine.config.ServiceSettings} of the calls of remote
 * SERVICE endpoints. Deployments subclass it; {@link cz.iocb.sparql.engine.config.EmptyConfiguration} is the
 * configuration without any mappings.
 */
package cz.iocb.sparql.engine.config;

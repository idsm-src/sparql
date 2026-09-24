/**
 * Mapping of database tables to RDF quads. A {@link cz.iocb.sparql.engine.mapping.QuadMapping} names the table (or join
 * of tables) and gives one {@link cz.iocb.sparql.engine.mapping.TermMapping} per quad position, either a constant term
 * or a term parametrised by the columns of the table through a resource class.
 */
package cz.iocb.sparql.engine.mapping;

/**
 * Resource classes, the central abstraction of the engine: a
 * {@link cz.iocb.sparql.engine.mapping.classes.ResourceClass} defines how RDF terms of one kind are stored in SQL
 * columns and how they are converted to more general classes, up to the universal {@code sparql.rdfbox}. The package
 * holds the built-in literal classes (with a base class carrying the generic SQL type and a canonical class checking
 * the lexical form), the IRI classes including the user-configurable ones, the blank node classes, and the internal
 * classes derived from them during translation.
 */
package cz.iocb.sparql.engine.mapping.classes;

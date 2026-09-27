/**
 * Resource classes, the central abstraction of the engine: a
 * {@link cz.iocb.sparql.engine.mapping.classes.ResourceClass} defines how RDF terms of one kind are stored in SQL
 * columns and how they are converted to more general classes, up to the universal {@code sparql.rdfbox}. The package
 * holds the built-in literal classes (with a base class carrying the generic SQL type and a canonical class checking
 * the lexical form), the IRI classes including the user-configurable ones, the blank node classes, the triple term
 * classes, and the internal classes derived from them during translation. The hierarchy is sealed: a deployment can add
 * IRI classes ({@link cz.iocb.sparql.engine.mapping.classes.UserIriClass}), literal classes of its own datatypes
 * ({@link cz.iocb.sparql.engine.mapping.classes.UserLiteralClass}) and subsets of literal classes
 * ({@link cz.iocb.sparql.engine.mapping.classes.SubsetLiteralClass}), whose result classes are fixed, so that every
 * class delivers its values to the query result in one of the classes the result reader can decode.
 */
package cz.iocb.sparql.engine.mapping.classes;

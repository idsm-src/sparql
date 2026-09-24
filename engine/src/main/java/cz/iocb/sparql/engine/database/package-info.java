/**
 * Model of the relational side: the table names the generated SQL refers to
 * ({@link cz.iocb.sparql.engine.database.Table} and its kinds: database tables, virtual tables declared in a
 * {@code WITH} clause, and subquery aliases), the catalog facts about them read by
 * {@link cz.iocb.sparql.engine.database.DatabaseSchema} or stated by a
 * {@link cz.iocb.sparql.engine.database.VirtualTableDefinition}, functions and user types, and the column and condition
 * abstractions the generated SQL is composed of.
 */
package cz.iocb.sparql.engine.database;

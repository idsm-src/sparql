/**
 * Model of the relational side: the table names the generated SQL refers to
 * ({@link cz.iocb.sparql.engine.database.Table} and its kinds: database tables, virtual tables and common table
 * expressions declared in a {@code WITH} clause, subquery aliases, and the aliases nested queries refer to the
 * solutions of enclosing ones through), the catalog facts about them read by
 * {@link cz.iocb.sparql.engine.database.DatabaseSchema} or stated by a
 * {@link cz.iocb.sparql.engine.database.VirtualTableDefinition}, functions and user types, the column and condition
 * abstractions the generated SQL is composed of, and the generated SQL code itself, which keeps the tables it reads
 * that a statement has to declare and the aliases of enclosing queries its columns are addressed through
 * ({@link cz.iocb.sparql.engine.database.SqlCode}).
 */
package cz.iocb.sparql.engine.database;

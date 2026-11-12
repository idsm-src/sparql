package cz.iocb.sparql.engine.database;



/**
 * A table that is not stored in the database but declared in the {@code WITH} clause of every generated statement whose
 * SQL reads it: a {@link VirtualTable} defined by the configuration, or a {@link CommonTable} the engine defines for a
 * query a statement is to evaluate once. SQL code keeps the declared tables it reads (see {@link SqlCode}), so that the
 * statement it becomes a part of knows what to declare.
 */
public sealed interface DeclaredTable permits VirtualTable, CommonTable
{
}

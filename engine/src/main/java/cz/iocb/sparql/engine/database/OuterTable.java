package cz.iocb.sparql.engine.database;



/**
 * Alias of the solutions of a query that the SQL of queries nested in it refers to: the input of a node holding
 * expressions, which the patterns of the expressions read as their current solution (an {@link InputTable}), or the
 * left side of a lateral join, which the right side reads (a {@link LateralTable}). Unlike an {@link AliasTable}, which
 * only the query giving it refers to, such an alias is referred to across the boundaries of subqueries, so SQL code
 * referring to it has a meaning only inside the query that gives the alias. SQL code therefore keeps the outer tables
 * it refers to (see {@link SqlCode#getOuterTables}); a reference arises where a column is addressed through the alias
 * (see {@link TableColumn#fromTable}).
 */
public sealed interface OuterTable permits InputTable, LateralTable
{
}

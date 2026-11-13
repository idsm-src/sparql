package cz.iocb.sparql.engine.database;



/**
 * Alias of the input of a node holding expressions (a filter, a bind, an aggregation, a procedure call, a side of a
 * left join), through which the patterns of the expressions refer to the current solution they are evaluated for. The
 * alias is given by the scope of the node (see {@code Scope#getInputTable}): its name is followed by the number of the
 * patterns of expressions the node lies in, so a pattern can refer to it from any depth, because no subquery between
 * the reference and the node gives the alias again.
 */
public final class InputTable extends Table implements OuterTable
{
    /**
     * The number of the patterns of expressions the node giving the alias lies in.
     */
    private final int depth;


    /**
     * Creates the alias.
     *
     * @param name the name the depth follows
     * @param depth the number of the patterns of expressions the node giving the alias lies in
     */
    public InputTable(String name, int depth)
    {
        super(name + depth);

        this.depth = depth;
    }


    /**
     * The number of the patterns of expressions the node giving the alias lies in.
     *
     * @return the number of the patterns of expressions the node giving the alias lies in
     */
    public int getDepth()
    {
        return depth;
    }
}

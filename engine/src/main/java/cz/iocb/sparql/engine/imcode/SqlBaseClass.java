package cz.iocb.sparql.engine.imcode;



/**
 * Common base of intermediate code nodes: caches the hash code and provides helpers for assembling SQL and the
 * explanation tree.
 */
public abstract class SqlBaseClass
{
    /**
     * Whether the hash code has been computed.
     */
    private boolean hasHashCode = false;

    /**
     * Cached hash code.
     */
    private int hashCode;


    /**
     * Computes the structural hash code; called once and cached.
     *
     * @return the hash code
     */
    protected abstract int getHashCode();


    /**
     * Creates the node.
     */
    protected SqlBaseClass()
    {
    }


    /**
     * Appends a comma separator when {@code condition} holds.
     *
     * @param builder the builder to append to
     * @param condition whether to append the separator
     */
    protected static void appendComma(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(", ");
    }


    /**
     * Appends an {@code AND} separator when {@code condition} holds.
     *
     * @param builder the builder to append to
     * @param condition whether to append the separator
     */
    protected static void appendAnd(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(" AND ");
    }


    /**
     * Appends an {@code OR} separator when {@code condition} holds.
     *
     * @param builder the builder to append to
     * @param condition whether to append the separator
     */
    protected static void appendOr(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(" OR ");
    }


    /**
     * Appends the tree-drawing prefix of a child line of the explanation.
     *
     * @param builder the builder to append to
     * @param indent the current indentation
     * @param last whether the child is the last one
     */
    protected static void indentChild(StringBuilder builder, String indent, boolean last)
    {
        builder.append(indent);
        builder.append(last ? " └─ " : " ├─ ");
    }


    /**
     * Appends the tree-drawing prefix of an information line of the explanation.
     *
     * @param builder the builder to append to
     * @param indent the current indentation
     * @param child whether the line belongs to a child
     */
    protected static void indentInfo(StringBuilder builder, String indent, boolean child)
    {
        builder.append(indent);
        builder.append(child ? " │  " : "  ");
    }


    /**
     * Indentation for the children of a node at the given indentation.
     *
     * @param indent the current indentation
     * @param last whether the child is the last one
     * @return indentation for the children of a node at the given indentation
     */
    protected static String getIndent(String indent, boolean last)
    {
        return indent + (last ? "    " : " │  ");
    }


    @Override
    public int hashCode()
    {
        if(!hasHashCode)
        {
            hashCode = getHashCode();
            hasHashCode = true;
        }

        return hashCode;
    }
}

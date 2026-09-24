package cz.iocb.sparql.engine.common;



/**
 * Ordered pair of two values compared member-wise.
 *
 * @param <L> type of the first member
 * @param <R> type of the second member
 */
public class Pair<L, R>
{
    /**
     * First member.
     */
    protected final L left;

    /**
     * Second member.
     */
    protected final R right;


    /**
     * Creates the pair.
     *
     * @param left the first member
     * @param right the second member
     */
    public Pair(L left, R right)
    {
        this.left = left;
        this.right = right;
    }


    /**
     * First member.
     *
     * @return first member
     */
    public final L getLeft()
    {
        return left;
    }


    /**
     * Second member.
     *
     * @return second member
     */
    public final R getRight()
    {
        return right;
    }


    @Override
    public int hashCode()
    {
        return left.hashCode() ^ right.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Pair<?, ?> other = (Pair<?, ?>) object;

        return left.equals(other.left) && right.equals(other.right);
    }
}

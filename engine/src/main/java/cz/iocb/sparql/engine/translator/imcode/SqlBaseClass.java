package cz.iocb.sparql.engine.translator.imcode;

public abstract class SqlBaseClass
{
    private boolean hasHashCode = false;
    private int hashCode;


    protected abstract int getHashCode();


    protected static void appendComma(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(", ");
    }


    protected static void appendAnd(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(" AND ");
    }


    protected static void appendOr(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(" OR ");
    }


    protected static void indentChild(StringBuilder builder, String indent, boolean last)
    {
        builder.append(indent);
        builder.append(last ? " └─ " : " ├─ ");
    }


    protected static void indentInfo(StringBuilder builder, String indent, boolean child)
    {
        builder.append(indent);
        builder.append(child ? " │  " : "  ");
    }


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

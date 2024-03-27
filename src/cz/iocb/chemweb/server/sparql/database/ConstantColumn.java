package cz.iocb.chemweb.server.sparql.database;



public class ConstantColumn extends Column
{
    private final String literal;

    public ConstantColumn(String literal, String type)
    {
        super(literal == null ? "NULL" : "'" + literal.replaceAll("'", "''") + "'::" + type);

        this.literal = literal;
    }


    public ConstantColumn(int literal, String type)
    {
        this(Integer.toString(literal), type);
    }


    public ConstantColumn(short literal, String type)
    {
        this(Short.toString(literal), type);
    }


    @Override
    public String toString()
    {
        return value;
    }


    @Override
    public Column fromTable(Table table)
    {
        return this;
    }


    public String getValue()
    {
        return literal;
    }
}

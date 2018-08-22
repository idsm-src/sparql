package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import java.util.HashSet;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlNull extends SqlExpressionIntercode
{
    static private final SqlNull singleton = new SqlNull();


    private SqlNull()
    {
        super(new HashSet<ExpressionResourceClass>(), false, true);
    }


    public static SqlExpressionIntercode get()
    {
        return singleton;
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        return singleton;
    }


    @Override
    public String translate()
    {
        return "NULL";
    }
}

package cz.iocb.chemweb.server.sparql.mapping.extension;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Function;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag;
import cz.iocb.chemweb.server.sparql.mapping.classes.SimpleLiteralClass;



public class FunctionDefinition
{
    public static final ResourceClass stringLiteral = new SimpleLiteralClass(ResultTag.NULL, "string", null)
    {
    };


    private final String functionName;
    private final Function sqlFunction;
    private final ResourceClass result;
    private final List<ResourceClass> arguments;
    private final boolean canBeNull;
    private final boolean isDeterministic;


    public FunctionDefinition(String functionName, Function sqlFunction, ResourceClass result,
            List<ResourceClass> arguments, boolean canBeNull, boolean isDeterministic)
    {
        this.functionName = functionName;
        this.sqlFunction = sqlFunction;
        this.result = result;
        this.arguments = arguments;
        this.canBeNull = canBeNull;
        this.isDeterministic = isDeterministic;
    }


    public String getFunctionName()
    {
        return functionName;
    }


    public Function getSqlFunction()
    {
        return sqlFunction;
    }


    public ResourceClass getResultClass()
    {
        return result;
    }


    public List<ResourceClass> getArgumentClasses()
    {
        return arguments;
    }


    public boolean canBeNull()
    {
        return canBeNull;
    }


    public boolean isDeterministic()
    {
        return isDeterministic;
    }
}

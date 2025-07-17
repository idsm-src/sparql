package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public class SqlFunctionCall extends SqlExpressionIntercode
{
    private final FunctionDefinition definition;
    private final List<SqlExpressionIntercode> arguments;


    SqlFunctionCall(FunctionDefinition definition, List<SqlExpressionIntercode> arguments, boolean canBeNull,
            boolean isDeterministic)
    {
        super(asSet(definition.getResultClass()), canBeNull, isDeterministic);

        for(SqlExpressionIntercode argument : arguments)
            this.referencedVariables.addAll(argument.getReferencedVariables());

        this.definition = definition;
        this.arguments = arguments;
    }


    public static SqlExpressionIntercode create(FunctionDefinition definition, List<SqlExpressionIntercode> arguments)
    {
        boolean canBeNull = definition.canBeNull() || arguments.stream().anyMatch(a -> a.canBeNull());
        boolean isDeterministic = definition.isDeterministic() && arguments.stream().allMatch(a -> a.isDeterministic());

        for(int i = 0; i < arguments.size(); i++)
        {
            Set<ResourceClass> resClasses = arguments.get(i).getResourceClasses();
            ResourceClass refClass = definition.getArgumentClasses().get(i);

            if(refClass == FunctionDefinition.stringLiteral)
            {
                if(resClasses.stream().allMatch(r -> !isStringLiteral(r)))
                    return SqlNull.get();
            }
            else
            {
                if(resClasses.stream().allMatch(r -> r.getGeneralClass() != refClass.getGeneralClass()))
                    return SqlNull.get();
            }
        }

        return new SqlFunctionCall(definition, arguments, canBeNull, isDeterministic);
    }


    @Override
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        Restrictions restrictions = new Restrictions();

        for(int i = 0; i < arguments.size(); i++)
        {
            if(definition.getArgumentClasses().get(i) == FunctionDefinition.stringLiteral)
                restrictions.add(arguments.get(i).getRequirements(Set.of(xsdString, rdfLangString)));
            else
                restrictions.add(arguments.get(i).getRequirements(Set.of(definition.getArgumentClasses().get(i))));
        }

        return restrictions;
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        List<SqlExpressionIntercode> optArguments = new LinkedList<SqlExpressionIntercode>();

        for(SqlExpressionIntercode argument : arguments)
            optArguments.add(argument.optimize(request, variables, evalServices));


        if(optArguments.equals(arguments))
            return this;

        return create(definition, optArguments);
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(definition.getSqlFunction());
        builder.append("(");

        for(int i = 0; i < arguments.size(); i++)
        {
            appendComma(builder, i > 0);

            if(definition.getArgumentClasses().get(i) == FunctionDefinition.stringLiteral)
                builder.append(translateAsStringLiteral(request, arguments.get(i)));
            else
                builder.append(
                        translateAsUnboxedOperand(request, arguments.get(i), definition.getArgumentClasses().get(i)));
        }

        builder.append(")");

        return builder.toString();
    }
}

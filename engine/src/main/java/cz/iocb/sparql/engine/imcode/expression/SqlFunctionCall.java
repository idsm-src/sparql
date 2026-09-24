package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.stringLiteral;
import static java.util.Collections.singletonMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Call of an extension function defined by the configuration; an argument whose classes are disjoint with the declared
 * argument class makes the call an error.
 */
public final class SqlFunctionCall extends SqlExpressionIntercode
{
    /**
     * Called function.
     */
    private final FunctionDefinition definition;

    /**
     * Arguments in order.
     */
    private final List<SqlExpressionIntercode> arguments;


    /**
     * Creates the expression; deterministic if the function and all arguments are.
     *
     * @param definition the function definition
     * @param arguments the arguments
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    protected SqlFunctionCall(FunctionDefinition definition, List<SqlExpressionIntercode> arguments,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        boolean isDeterministic = definition.isDeterministic() && arguments.stream().allMatch(a -> a.isDeterministic());

        super(mappings, canBeNull, isDeterministic);

        for(SqlExpressionIntercode argument : arguments)
            this.referencedVariables.addAll(argument.getReferencedVariables());

        this.definition = definition;
        this.arguments = arguments;
    }


    /**
     * Call of the function with the given arguments.
     *
     * @param definition the function definition
     * @param arguments the arguments
     * @return call of the function with the given arguments
     */
    public static SqlExpressionIntercode create(FunctionDefinition definition, List<SqlExpressionIntercode> arguments)
    {
        return create(definition, arguments, Restriction.ALL);
    }


    /**
     * Call materialising the result only when needed; an argument disjoint with its declared class makes the call NULL.
     *
     * @param definition the function definition
     * @param arguments the arguments
     * @param restriction the result classes the parent needs
     * @return call materialising the result only when needed; an argument disjoint with its declared class makes the
     *         call NULL
     */
    public static SqlExpressionIntercode create(FunctionDefinition definition, List<SqlExpressionIntercode> arguments,
            Restriction restriction)
    {
        boolean canBeNull = definition.canBeNull() || arguments.stream().anyMatch(a -> a.canBeNull());

        for(int i = 0; i < arguments.size(); i++)
        {
            Set<ResourceClass> resClasses = arguments.get(i).getResourceClasses();
            ResourceClass refClass = definition.getArgumentClasses().get(i);

            if(ResourceClass.areDisjunct(refClass, resClasses))
                return SqlNull.get();
        }

        List<Column> cols = restriction.contains(definition.getResultClass()) ? translate(definition, arguments) : null;
        Map<ResourceClass, List<Column>> mappings = singletonMap(definition.getResultClass(), cols);

        return new SqlFunctionCall(definition, arguments, mappings, canBeNull);
    }


    @Override
    public Restrictions getRequirements()
    {
        Restrictions restrictions = new Restrictions();

        for(SqlExpressionIntercode argument : arguments)
            restrictions.add(argument.getRequirements());

        return restrictions;
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        List<SqlExpressionIntercode> optArguments = new ArrayList<>();

        for(int i = 0; i < arguments.size(); i++)
        {
            Restriction argRestriction = new Restriction();

            if(restriction.contains(definition.getResultClass()))
                argRestriction.add(definition.getArgumentClasses().get(i));

            optArguments.add(arguments.get(i).optimize(request, bindings, argRestriction, evalServices));
        }

        if(optArguments.equals(arguments))
            return this;

        return create(definition, optArguments, restriction);
    }


    /**
     * SQL calling the function with the arguments converted to their declared classes.
     *
     * @param definition the function definition
     * @param arguments the arguments
     * @return SQL calling the function with the arguments converted to their declared classes
     */
    private static List<Column> translate(FunctionDefinition definition, List<SqlExpressionIntercode> arguments)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(definition.getSqlFunction());
        builder.append("(");

        for(int i = 0; i < arguments.size(); i++)
        {
            appendComma(builder, i > 0);

            ResourceClass argClass = definition.getArgumentClasses().get(i);

            if(argClass.equals(stringLiteral)) //FIXME: use different approach
                builder.append(arguments.get(i).getStringLiteral());
            else
                builder.append(arguments.get(i).get(argClass).get(0)); //NOTE: only first column is used
        }

        builder.append(")");

        return List.of(new ExpressionColumn(builder.toString()));
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append(definition.getFunctionName());
        builder.append("(");

        for(int i = 0; i < arguments.size(); i++)
        {
            if(i > 0)
                builder.append(", ");

            arguments.get(i).generateExplanation(builder, indent, 10);
        }

        builder.append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlFunctionCall imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(definition, imcode.definition))
            return false;

        if(!Objects.equals(arguments, imcode.arguments))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(definition, arguments);
    }
}

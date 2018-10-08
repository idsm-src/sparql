package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlVariable extends SqlNodeValue
{
    private final UsedVariable usedVariable;
    private final VariableAccessor variableAccessor;


    protected SqlVariable(UsedVariable usedVariable, VariableAccessor variableAccessor,
            Set<ResourceClass> resourceClasses, boolean isBoxed, boolean canBeNull)
    {
        super(resourceClasses, isBoxed, canBeNull);
        this.usedVariable = usedVariable;
        this.variableAccessor = variableAccessor;
    }


    public static SqlExpressionIntercode create(String variable, VariableAccessor variableAccessor)
    {
        UsedVariable usedVariable = variableAccessor.getUsedVariable(variable);

        if(usedVariable == null)
            return SqlNull.get();

        Set<ResourceClass> classes = usedVariable.getClasses();

        boolean isBoxed = classes.size() > 1 && !classes.stream().allMatch(r -> isDateTime(r))
                && !classes.stream().allMatch(r -> isDate(r)) || classes.contains(rdfLangString)
                || classes.contains(unsupportedLiteral);

        return new SqlVariable(usedVariable, variableAccessor, classes, isBoxed, usedVariable.canBeNull());
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        return create(usedVariable.getName(), variableAccessor);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();
        boolean hasAlternative = false;

        if(getResourceClasses().size() > 1)
            builder.append("COALESCE(");

        for(ResourceClass resourceClass : usedVariable.getClasses())
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            if(resourceClass instanceof DateTimeConstantZoneClass && getResourceClasses().size() > 1 && !isBoxed())
            {
                builder.append("sparql.zoneddatetime_create(");
                builder.append(getExpressionValue(resourceClass, false));
                builder.append(", '");
                builder.append(((DateTimeConstantZoneClass) resourceClass).getZone());
                builder.append("'::int4)");
            }
            else if(resourceClass instanceof DateConstantZoneClass && getResourceClasses().size() > 1 && !isBoxed())
            {
                builder.append("sparql.zoneddate_create(");
                builder.append(getExpressionValue(resourceClass, false));
                builder.append(", '");
                builder.append(((DateConstantZoneClass) resourceClass).getZone());
                builder.append("'::int4)");
            }
            else
            {
                builder.append(resourceClass.getExpressionCode(usedVariable.getName(), variableAccessor, isBoxed()));
            }
        }

        if(getResourceClasses().size() > 1)
            builder.append(")");

        return builder.toString();
    }


    String getExpressionValue(ResourceClass resourceClass, boolean isBoxed)
    {
        return resourceClass.getExpressionCode(usedVariable.getName(), variableAccessor, isBoxed);
    }


    @Override
    public String getNodeAccess(ResourceClass resourceClass, int part)
    {
        return variableAccessor.getSqlVariableAccess(usedVariable.getName(), resourceClass, part);
    }


    public String getName()
    {
        return usedVariable.getName();
    }


    public VariableAccessor getVariableAccessor()
    {
        return variableAccessor;
    }
}

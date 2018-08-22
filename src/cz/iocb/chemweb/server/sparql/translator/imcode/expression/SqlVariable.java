package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangStringExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteralExpr;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlVariable extends SqlNodeValue
{
    private final UsedVariable usedVariable;
    private final VariableAccessor variableAccessor;


    protected SqlVariable(UsedVariable usedVariable, VariableAccessor variableAccessor,
            Set<ExpressionResourceClass> resourceClasses, boolean isBoxed, boolean canBeNull)
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

        Set<ExpressionResourceClass> cleasses = new HashSet<ExpressionResourceClass>();

        for(PatternResourceClass resourceClass : usedVariable.getClasses())
            cleasses.add(resourceClass.getExpressionResourceClass());

        boolean isBoxed = cleasses.size() > 1 || cleasses.contains(rdfLangStringExpr)
                || cleasses.contains(unsupportedLiteralExpr);

        return new SqlVariable(usedVariable, variableAccessor, cleasses, isBoxed, usedVariable.canBeNull());
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

        if(usedVariable.getClasses().size() > 1)
            builder.append("COALESCE(");

        for(PatternResourceClass resourceClass : usedVariable.getClasses())
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            builder.append(resourceClass.getSqlExpressionValue(usedVariable.getName(), variableAccessor, isBoxed()));
        }

        if(usedVariable.getClasses().size() > 1)
            builder.append(")");

        return builder.toString();
    }


    String getExpressionValue(PatternResourceClass resourceClass, boolean isBoxed)
    {
        return resourceClass.getSqlExpressionValue(usedVariable.getName(), variableAccessor, isBoxed);
    }


    @Override
    public Set<PatternResourceClass> getPatternResourceClasses()
    {
        return usedVariable.getClasses();
    }


    @Override
    public String nodeAccess(PatternResourceClass resourceClass, int part)
    {
        return variableAccessor.variableAccess(usedVariable.getName(), resourceClass, part);
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

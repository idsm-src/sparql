package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public class SqlUnaryLogical extends SqlUnary
{
    protected SqlUnaryLogical(SqlExpressionIntercode operand, Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(operand, resourceClasses, canBeNull);
    }


    public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
    {
        operand = SqlEffectiveBooleanValue.create(operand);

        if(operand == SqlNull.get())
            return SqlNull.get();

        if(operand == falseValue)
            return trueValue;

        if(operand == trueValue)
            return falseValue;

        return new SqlUnaryLogical(operand, asSet(xsdBoolean), operand.canBeNull());
    }


    @Override
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        return getOperand().getRequirements(Set.of(xsdBoolean));
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        SqlExpressionIntercode optOperand = getOperand().optimize(request, variables, evalServices);

        if(optOperand == getOperand())
            return this;

        return create(optOperand);
    }


    @Override
    public String translate(Request request)
    {
        return "(not " + getOperand().translate(request) + ")";
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append("not ");
        getOperand().generateExplanation(builder, indent, 3);
    }
}

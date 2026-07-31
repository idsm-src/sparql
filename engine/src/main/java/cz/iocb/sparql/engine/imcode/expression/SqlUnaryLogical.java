package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.ANY;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.FALSE_OR_ERROR;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.TRUE_OR_ERROR;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.Collections.singletonMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlUnaryLogical extends SqlUnary implements SqlBooleanExpression
{
    private final NonConstantBooleanValue value;


    private SqlUnaryLogical(SqlExpressionIntercode operand, Map<ResourceClass, List<Column>> mappings,
            boolean canBeNull, NonConstantBooleanValue value)
    {
        super(operand, mappings, canBeNull);

        this.value = value;
    }


    public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
    {
        return create(operand, Restriction.ALL);
    }


    public static SqlExpressionIntercode create(SqlExpressionIntercode operand, Restriction restriction)
    {
        if(operand.equals(SqlNull.get()))
            return SqlNull.get();

        if(operand.equals(falseValue))
            return trueValue;

        if(operand.equals(trueValue))
            return falseValue;

        NonConstantBooleanValue value = ANY;

        if(operand instanceof SqlBooleanExpression e && e.isFalseOrError())
            value = TRUE_OR_ERROR;

        if(operand instanceof SqlBooleanExpression e && e.isTrueOrError())
            value = FALSE_OR_ERROR;

        List<Column> columns = restriction.contains(xsdBoolean) ? translate(operand) : null;
        Map<ResourceClass, List<Column>> mappings = singletonMap(xsdBoolean, columns);

        return new SqlUnaryLogical(operand, mappings, operand.canBeNull(), value);
    }


    private static List<Column> translate(SqlExpressionIntercode operand)
    {
        return List.of(new ExpressionColumn("(not " + operand.get(xsdBoolean).get(0) + ")", operand.canBeNull()));
    }


    @Override
    public NonConstantBooleanValue getBooleanValue()
    {
        return value;
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        Restriction operandRestriction = new Restriction();

        if(restriction.contains(xsdBoolean))
            operandRestriction.add(xsdBoolean);

        SqlExpressionIntercode optOperand = operand.optimize(request, bindings, operandRestriction, evalServices);

        if(optOperand == operand)
            return this;

        return create(optOperand, restriction);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append("not ");
        operand.generateExplanation(builder, indent, 3);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlUnaryLogical imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(operand);
    }
}

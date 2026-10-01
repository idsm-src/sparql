package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.ANY;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.FALSE_OR_ERROR;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.TRUE_OR_ERROR;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.Collections.singletonMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Logical NOT of an effective boolean value.
 */
public final class SqlUnaryLogical extends SqlUnary implements SqlBooleanExpression
{
    /**
     * Values the expression can take besides an error.
     */
    private final NonConstantBooleanValue value;


    /**
     * Creates the expression.
     *
     * @param operand the operand
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     * @param value values the expression can take besides an error
     */
    private SqlUnaryLogical(SqlExpressionIntercode operand, Map<ResourceClass, List<Column>> mappings,
            boolean canBeNull, NonConstantBooleanValue value)
    {
        super(operand, mappings, canBeNull);

        this.value = value;
    }


    /**
     * Negation of a boolean operand.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operand the operand
     * @return negation of a boolean operand
     */
    public static SqlExpressionIntercode create(ClassRelations relations, SqlExpressionIntercode operand)
    {
        return create(relations, operand, Restriction.ALL);
    }


    /**
     * Negation materialising only when needed; constants are folded.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operand the operand
     * @param restriction the result classes the parent needs
     * @return negation materialising only when needed; constants are folded
     */
    public static SqlExpressionIntercode create(ClassRelations relations, SqlExpressionIntercode operand,
            Restriction restriction)
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

        List<Column> columns = restriction.contains(relations, xsdBoolean) ? translate(relations, operand) : null;
        Map<ResourceClass, List<Column>> mappings = singletonMap(xsdBoolean, columns);

        return new SqlUnaryLogical(operand, mappings, operand.canBeNull(), value);
    }


    /**
     * SQL negating the boolean value of the operand.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operand the operand
     * @return SQL negating the boolean value of the operand
     */
    private static List<Column> translate(ClassRelations relations, SqlExpressionIntercode operand)
    {
        return List.of(
                new ExpressionColumn("(not " + operand.get(relations, genBoolean).get(0) + ")", operand.canBeNull()));
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
        ClassRelations relations = request.getConfiguration();

        Restriction operandRestriction = new Restriction();

        if(restriction.contains(relations, xsdBoolean))
            operandRestriction.add(genBoolean);

        SqlExpressionIntercode optOperand = operand.optimize(request, bindings, operandRestriction, evalServices);

        if(optOperand == operand)
            return this;

        return create(relations, optOperand, restriction);
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

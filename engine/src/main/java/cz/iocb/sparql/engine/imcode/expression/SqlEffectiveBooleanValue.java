package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.ANY;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.FALSE_OR_ERROR;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloatPoint;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isUnsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.numericBaseClasses;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdByteIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNegativeIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonNegativeIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonPositiveIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdPositiveIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedLongIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortIri;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Effective boolean value of an operand (SPARQL 1.1, section 17.2.2): booleans as they are, numerics true when
 * non-zero, strings true when non-empty, invalid lexical forms false, other types an error.
 */
public final class SqlEffectiveBooleanValue extends SqlUnary
{
    /**
     * Classes that have an effective boolean value.
     */
    private static final Set<ResourceClass> operandClasses = Stream
            .concat(Stream.of(genBoolean, xsdString, unsupportedType), numericBaseClasses.stream()).collect(toSet());

    /**
     * Union of the classes that have an effective boolean value.
     */
    private static final ResourceClass operandClass = unionize(operandClasses);


    /**
     * Creates the expression.
     *
     * @param operand the operand
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     * @param value values the expression can take besides an error
     */
    private SqlEffectiveBooleanValue(SqlExpressionIntercode operand, Map<ResourceClass, List<Column>> mappings,
            boolean canBeNull, NonConstantBooleanValue value)
    {
        super(operand, mappings, canBeNull);
    }


    /**
     * Effective boolean value of the operand.
     *
     * @param operand the operand
     * @return effective boolean value of the operand
     */
    public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
    {
        return create(operand, Restriction.ALL);
    }


    /**
     * Effective boolean value materialising only when needed; constants and NULL pass through.
     *
     * @param operand the operand
     * @param restriction the result classes the parent needs
     * @return effective boolean value materialising only when needed; constants and NULL pass through
     */
    private static SqlExpressionIntercode create(SqlExpressionIntercode operand, Restriction restriction)
    {
        if(operand.equals(SqlNull.get()))
            return SqlNull.get();

        if(operand.equals(trueValue) || operand.equals(falseValue))
            return operand;

        //TODO: add compile-time evaluation for literals

        if(operand.getMappings().keySet().stream().noneMatch(r -> !areDisjunct(r, operandClass)))
            return SqlNull.get();

        Set<ResourceClass> classes = operand.getMappings().keySet();

        boolean canBeNull = operand.canBeNull() || classes.stream().anyMatch(r -> !r.isSubclassOf(operandClass));

        NonConstantBooleanValue value = classes.stream().allMatch(r -> r.isSubclassOf(unsupportedType)) ?
                FALSE_OR_ERROR : ANY;


        List<Column> columns = restriction.contains(xsdBoolean) ? translate(operand) : null;
        Map<ResourceClass, List<Column>> mappings = singletonMap(xsdBoolean, columns);

        return new SqlEffectiveBooleanValue(operand, mappings, canBeNull, value);
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        Restriction operandRestriction = new Restriction();

        if(restriction.contains(xsdBoolean))
            operandRestriction.add(operandClasses);

        SqlExpressionIntercode optOperand = operand.optimize(request, bindings, operandRestriction, evalServices);

        if(optOperand.getResourceClasses().stream().allMatch(r -> r.isSubclassOf(xsdBoolean)))
            return optOperand;

        if(optOperand == operand)
            return this;

        return create(optOperand);
    }


    /**
     * SQL computing the effective boolean value: booleans as is, numerics compared to zero, strings tested for
     * emptiness, unsupported literals false.
     *
     * @param operand the operand
     * @return SQL computing the effective boolean value: booleans as is, numerics compared to zero, strings tested for
     *         emptiness, unsupported literals false
     */
    private static List<Column> translate(SqlExpressionIntercode operand)
    {
        Set<Column> cols = new HashSet<>();

        for(Entry<ResourceClass, List<Column>> e : operand.getMappings().entrySet())
        {
            ResourceClass numericBase = numericBaseClasses.stream().filter(r -> e.getKey().isSubclassOf(r)).findFirst()
                    .orElse(null);

            if(numericBase != null && isFloatPoint(numericBase))
            {
                SqlType type = numericBase.getSqlTypes().get(0);
                Column col = e.getKey().toGeneralClass(numericBase, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(" + col + " not in ('0'::" + type + ", 'NaN'::" + type + "))"));
            }
            else if(numericBase != null)
            {
                SqlType type = numericBase.getSqlTypes().get(0);
                Column col = e.getKey().toGeneralClass(numericBase, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(" + col + " != '0'::" + type + ")"));
            }
            else if(isString(e.getKey()))
            {
                Column col = e.getKey().toGeneralClass(xsdString, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(octet_length(" + col + ") != 0)"));
            }
            else if(isBoolean(e.getKey()))
            {
                Column col = e.getKey().toGeneralClass(genBoolean, e.getValue(), true).get(0);
                cols.add(col);
            }
            else if(isUnsupportedLiteral(e.getKey()))
            {
                String types = Stream
                        .of(xsdBooleanIri, xsdByteIri, xsdUnsignedByteIri, xsdShortIri, xsdUnsignedShortIri, xsdIntIri,
                                xsdUnsignedIntIri, xsdLongIri, xsdUnsignedLongIri, xsdIntegerIri,
                                xsdNonPositiveIntegerIri, xsdNegativeIntegerIri, xsdNonNegativeIntegerIri,
                                xsdPositiveIntegerIri, xsdDecimalIri, xsdFloatIri, xsdDoubleIri)
                        .map(i -> "'" + i.getValue().replaceAll("'", "''") + "'").collect(joining(", ", "(", ")"));

                Column col = e.getKey().toGeneralClass(unsupportedType, e.getValue(), true).get(1);
                cols.add(new ExpressionColumn("NULLIF(" + col + " NOT IN " + types + ", true)"));
            }
            else if(!areDisjunct(e.getKey(), operandClass))
            {
                Column col = operand.get(unionize(operandClasses, box)).get(0);
                cols.add(new ExpressionColumn("sparql.ebv_rdfbox(" + col + ")"));
            }
        }

        return List.of(Column.coalesce(cols));
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append("evb(");
        operand.generateExplanation(builder, indent, 10);
        builder.append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlEffectiveBooleanValue imcode))
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

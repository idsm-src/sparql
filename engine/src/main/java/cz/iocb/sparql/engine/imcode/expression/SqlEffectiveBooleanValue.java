package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.ANY;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.FALSE_OR_ERROR;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isUnsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortIri;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlEffectiveBooleanValue extends SqlUnary
{
    private static final Set<ResourceClass> operandClasses = Set.of(genBoolean, genShort, genInt, genLong, genInteger,
            genDecimal, genFloat, genDouble, xsdString, unsupportedType);

    private static final ResourceClass operandClass = unionize(operandClasses);


    private SqlEffectiveBooleanValue(SqlExpressionIntercode operand, Map<ResourceClass, List<Column>> mappings,
            boolean canBeNull, NonConstantBooleanValue value)
    {
        super(operand, mappings, canBeNull);
    }


    public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
    {
        return create(operand, Restriction.ALL);
    }


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


    private static List<Column> translate(SqlExpressionIntercode operand)
    {
        Set<Column> cols = new HashSet<>();

        for(Entry<ResourceClass, List<Column>> e : operand.getMappings().entrySet())
        {
            if(isShort(e.getKey()))
            {
                Column col = e.getKey().toGeneralClass(genShort, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(" + col + " != '0'::" + genShort.getSqlTypes().get(0) + ")"));
            }
            else if(isInt(e.getKey()))
            {
                Column col = e.getKey().toGeneralClass(genInt, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(" + col + " != '0'::" + genInt.getSqlTypes().get(0) + ")"));
            }
            else if(isLong(e.getKey()))
            {
                Column col = e.getKey().toGeneralClass(genLong, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(" + col + " != '0'::" + genLong.getSqlTypes().get(0) + ")"));
            }

            else if(isInteger(e.getKey()))
            {
                Column col = e.getKey().toGeneralClass(genInteger, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(" + col + " != '0'::" + genInteger.getSqlTypes().get(0) + ")"));
            }
            else if(isDecimal(e.getKey()))
            {
                Column col = e.getKey().toGeneralClass(genDecimal, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(" + col + " != '0'::" + genDecimal.getSqlTypes().get(0) + ")"));
            }
            else if(isFloat(e.getKey()))
            {
                String type = genFloat.getSqlTypes().get(0);
                Column col = e.getKey().toGeneralClass(genFloat, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(" + col + " not in ('0'::" + type + ", 'NaN'::" + type + "))"));
            }
            else if(isDouble(e.getKey()))
            {
                String type = genDouble.getSqlTypes().get(0);
                Column col = e.getKey().toGeneralClass(genDouble, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(" + col + " not in ('0'::" + type + ", 'NaN'::" + type + "))"));
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
                        .of(xsdBooleanIri, xsdShortIri, xsdIntIri, xsdLongIri, xsdIntegerIri, xsdDecimalIri,
                                xsdFloatIri, xsdDoubleIri)
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

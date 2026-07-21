package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.getNumericClasses;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getUnionClass;
import static cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator.Divide;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;



public final class SqlBinaryArithmetic extends SqlBinary
{
    private final Operator operator;


    private SqlBinaryArithmetic(Operator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(left, right, mappings, canBeNull);

        this.operator = operator;
    }


    public static SqlExpressionIntercode create(Operator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        return create(operator, left, right, Restriction.ALL);
    }


    private static SqlExpressionIntercode create(Operator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right, Restriction restriction)
    {
        Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<ResourceClass, Set<List<ResourceClass>>>();

        boolean canBeNull = left.canBeNull() || right.canBeNull();

        for(ResourceClass le : left.getUsedVariable().getMappings().keySet())
        {
            for(ResourceClass re : right.getUsedVariable().getMappings().keySet())
            {
                canBeNull |= !isNumeric(le) || !isNumeric(re);

                Set<ResourceClass> resultClasses = new HashSet<ResourceClass>();

                for(ResourceClass l : getNumericClasses(le))
                {
                    for(ResourceClass r : getNumericClasses(re))
                    {
                        ResourceClass res = determineResultClass(l, r);

                        if(operator == Divide && res.equals(xsdInteger))
                            res = xsdDecimal;

                        if(operator == Divide && res.equals(xsdDecimal) && canBeDecimalZero(right))
                            canBeNull = true;

                        resultClasses.add(res);
                    }
                }

                if(resultClasses.size() == 1)
                    map.computeIfAbsent(resultClasses.iterator().next(), _ -> new HashSet<List<ResourceClass>>())
                            .add(List.of(le, re));
                else if(!resultClasses.isEmpty())
                    map.computeIfAbsent(getUnionClass(resultClasses, box), _ -> new HashSet<List<ResourceClass>>())
                            .add(List.of(le, re));
            }
        }

        if(map.isEmpty())
            return SqlNull.get();


        List<SqlExpressionIntercode> operands = List.of(left, right);
        Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(operands, map, restriction, box);

        Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();

        for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
            mappings.put(e.getKey(),
                    e.getValue() == null ? null : translate(operator, e.getKey(), e.getValue(), left, right));

        return new SqlBinaryArithmetic(operator, left, right, mappings, canBeNull);
    }


    private static boolean canBeDecimalZero(SqlExpressionIntercode operand)
    {
        if(!(operand instanceof SqlLiteral literal))
            return true;

        //NOTE: the caller has already verified that operand is numeric

        return switch(literal.getLiteral().getValue())
        {
            case Short number -> number == 0;
            case Integer number -> number == 0;
            case Long number -> number == 0l;
            case BigInteger number -> number.equals(BigInteger.ZERO);
            case BigDecimal number -> number.equals(BigDecimal.ZERO);
            default -> false;
        };
    }


    private static List<Column> translate(Operator operator, ResourceClass resultClass,
            Set<List<Set<ResourceClass>>> variants, SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        Set<Column> cols = new HashSet<Column>();

        if(Stream.of(xsdDouble, xsdFloat, xsdDecimal, xsdInteger).anyMatch(r -> r.equals(resultClass)))
        {
            for(List<Set<ResourceClass>> variant : variants)
            {
                Column cl = left.promoteNumericAs(variant.get(0), resultClass);
                Column cr = right.promoteNumericAs(variant.get(1), resultClass);

                cols.add(new ExpressionColumn("(" + cl + " operator(sparql." + operator.getText() + ") " + cr + ")"));
            }
        }
        else
        {
            for(List<Set<ResourceClass>> variant : variants)
            {
                Column cl = left.get(getUnionClass(variant.get(0), box)).get(0);
                Column cr = right.get(getUnionClass(variant.get(1), box)).get(0);

                cols.add(new ExpressionColumn("(" + cl + " operator(sparql." + operator.getText() + ") " + cr + ")"));
            }
        }

        return List.of(Column.coalesce(cols));
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, Restriction restriction,
            boolean evalServices)
    {
        List<ResourceClass> numbers = List.of(xsdShort, xsdInt, xsdLong, xsdInteger, xsdDecimal, xsdFloat, xsdDouble);
        Restriction operandRestriction = new Restriction();

        for(ResourceClass number : List.of(xsdInteger, xsdDecimal, xsdFloat, xsdDouble))
            if(restriction.contains(number))
                operandRestriction.add(numbers.subList(0, numbers.indexOf(number) + 1));

        SqlExpressionIntercode optLeft = left.optimize(request, variables, operandRestriction, evalServices);
        SqlExpressionIntercode optRight = right.optimize(request, variables, operandRestriction, evalServices);

        if(optLeft == left && optRight == right && restriction.isOptimized(variable))
            return this;

        return create(operator, optLeft, optRight, restriction);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        int myPriortity = operator == Operator.Multiply || operator == Operator.Divide ? 4 : 5;

        if(myPriortity > priority)
            builder.append("(");

        left.generateExplanation(builder, indent, myPriortity);
        builder.append(" ");
        builder.append(operator.getText());
        builder.append(" ");
        right.generateExplanation(builder, indent, myPriortity);

        if(myPriortity > priority)
            builder.append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlBinaryArithmetic imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(operator, imcode.operator))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(operator, left, right);
    }
}

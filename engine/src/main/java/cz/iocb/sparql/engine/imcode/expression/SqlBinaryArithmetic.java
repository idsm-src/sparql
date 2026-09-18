package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic.ArithmeticOperator.DIVIDE;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic.ArithmeticOperator.MULTIPLY;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.numericBaseClasses;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
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
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlBinaryArithmetic extends SqlBinary
{
    public static enum ArithmeticOperator
    {
        MULTIPLY("*", "mul"), DIVIDE("/", "div"), ADD("+", "add"), SUBTRACT("-", "sub");

        private final String text;
        private final String name;

        ArithmeticOperator(String text, String name)
        {
            this.text = text;
            this.name = name;
        }

        public String getText()
        {
            return text;
        }

        public String getName()
        {
            return name;
        }
    }


    private final ArithmeticOperator operator;


    private SqlBinaryArithmetic(ArithmeticOperator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(left, right, mappings, canBeNull);

        this.operator = operator;
    }


    public static SqlExpressionIntercode create(ArithmeticOperator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        return create(operator, left, right, Restriction.ALL);
    }


    private static SqlExpressionIntercode create(ArithmeticOperator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right, Restriction restriction)
    {
        Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<>();

        boolean canBeNull = left.canBeNull() || right.canBeNull();

        for(ResourceClass le : left.getBinding().getMappings().keySet())
        {
            for(ResourceClass re : right.getBinding().getMappings().keySet())
            {
                canBeNull |= !isNumeric(le) || !isNumeric(re);

                Set<ResourceClass> resultClasses = new HashSet<>();

                for(ResourceClass l : getNumericClasses(le))
                {
                    for(ResourceClass r : getNumericClasses(re))
                    {
                        ResourceClass res = determineResultClass(l, r);

                        if(operator == DIVIDE && res.equals(xsdInteger))
                            res = xsdDecimal;

                        if(res.equals(xsdDecimal) || res.equals(xsdInteger))
                            canBeNull = true; // because integer/decimal operations can overflow

                        resultClasses.add(res);
                    }
                }

                if(resultClasses.size() == 1)
                    map.computeIfAbsent(resultClasses.iterator().next(), _ -> new HashSet<>()).add(List.of(le, re));
                else if(!resultClasses.isEmpty())
                    map.computeIfAbsent(unionize(resultClasses, box), _ -> new HashSet<>()).add(List.of(le, re));
            }
        }

        if(map.isEmpty())
            return SqlNull.get();


        List<SqlExpressionIntercode> operands = List.of(left, right);
        Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(operands, map, restriction, box);

        Map<ResourceClass, List<Column>> mappings = new HashMap<>();

        for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
            mappings.put(e.getKey(),
                    e.getValue() == null ? null : translate(operator, e.getKey(), e.getValue(), left, right));

        return new SqlBinaryArithmetic(operator, left, right, mappings, canBeNull);
    }


    private static List<Column> translate(ArithmeticOperator operator, ResourceClass resultClass,
            Set<List<Set<ResourceClass>>> variants, SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        Set<Column> cols = new HashSet<>();

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
                Column cl = left.get(unionize(variant.get(0), box)).get(0);
                Column cr = right.get(unionize(variant.get(1), box)).get(0);

                cols.add(new ExpressionColumn("(" + cl + " operator(sparql." + operator.getText() + ") " + cr + ")"));
            }
        }

        return List.of(Column.coalesce(cols));
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        List<ResourceClass> results = List.of(xsdDouble, xsdFloat, xsdDecimal, xsdInteger);

        Restriction operandRestriction = new Restriction();

        // the operands promoted to a result class are all the classes not following it in the promotion order
        for(int i = 0; i < results.size(); i++)
            if(restriction.contains(results.get(i)))
                operandRestriction.add(numericBaseClasses.subList(0, numericBaseClasses.size() - i));

        SqlExpressionIntercode optLeft = left.optimize(request, bindings, operandRestriction, evalServices);
        SqlExpressionIntercode optRight = right.optimize(request, bindings, operandRestriction, evalServices);

        if(optLeft == left && optRight == right && restriction.isOptimized(variableBinding))
            return this;

        return create(operator, optLeft, optRight, restriction);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        int myPriortity = operator == MULTIPLY || operator == DIVIDE ? 4 : 5;

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

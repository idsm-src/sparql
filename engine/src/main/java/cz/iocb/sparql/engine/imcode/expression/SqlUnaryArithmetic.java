package cz.iocb.sparql.engine.imcode.expression;

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



public final class SqlUnaryArithmetic extends SqlUnary
{
    private final boolean isMinus;


    private SqlUnaryArithmetic(boolean isMinus, SqlExpressionIntercode operand,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(operand, mappings, canBeNull);

        this.isMinus = isMinus;
    }


    public static SqlExpressionIntercode create(boolean isMinus, SqlExpressionIntercode operand)
    {
        return create(isMinus, operand, Restriction.ALL);
    }


    public static SqlExpressionIntercode create(boolean isMinus, SqlExpressionIntercode operand,
            Restriction restriction)
    {
        Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<>();

        boolean canBeNull = operand.canBeNull();

        for(ResourceClass le : operand.getBinding().getMappings().keySet())
        {
            canBeNull |= !isNumeric(le);

            for(ResourceClass l : getNumericClasses(le))
            {
                ResourceClass res = determineResultClass(l);
                map.computeIfAbsent(res, _ -> new HashSet<>()).add(List.of(l));
            }
        }

        if(map.isEmpty())
            return SqlNull.get();


        List<SqlExpressionIntercode> operands = List.of(operand);
        Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(operands, map, restriction, box);

        Map<ResourceClass, List<Column>> mappings = new HashMap<>();

        for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
            mappings.put(e.getKey(),
                    e.getValue() == null ? null : translate(isMinus, e.getKey(), e.getValue(), operand));

        return new SqlUnaryArithmetic(isMinus, operand, mappings, canBeNull);
    }


    private static List<Column> translate(boolean isMinus, ResourceClass resultClass,
            Set<List<Set<ResourceClass>>> variants, SqlExpressionIntercode operand)
    {
        Set<Column> cols = new HashSet<>();

        if(Stream.of(xsdDouble, xsdFloat, xsdDecimal, xsdInteger).anyMatch(r -> r.equals(resultClass)))
        {
            for(List<Set<ResourceClass>> variant : variants)
            {
                Column op = operand.promoteNumericAs(variant.get(0), resultClass);
                cols.add(new ExpressionColumn("(operator(sparql.-) " + op + ")"));
            }
        }
        else
        {
            for(List<Set<ResourceClass>> variant : variants)
            {
                Column op = operand.get(getUnionClass(variant.get(0), box)).get(0);
                cols.add(new ExpressionColumn("(operator(sparql.-) " + op + ")"));
            }
        }

        return List.of(Column.coalesce(cols));
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        List<ResourceClass> numbers = List.of(xsdShort, xsdInt, xsdLong, xsdInteger, xsdDecimal, xsdFloat, xsdDouble);
        Restriction operandRestriction = new Restriction();

        for(ResourceClass number : List.of(xsdInteger, xsdDecimal, xsdFloat, xsdDouble))
            if(restriction.contains(number))
                operandRestriction.add(numbers.subList(0, numbers.indexOf(number) + 1));

        SqlExpressionIntercode optOperand = operand.optimize(request, bindings, operandRestriction, evalServices);

        if(optOperand == operand && restriction.isOptimized(variableBinding))
            return this;

        return create(isMinus, optOperand, restriction);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append(isMinus ? "- " : "+ ");
        operand.generateExplanation(builder, indent, 3);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlUnaryArithmetic imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(isMinus, imcode.isMinus))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(isMinus, operand);
    }
}

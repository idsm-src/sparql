package cz.iocb.sparql.engine.imcode.expression;

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



/**
 * Unary plus or minus of a numeric operand; other operands are an error.
 */
public final class SqlUnaryArithmetic extends SqlUnary
{
    /**
     * True for minus, false for plus.
     */
    private final boolean isMinus;


    /**
     * Creates the expression.
     *
     * @param isMinus true for minus, false for plus
     * @param operand the operand
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    private SqlUnaryArithmetic(boolean isMinus, SqlExpressionIntercode operand,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(operand, mappings, canBeNull);

        this.isMinus = isMinus;
    }


    /**
     * Unary plus or minus of the operand.
     *
     * @param isMinus true for minus, false for plus
     * @param operand the operand
     * @return unary plus or minus of the operand
     */
    public static SqlExpressionIntercode create(boolean isMinus, SqlExpressionIntercode operand)
    {
        return create(isMinus, operand, Restriction.ALL);
    }


    /**
     * Unary plus or minus materialising only the needed result classes; NULL for non-numeric operands.
     *
     * @param isMinus true for minus, false for plus
     * @param operand the operand
     * @param restriction the result classes the parent needs
     * @return unary plus or minus materialising only the needed result classes; NULL for non-numeric operands
     */
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


    /**
     * SQL computing the result in the class from the operand promoted to it.
     *
     * @param isMinus true for minus, false for plus
     * @param resultClass the result class
     * @param variants combinations of argument classes
     * @param operand the operand
     * @return SQL computing the result in the class from the operand promoted to it
     */
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
                Column op = operand.get(unionize(variant.get(0), box)).get(0);
                cols.add(new ExpressionColumn("(operator(sparql.-) " + op + ")"));
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

package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloatPoint;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.numericBaseClasses;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static java.util.Collections.singletonMap;
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
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Effective boolean value of an operand (SPARQL 1.2, section 17.2.2): booleans as they are, numerics true when non-zero
 * and not NaN, strings true when non-empty, anything else (a literal with an invalid lexical form or of another
 * datatype, a non-literal) an error.
 */
public final class SqlEffectiveBooleanValue extends SqlUnary
{
    /**
     * Classes that have an effective boolean value.
     */
    private static final Set<ResourceClass> operandClasses = Stream
            .concat(Stream.of(genBoolean, xsdString), numericBaseClasses.stream()).collect(toSet());

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
     */
    private SqlEffectiveBooleanValue(SqlExpressionIntercode operand, Map<ResourceClass, List<Column>> mappings,
            boolean canBeNull)
    {
        super(operand, mappings, canBeNull);
    }


    /**
     * Effective boolean value of the operand.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operand the operand
     * @return effective boolean value of the operand
     */
    public static SqlExpressionIntercode create(ClassRelations relations, SqlExpressionIntercode operand)
    {
        return create(relations, operand, Restriction.ALL);
    }


    /**
     * Effective boolean value materialising only when needed; constants and NULL pass through.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operand the operand
     * @param restriction the result classes the parent needs
     * @return effective boolean value materialising only when needed; constants and NULL pass through
     */
    private static SqlExpressionIntercode create(ClassRelations relations, SqlExpressionIntercode operand,
            Restriction restriction)
    {
        if(operand.equals(SqlNull.get()))
            return SqlNull.get();

        if(operand.equals(trueValue) || operand.equals(falseValue))
            return operand;

        //TODO: add compile-time evaluation for literals

        if(operand.getMappings().keySet().stream().noneMatch(r -> !areDisjunct(relations, r, operandClass)))
            return SqlNull.get();

        Set<ResourceClass> classes = operand.getMappings().keySet();

        boolean canBeNull = operand.canBeNull() || classes.stream().anyMatch(r -> !r.isSubclassOf(operandClass));


        List<Column> columns = restriction.contains(relations, xsdBoolean) ? translate(relations, operand) : null;
        Map<ResourceClass, List<Column>> mappings = singletonMap(xsdBoolean, columns);

        return new SqlEffectiveBooleanValue(operand, mappings, canBeNull);
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        Restriction operandRestriction = new Restriction();

        if(restriction.contains(relations, xsdBoolean))
            operandRestriction.add(operandClasses);

        SqlExpressionIntercode optOperand = operand.optimize(request, bindings, operandRestriction, evalServices);

        if(optOperand.getResourceClasses().stream().allMatch(r -> r.isSubclassOf(xsdBoolean)))
            return optOperand;

        if(optOperand == operand)
            return this;

        return create(relations, optOperand);
    }


    /**
     * SQL computing the effective boolean value: booleans as is, numerics compared to zero, strings tested for
     * emptiness, boxed values through the extension; a class with no effective boolean value yields NULL.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operand the operand
     * @return SQL computing the effective boolean value: booleans as is, numerics compared to zero, strings tested for
     *         emptiness, boxed values through the extension; a class with no effective boolean value yields NULL
     */
    private static List<Column> translate(ClassRelations relations, SqlExpressionIntercode operand)
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
                cols.add(new ExpressionColumn("(" + col + " not in ('0'::" + type + ", 'NaN'::" + type + "))", BOOL));
            }
            else if(numericBase != null)
            {
                SqlType type = numericBase.getSqlTypes().get(0);
                Column col = e.getKey().toGeneralClass(numericBase, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(" + col + " != '0'::" + type + ")", BOOL));
            }
            else if(isString(e.getKey()))
            {
                Column col = e.getKey().toGeneralClass(xsdString, e.getValue(), true).get(0);
                cols.add(new ExpressionColumn("(octet_length(" + col + ") != 0)", BOOL));
            }
            else if(isBoolean(e.getKey()))
            {
                Column col = e.getKey().toGeneralClass(genBoolean, e.getValue(), true).get(0);
                cols.add(col);
            }
            else if(!areDisjunct(relations, e.getKey(), operandClass))
            {
                Column col = operand.get(relations, unionize(operandClasses, box)).get(0);
                cols.add(new ExpressionColumn("sparql.ebv_rdfbox(" + col + ")", BOOL));
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

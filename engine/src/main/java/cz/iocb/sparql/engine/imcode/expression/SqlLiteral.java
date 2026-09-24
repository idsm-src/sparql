package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static java.util.Collections.singletonMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Constant literal, represented in the class given by its datatype; {@link #trueValue} and {@link #falseValue} are the
 * shared boolean constants.
 */
public final class SqlLiteral extends SqlExpressionIntercode
{
    /**
     * The literal {@code true}.
     */
    private static final Literal trueLiteral = new TypedLiteral("true", xsdBooleanIri);

    /**
     * The literal {@code false}.
     */
    private static final Literal falseLiteral = new TypedLiteral("false", xsdBooleanIri);

    /**
     * Shared expression of the boolean {@code true}.
     */
    public static final SqlLiteral trueValue = create(trueLiteral, xsdBoolean);

    /**
     * Shared expression of the boolean {@code false}.
     */
    public static final SqlLiteral falseValue = create(falseLiteral, xsdBoolean);

    /**
     * The literal.
     */
    private final Literal literal;


    /**
     * Creates the expression.
     *
     * @param literal the literal
     * @param map columns per resource class
     */
    private SqlLiteral(Literal literal, Map<ResourceClass, List<Column>> map)
    {
        super(map, false, true);

        this.literal = literal;
    }


    /**
     * Constant literal expression in the class given by its datatype.
     *
     * @param request the current request
     * @param literal the literal
     * @return constant literal expression in the class given by its datatype
     */
    public static SqlExpressionIntercode create(Request request, Literal literal)
    {
        return create(request, literal, Restriction.ALL);
    }


    /**
     * Constant literal expression; the boolean constants are the shared instances.
     *
     * @param request the current request
     * @param literal the literal
     * @param restriction the result classes the parent needs
     * @return constant literal expression; the boolean constants are the shared instances
     */
    private static SqlExpressionIntercode create(Request request, Literal literal, Restriction restriction)
    {
        //TODO: delete these variants after the equals method is used

        if(literal.equals(trueLiteral))
            return trueValue;

        if(literal.equals(falseLiteral))
            return falseValue;

        ResourceClass resClass = request.getLiteralClass(literal);
        List<Column> columns = resClass.toColumns(request.getStatement(), literal);

        return new SqlLiteral(literal, singletonMap(resClass, columns));
    }


    /**
     * Constant literal expression in the given class.
     *
     * @param literal the literal
     * @param resClass the resource class
     * @return constant literal expression in the given class
     */
    private static SqlLiteral create(Literal literal, LiteralClass resClass)
    {
        List<Column> columns = resClass.toColumns(literal);

        return new SqlLiteral(literal, singletonMap(resClass, columns));
    }


    @Override
    public Restrictions getRequirements()
    {
        return new Restrictions();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        if(restriction.isOptimized(variableBinding))
            return this;

        return create(request, literal, restriction);
    }


    /**
     * The literal.
     *
     * @return the literal
     */
    public Literal getLiteral()
    {
        return literal;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append(literal);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlLiteral imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(literal, imcode.literal))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(literal);
    }
}

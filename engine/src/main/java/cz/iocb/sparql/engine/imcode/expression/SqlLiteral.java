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



public final class SqlLiteral extends SqlExpressionIntercode
{
    private static final Literal trueLiteral = new TypedLiteral("true", xsdBooleanIri);
    private static final Literal falseLiteral = new TypedLiteral("false", xsdBooleanIri);

    public static final SqlLiteral trueValue = create(trueLiteral, xsdBoolean);
    public static final SqlLiteral falseValue = create(falseLiteral, xsdBoolean);

    private final Literal literal;


    private SqlLiteral(Literal literal, Map<ResourceClass, List<Column>> map)
    {
        super(map, false, true);

        this.literal = literal;
    }


    public static SqlExpressionIntercode create(Request request, Literal literal)
    {
        return create(request, literal, Restriction.ALL);
    }


    private static SqlExpressionIntercode create(Request request, Literal literal, Restriction restriction)
    {
        //TODO: delete these variants after the equals method is used

        if(literal.equals(trueLiteral))
            return trueValue;

        if(literal.equals(falseLiteral))
            return falseValue;

        LiteralClass resClass = request.getLiteralClass(literal);
        List<Column> columns = resClass.toColumns(literal);

        return new SqlLiteral(literal, singletonMap(resClass, columns));
    }


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

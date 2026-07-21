package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdBooleanType;
import static java.util.Collections.singletonMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public final class SqlLiteral extends SqlExpressionIntercode
{
    private static final Literal trueLiteral = new Literal("true", xsdBooleanType);
    private static final Literal falseLiteral = new Literal("false", xsdBooleanType);

    public static final SqlLiteral trueValue = create(trueLiteral);
    public static final SqlLiteral falseValue = create(falseLiteral);

    private final Literal literal;


    private SqlLiteral(Literal literal, Map<ResourceClass, List<Column>> map)
    {
        super(map, false, true);

        this.literal = literal;
    }


    private static SqlLiteral create(Literal literal)
    {
        return create(literal, Restriction.ALL);
    }


    private static SqlLiteral create(Literal literal, Restriction restriction)
    {
        LiteralClass resClass = literal.isTypeSupported() ? literal.getDataType().getResourceClass(literal) :
                unsupportedLiteral;

        List<Column> columns = restriction.contains(resClass) ? resClass.toColumns(literal) : null;

        return new SqlLiteral(literal, singletonMap(resClass, columns));
    }


    public static SqlExpressionIntercode create(Request request, Literal literal)
    {
        //TODO: delete these variants after the equals method is used

        if(literal.equals(trueLiteral))
            return trueValue;

        if(literal.equals(falseLiteral))
            return falseValue;

        LiteralClass resClass = literal.isTypeSupported() ? literal.getDataType().getResourceClass(literal) :
                unsupportedLiteral;
        List<Column> columns = resClass.toColumns(literal);

        return new SqlLiteral(literal, singletonMap(resClass, columns));
    }


    @Override
    public Restrictions getRequirements()
    {
        return new Restrictions();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, Restriction restriction,
            boolean evalServices)
    {
        if(restriction.isOptimized(variable))
            return this;

        return create(literal, restriction);
    }


    public Literal getLiteral()
    {
        return literal;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append("'");
        builder.append(literal.getStringValue().replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r"));
        builder.append("'");

        if(literal.getLanguageTag() != null)
        {
            builder.append('@');
            builder.append(literal.getLanguageTag());
        }

        else if(literal.getTypeIri() != null && !literal.isSimple())
        {
            builder.append("^^");

            IRI type = literal.getTypeIri();

            if(type.getValue().startsWith("http://www.w3.org/2001/XMLSchema#"))
                builder.append("xsd:").append(type.getValue().substring(33));
            else
                builder.append(type);
        }
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

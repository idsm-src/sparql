package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdBooleanType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public class SqlLiteral extends SqlNodeValue
{
    public static final Literal trueLiteral = new Literal("true", xsdBooleanType);
    public static final Literal falseLiteral = new Literal("false", xsdBooleanType);

    public static final SqlLiteral trueValue = new SqlLiteral(trueLiteral, xsdBoolean);
    public static final SqlLiteral falseValue = new SqlLiteral(falseLiteral, xsdBoolean);

    private final Literal literal;
    private final LiteralClass literalClass;


    protected SqlLiteral(Literal literal, LiteralClass resourceClass)
    {
        super(asSet(resourceClass), false);
        this.literalClass = resourceClass;
        this.literal = literal;
    }


    public static SqlExpressionIntercode create(Request request, Literal literal)
    {
        if(literal.equals(trueLiteral))
            return trueValue;

        if(literal.equals(falseLiteral))
            return falseValue;

        LiteralClass resourceClass = literal.isTypeSupported() ? literal.getDataType().getResourceClass(literal) :
                unsupportedLiteral;

        return new SqlLiteral(literal, resourceClass);
    }


    @Override
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        return new Restrictions();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        return this;
    }


    @Override
    public String translate(Request request)
    {
        return getResourceClass().toExpression(request.getStatement(), literal).toString();
    }


    public Literal getLiteral()
    {
        return literal;
    }


    public LiteralClass getLiteralClass()
    {
        return literalClass;
    }


    @Override
    public List<Column> asResource(Request request, ResourceClass resourceClass)
    {
        if(!resourceClass.match(request.getStatement(), literal))
            return resourceClass.getSqlTypes().stream().map(t -> (Column) new ConstantColumn(null, t)).toList();

        return resourceClass.toColumns(request.getStatement(), literal);
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
}

package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdBooleanType;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.DataType;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;



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

        DataType datatype = request.getConfiguration().getDataType(literal.getTypeIri());
        LiteralClass resourceClass = datatype == null ? unsupportedLiteral : datatype.getResourceClass(literal);

        return new SqlLiteral(literal, resourceClass);
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables)
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
        return resourceClass.toColumns(request.getStatement(), literal);
    }
}

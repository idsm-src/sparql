package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlLiteral extends SqlNodeValue
{
    private final Literal literal;
    private final LiteralClass literalClass;


    protected SqlLiteral(Literal literal, LiteralClass resourceClass)
    {
        super(asSet(resourceClass), false);
        this.literalClass = resourceClass;
        this.literal = literal;
    }


    public static SqlExpressionIntercode create(Literal literal)
    {
        LiteralClass resourceClass = unsupportedLiteral;

        if(literal.getLanguageTag() != null)
        {
            resourceClass = LangStringConstantTagClass.get(literal.getLanguageTag());
        }
        else if(literal.getValue() != null && literal.getTypeIri() != null)
        {
            for(LiteralClass literalClass : BuiltinClasses.getLiteralClasses())
                if(literalClass.getTypeIri().equals(literal.getTypeIri()))
                    resourceClass = literalClass;

            if(resourceClass == xsdDateTime)
                resourceClass = DateTimeConstantZoneClass.get(DateTimeClass.getZone(literal));
            else if(resourceClass == xsdDate)
                resourceClass = DateConstantZoneClass.get(DateClass.getZone(literal));
        }

        return new SqlLiteral(literal, resourceClass);
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        return this;
    }


    @Override
    public String translate()
    {
        LiteralClass resourceClass = (LiteralClass) getResourceClasses().iterator().next();

        return resourceClass.toExpression(literal).toString();
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
    public List<Column> asResource(ResourceClass resourceClass)
    {
        return resourceClass.toColumns(literal);
    }
}

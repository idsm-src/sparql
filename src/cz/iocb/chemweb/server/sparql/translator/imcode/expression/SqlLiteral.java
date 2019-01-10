package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlLiteral extends SqlNodeValue
{
    private final Literal literal;


    protected SqlLiteral(Literal literal, Set<ResourceClass> resourceClasses)
    {
        super(resourceClasses, false);
        this.literal = literal;
    }


    public static SqlExpressionIntercode create(Literal literal)
    {
        ResourceClass resourceClass = unsupportedLiteral;

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
                resourceClass = DateTimeConstantZoneClass.get(DateTimeConstantZoneClass.getZone(literal));
            else if(resourceClass == xsdDate)
                resourceClass = DateConstantZoneClass.get(DateConstantZoneClass.getZone(literal));
        }

        return new SqlLiteral(literal, asSet(resourceClass));
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        return this;
    }


    @Override
    public String translate()
    {
        LiteralClass resourceClass = (LiteralClass) getResourceClasses().iterator().next();

        return resourceClass.getExpressionCode(literal);
    }


    public Literal getLiteral()
    {
        return literal;
    }


    @Override
    public String getNodeAccess(ResourceClass resourceClass, int part)
    {
        return resourceClass.getPatternCode(literal, part);
    }
}

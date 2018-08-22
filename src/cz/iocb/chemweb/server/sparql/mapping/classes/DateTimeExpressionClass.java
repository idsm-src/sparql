package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateTimeIri;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.ExpressionResourceBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateTimeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class DateTimeExpressionClass extends ExpressionResourceBaseClass implements DateTimeClass
{
    DateTimeExpressionClass()
    {
        super("datetime");
    }


    @Override
    public IRI getTypeIri()
    {
        return xsdDateTimeIri;
    }


    @Override
    public String getSqlLiteralValue(Literal literal)
    {
        return "'" + literal.getStringValue().trim() + "'::sparql.zoneddatetime";
    }


    @Override
    public PatternResourceClass getPatternResourceClass()
    {
        return xsdDateTime;
    }


    @Override
    public String getSqlPatternValue(String column, int part, boolean isBoxed)
    {
        if(isBoxed)
            return "sparql.rdfbox_extract_datetime_" + (part == 0 ? "datetime" : "zone") + "(" + column + ")";
        else
            return "sparql.zoneddatetime_" + (part == 0 ? "datetime" : "zone") + "(" + column + ")";
    }
}

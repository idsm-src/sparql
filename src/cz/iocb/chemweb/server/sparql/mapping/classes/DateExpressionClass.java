package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.ExpressionResourceBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class DateExpressionClass extends ExpressionResourceBaseClass implements DateClass
{
    DateExpressionClass()
    {
        super("date");
    }


    @Override
    public IRI getTypeIri()
    {
        return xsdDateIri;
    }


    @Override
    public String getSqlLiteralValue(Literal literal)
    {
        return "'" + (String) literal.getValue() + "'::sparql.zoneddate";
    }


    @Override
    public PatternResourceClass getPatternResourceClass()
    {
        return xsdDate;
    }


    @Override
    public String getSqlPatternValue(String column, int part, boolean isBoxed)
    {
        if(isBoxed)
            return "sparql.rdfbox_extract_date_" + (part == 0 ? "date" : "zone") + "(" + column + ")";
        else
            return "sparql.zoneddate_" + (part == 0 ? "date" : "zone") + "(" + column + ")";
    }
}

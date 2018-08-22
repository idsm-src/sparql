package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.DATE;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.PatternLiteralBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class DatePatternClassWithConstantZone extends PatternLiteralBaseClass implements DateClass
{
    private final int zone;


    public DatePatternClassWithConstantZone(int zone)
    {
        super("date$" + zone, Arrays.asList("date"), Arrays.asList(DATE), xsdDateIri);
        this.zone = zone;
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        return "sparql.zoneddate_create(" + getSqlColumn(variable, 0) + ", '" + zone + "'::int4)";
    }


    @Override
    public String getSqlPatternLiteralValue(Literal literal, int part)
    {
        return "sparql.zoneddate_date('" + (String) literal.getValue() + "'::sparql.zoneddate)";
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return BuiltinClasses.xsdDateExpr;
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return (rdfbox ? "sparql.cast_as_rdfbox_from_date" : "sparql.zoneddate_create") + "("
                + variableAccessor.variableAccess(variable, this, 0) + ", '" + zone + "'::int4)";
    }


    @Override
    public boolean match(Node node)
    {
        if(!super.match(node))
            return false;

        if(!(node instanceof Literal))
            return true;

        if(getZone((Literal) node) != this.zone)
            return false;

        return true;
    }


    public int getZone()
    {
        return zone;
    }


    public static int getZone(Literal literal)
    {
        String value = (String) literal.getValue();
        String[] parts = value.replaceFirst(".*(Z|(([+-])([0-9][0-9]):([0-9][0-9])))$", "$31#0$4#0$5").split("#");
        int zone = Integer.MIN_VALUE;

        if(parts.length == 3)
            zone = Integer.parseInt(parts[0]) * (Integer.parseInt(parts[1]) * 3600 + Integer.parseInt(parts[2]) * 60);

        return zone;
    }
}

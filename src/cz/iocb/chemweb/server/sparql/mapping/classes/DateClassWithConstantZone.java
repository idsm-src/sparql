package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DATE;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class DateClassWithConstantZone extends LiteralClass
{
    private final int zone;


    public DateClassWithConstantZone(int zone)
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
    public String getSqlLiteralValue(Literal literal, int part)
    {
        return "sparql.zoneddate_date('" + (String) literal.getValue() + "'::sparql.zoneddate)";
    }


    @Override
    public boolean match(Node node)
    {
        if(!super.match(node))
            return false;

        if(!(node instanceof Literal))
            return true;

        Literal literal = (Literal) node;

        String value = (String) literal.getValue();
        String[] parts = value.replaceFirst(".*(Z|(([+-])([0-9][0-9]):([0-9][0-9])))$", "$31#0$4#0$5").split("#");
        int zone = Integer.MIN_VALUE;

        if(parts.length == 3)
            zone = Integer.parseInt(parts[0]) * (Integer.parseInt(parts[1]) * 3600 + Integer.parseInt(parts[2]) * 60);

        if(zone != this.zone)
            return false;

        return true;
    }
}

package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DATETIME;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateTimeIri;
import java.time.OffsetDateTime;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class DateTimeClassWithConstantZone extends LiteralClass
{
    private final int zone;


    public DateTimeClassWithConstantZone(int zone)
    {
        super("datetime$" + zone, Arrays.asList("timestamptz"), Arrays.asList(DATETIME), xsdDateTimeIri);
        this.zone = zone;
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        return "sparql.zoneddatetime_create(" + getSqlColumn(variable, 0) + ", '" + zone + "'::int4)";
    }


    @Override
    public String getSqlLiteralValue(Literal literal, int part)
    {
        return "sparql.zoneddatetime_datetime('" + literal.getStringValue().trim() + "'::sparql.zoneddatetime)";
    }


    @Override
    public boolean match(Node node)
    {
        if(!super.match(node))
            return false;

        if(!(node instanceof Literal))
            return true;

        Literal literal = (Literal) node;

        int zone = Integer.MIN_VALUE;

        if(literal.getValue() instanceof OffsetDateTime)
            zone = ((OffsetDateTime) literal.getValue()).getOffset().getTotalSeconds();

        if(zone != this.zone)
            return false;

        return true;
    }
}

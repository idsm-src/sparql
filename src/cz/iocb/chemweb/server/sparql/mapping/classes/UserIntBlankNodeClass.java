package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.intBlankNode;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class UserIntBlankNodeClass extends IntBlankNodeClass
{
    private final int segment;


    public UserIntBlankNodeClass(int segment)
    {
        super("int_blanknode_" + Integer.toHexString(segment), Arrays.asList("int4"));
        this.segment = segment;
    }


    @Override
    public String getPatternCode(Node node, int part)
    {
        if(!(node instanceof VariableOrBlankNode))
            throw new IllegalArgumentException();

        return getSqlColumn(((VariableOrBlankNode) node).getName(), part);
    }


    @Override
    public String getGeneralisedPatternCode(String table, String var, int part, boolean check)
    {
        return "('" + segment + "'::int8 << 32 | " + (table != null ? table + "." : "") + getSqlColumn(var, part) + ")";
    }


    @Override
    public String getSpecialisedPatternCode(String table, String var, int part)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN (");

        if(table != null)
            builder.append(table).append(".");

        builder.append(intBlankNode.getSqlColumn(var, 1));
        builder.append(" >> 32)::int4 = '");
        builder.append(segment);
        builder.append("'::int4 THEN (");

        if(table != null)
            builder.append(table).append(".");

        builder.append(intBlankNode.getSqlColumn(var, 0));
        builder.append(" & x'FFFFFFFF'::int8)::int4 END");

        return builder.toString();
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            return "(" + column + " & x'FFFFFFFF'::int8)::int4";

        return "(sparql.rdfbox_extract_int_blanknode" + "(" + column + ") & x'FFFFFFFF'::int8)::int4";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        String code = variableAccessor.getSqlVariableAccess(variable, this, 0);

        if(rdfbox)
            code = "sparql.cast_as_rdfbox_from_int_blanknode('" + segment + "'::int8 << 32 | " + code + ")";

        return code;
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        return "('" + segment + "'::int8 << 32 | " + getSqlColumn(variable, part) + ")";
    }


    public int getSegment()
    {
        return segment;
    }
}

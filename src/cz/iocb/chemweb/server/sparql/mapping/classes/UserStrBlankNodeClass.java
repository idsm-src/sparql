package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.strBlankNode;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.BlankNodeLiteral;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class UserStrBlankNodeClass extends StrBlankNodeClass
{
    private final String segment;


    public UserStrBlankNodeClass(int segment)
    {
        super("str_blanknode_" + Integer.toHexString(segment), Arrays.asList("varchar"));
        this.segment = String.format("%08X", segment);
    }


    @Override
    public String getPatternCode(Node node, int part)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getSqlName(), part);

        BlankNodeLiteral literal = (BlankNodeLiteral) node;

        return "'" + literal.getLabel() + "'::varchar";
    }


    @Override
    public String getGeneralisedPatternCode(String table, String var, int part, boolean check)
    {
        return "('" + segment + "'::varchar || " + (table != null ? table + "." : "") + getSqlColumn(var, part) + ")";
    }


    @Override
    public String getSpecialisedPatternCode(String table, String var, int part)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN substr(");

        if(table != null)
            builder.append(table).append(".");

        builder.append(strBlankNode.getSqlColumn(var, 1));
        builder.append(", 0, 9) = '");
        builder.append(segment);
        builder.append("'::varchar THEN substr(");

        if(table != null)
            builder.append(table).append(".");

        builder.append(strBlankNode.getSqlColumn(var, 0));
        builder.append(", 9) END");

        return builder.toString();
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            return "substr(" + column + ", 9)";

        return "substr(sparql.rdfbox_extract_str_blanknode" + "(" + column + "), 9)";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        String code = variableAccessor.getSqlVariableAccess(variable, this, 0);

        if(rdfbox)
            code = "sparql.cast_as_rdfbox_from_str_blanknode('" + segment + "'::varchar || " + code + ")";

        return code;
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        return "('" + segment + "'::varchar || " + getSqlColumn(variable, part) + ")";
    }


    public String getSegment()
    {
        return segment;
    }
}

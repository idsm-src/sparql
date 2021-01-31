package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.intBlankNode;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.BlankNodeLiteral;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class UserIntBlankNodeClass extends IntBlankNodeClass
{
    static private int counter;

    private final int segment;


    public UserIntBlankNodeClass()
    {
        super("sparql.int_blanknode_" + Integer.toHexString(counter), Arrays.asList("int4"));
        this.segment = counter++;
    }


    public UserIntBlankNodeClass(int segment)
    {
        super("sparql.int_blanknode_" + Integer.toHexString(counter), Arrays.asList("int4"));
        this.segment = segment;

        if(segment >= 0)
            throw new IllegalArgumentException();
    }


    @Override
    public String getPatternCode(Node node, int part)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getSqlName(), part);

        BlankNodeLiteral literal = (BlankNodeLiteral) node;

        return "'" + literal.getLabel() + "'::int4";
    }


    @Override
    public String getGeneralisedPatternCode(String table, String var, int part, boolean check)
    {
        return "sparql.int_blanknode_create('" + segment + "'::int4, " + (table != null ? table + "." : "")
                + getSqlColumn(var, part) + ")";
    }


    @Override
    public String getSpecialisedPatternCode(String table, String var, int part)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN sparql.int_blanknode_segment(");

        if(table != null)
            builder.append(table).append(".");

        builder.append(intBlankNode.getSqlColumn(var, 1));
        builder.append(") = '");
        builder.append(segment);
        builder.append("'::int4 THEN sparql.int_blanknode_label(");

        if(table != null)
            builder.append(table).append(".");

        builder.append(intBlankNode.getSqlColumn(var, 0));
        builder.append(") END");

        return builder.toString();
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            return "sparql.int_blanknode_label(" + column + ")";

        return "sparql.rdfbox_extract_int_blanknode_label" + "(" + column + ")";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        String code = variableAccessor.getSqlVariableAccess(variable, this, 0);

        if(rdfbox)
            code = "sparql.cast_as_rdfbox_from_int_blanknode('" + segment + "'::int4, " + code + ")";

        return code;
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        return "sparql.int_blanknode_create('" + segment + "'::int4, " + getSqlColumn(variable, part) + ")";
    }


    public int getSegment()
    {
        return segment;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        UserIntBlankNodeClass other = (UserIntBlankNodeClass) object;

        if(segment != other.segment)
            return false;

        return true;
    }
}

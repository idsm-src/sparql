package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class CommonStrBlankNodeClass extends StrBlankNodeClass
{
    CommonStrBlankNodeClass()
    {
        super("str_blanknode", Arrays.asList("varchar"));
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
        return (table != null ? table + "." : "") + getSqlColumn(var, part);
    }


    @Override
    public String getSpecialisedPatternCode(String table, String var, int part)
    {
        return (table != null ? table + "." : "") + getSqlColumn(var, part);
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            return column;

        return "sparql.rdfbox_extract_str_blanknode" + "(" + column + ")";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        String code = variableAccessor.getSqlVariableAccess(variable, this, 0);

        if(rdfbox)
            code = "sparql.cast_as_rdfbox_from_str_blanknode(" + code + ")";

        return code;
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        return getSqlColumn(variable, part);
    }
}

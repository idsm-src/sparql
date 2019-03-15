package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.Arrays;
import java.util.List;
import cz.iocb.chemweb.server.db.schema.Column;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class CommonIriClass extends IriClass
{
    CommonIriClass()
    {
        super("iri", Arrays.asList("varchar"), Arrays.asList(ResultTag.IRI));
    }


    @Override
    public String getPatternCode(Node node, int part)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getName(), part);

        IRI iri = (IRI) node;

        return "'" + iri.getValue().replaceAll("'", "''") + "'::varchar";
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
            throw new IllegalArgumentException();

        return "sparql.rdfbox_extract_iri" + "(" + column + ")";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        String code = variableAccessor.getSqlVariableAccess(variable, this, 0);

        if(rdfbox)
            code = "sparql.cast_as_rdfbox_from_iri(" + code + ")";

        return code;
    }


    @Override
    public String getResultCode(String var, int part)
    {
        return getSqlColumn(var, 0);
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode || node instanceof IRI)
            return true;

        return false;
    }


    @Override
    public String getIriValueCode(List<Column> columns)
    {
        return columns.get(0).getCode();
    }
}

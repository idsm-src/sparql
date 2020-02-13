package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.iri;
import java.util.Arrays;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public abstract class SimpleUserIriClass extends UserIriClass
{
    public SimpleUserIriClass(String name, String sqlType)
    {
        super(name, Arrays.asList(sqlType), Arrays.asList(ResultTag.IRI));
    }


    @Override
    public String getPatternCode(Node node, int part)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getSqlName(), part);

        return generateInverseFunction("'" + ((IRI) node).getValue().replaceAll("'", "''") + "'::varchar");
    }


    @Override
    public String getGeneralisedPatternCode(String table, String var, int part, boolean check)
    {
        return generateFunction(table != null ? table + "." + getSqlColumn(var, 0) : getSqlColumn(var, 0));
    }


    @Override
    public String getSpecialisedPatternCode(String table, String var, int part)
    {
        return generateInverseFunction((table != null ? table + "." : "") + iri.getSqlColumn(var, 0));
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        if(isBoxed)
            return generateInverseFunction("sparql.rdfbox_extract_iri(" + column + ")");

        return generateInverseFunction(column);
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        StringBuffer strBuf = new StringBuffer();

        if(rdfbox)
            strBuf.append("sparql.cast_as_rdfbox_from_iri(");

        strBuf.append(generateFunction(variableAccessor.getSqlVariableAccess(variable, this, 0)));

        if(rdfbox)
            strBuf.append(")");

        return strBuf.toString();
    }


    @Override
    public String getResultCode(String var, int part)
    {
        return generateFunction(getSqlColumn(var, 0));
    }


    @Override
    public boolean match(Node node, Request request)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        if(!(node instanceof IRI))
            return false;

        return match(((IRI) node).getValue(), request);
    }


    @Override
    public String getIriValueCode(List<Column> columns)
    {
        return generateFunction(columns.get(0).getCode());
    }


    protected abstract String generateFunction(String parameter);


    protected abstract String generateInverseFunction(String parameter);
}

package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class CommonIriClass extends IriClass implements ExpressionResourceClass
{
    CommonIriClass()
    {
        super("iri", Arrays.asList("varchar"), Arrays.asList(ResultTag.IRI));
    }


    @Override
    public String getResultValue(String var, int part)
    {
        return getSqlColumn(var, 0);
    }


    @Override
    public String getSqlValue(Node node, int part)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getName(), part);

        IRI iri = (IRI) node;

        return "'" + iri.getUri().toString().replaceAll("'", "''") + "'::varchar";
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        String code = variableAccessor.variableAccess(variable, this, 0);

        if(rdfbox)
            code = "sparql.cast_as_rdfbox_from_iri(" + code + ")";

        return code;
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return this;
    }


    @Override
    public boolean match(Node node)
    {
        return false;
    }


    @Override
    public PatternResourceClass getPatternResourceClass()
    {
        return this;
    }


    @Override
    public String getSqlPatternValue(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        return "sparql.rdfbox_extract_iri" + "(" + column + ")";
    }
}

package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.ArrayList;
import java.util.Collections;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class InternalResourceClass extends ResourceClass
{
    public InternalResourceClass(int size)
    {
        super("internal", new ArrayList<String>(Collections.nCopies(size, "any")),
                new ArrayList<ResultTag>(Collections.nCopies(size, ResultTag.NULL)));
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return this;
    }


    @Override
    public String getPatternCode(Node node, int part)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public String getGeneralisedPatternCode(String table, String var, int part, boolean check)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public String getSpecialisedPatternCode(String table, String var, int part)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean match(Node node, Request request)
    {
        throw new UnsupportedOperationException();
    }
}

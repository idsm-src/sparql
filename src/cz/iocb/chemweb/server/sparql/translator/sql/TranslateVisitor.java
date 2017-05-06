package cz.iocb.chemweb.server.sparql.translator.sql;

import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Bind;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Filter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Graph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GroupGraph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Minus;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Optional;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Service;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Union;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;



public class TranslateVisitor extends ElementVisitor<TranslatedSegment>
{
    @Override
    public TranslatedSegment visit(SelectQuery selectQuery)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Select select)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(GroupGraph groupGraph)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Graph graph)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Service service)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Union union)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Values values)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Triple triple)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Minus minus)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Optional optional)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Bind bind)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Filter filter)
    {
        return null;
    }
}

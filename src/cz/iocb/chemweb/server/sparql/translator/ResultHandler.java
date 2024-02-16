package cz.iocb.chemweb.server.sparql.translator;

import java.util.Map;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlIntercode;



public abstract class ResultHandler
{
    public abstract void add(Map<String, Node> row);

    public abstract SqlIntercode get();

    public abstract int size();
}

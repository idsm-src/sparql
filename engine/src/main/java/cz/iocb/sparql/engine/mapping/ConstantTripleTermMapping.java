package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.request.Request;



/**
 * Mapping of a position to a fixed triple term, whose class (the triple term class of the most specific classes of its
 * components) and columns are detected lazily, as the IRIs inside may belong to user classes.
 */
public class ConstantTripleTermMapping extends ConstantMapping
{
    /**
     * Creates the mapping.
     *
     * @param term the triple term
     */
    public ConstantTripleTermMapping(TripleTerm term)
    {
        super(term, null, null);
    }


    @Override
    public ResourceClass getResourceClass(Request request)
    {
        return request.getTripleTermClass((TripleTerm) value);
    }


    @Override
    public List<Column> getColumns(Request request)
    {
        return request.getColumns(getResourceClass(request), value);
    }


    /**
     * The triple term.
     *
     * @return the triple term
     */
    public TripleTerm getTripleTerm()
    {
        return (TripleTerm) value;
    }
}

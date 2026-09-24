package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Mapping of a position to a fixed RDF term; matches that term only.
 */
public abstract class ConstantMapping extends TermMapping
{
    /**
     * The constant term.
     */
    protected final RdfTerm value;


    /**
     * Creates the mapping.
     *
     * @param value the term
     * @param resourceClass the resource class
     * @param columns the columns
     */
    protected ConstantMapping(RdfTerm value, ResourceClass resourceClass, List<Column> columns)
    {
        super(resourceClass, columns);
        this.value = value;
    }


    /**
     * The constant term.
     *
     * @return the constant term
     */
    public RdfTerm getValue()
    {
        return value;
    }


    @Override
    public boolean match(Request request, RdfTerm term)
    {
        if(term instanceof Variable)
            return true;

        return value.equals(term);
    }


    @Override
    public TermMapping remap(List<ColumnPair> columnMap)
    {
        return this;
    }


    @Override
    public int hashCode()
    {
        return value.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        ConstantMapping other = (ConstantMapping) object;

        return value.equals(other.value);
    }
}

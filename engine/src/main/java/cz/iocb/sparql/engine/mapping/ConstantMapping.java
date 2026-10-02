package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TripleTerm;
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


    /**
     * True if the term matches the fixed term: a variable matches anything, a triple term matches a triple term
     * component by component (so the variables inside a triple term of a pattern unify with the components), another
     * term by equality.
     */
    @Override
    public boolean match(Request request, RdfTerm term)
    {
        return matches(term, value);
    }


    /**
     * True if the pattern term matches the value, see {@link #match}.
     *
     * @param pattern the pattern term
     * @param value the value
     * @return true if the pattern term matches the value, false otherwise
     */
    private static boolean matches(RdfTerm pattern, RdfTerm value)
    {
        if(pattern instanceof Variable)
            return true;

        if(pattern instanceof TripleTerm p && value instanceof TripleTerm v)
            return matches(p.getSubject(), v.getSubject()) && matches(p.getPredicate(), v.getPredicate())
                    && matches(p.getObject(), v.getObject());

        return value.equals(pattern);
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

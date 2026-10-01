package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strBlankNode;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Resource class of blank nodes with a string value; their result class is {@link BuiltinClasses#strBlankNode}.
 */
public sealed abstract class StrBlankNodeClass extends BlankNodeClass
        permits StrBlankNodeCompositeClass, StrBlankNodeInSegmentClass, StrBlankNodeScalarClass
{
    /**
     * Creates the class with its name, column types and superclasses.
     *
     * @param name the name
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected StrBlankNodeClass(String name, List<SqlType> sqlTypes, Set<PrimitiveResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    /**
     * Constant columns representing the blank node.
     *
     * @param bnode the blank node
     * @return the constant columns
     */
    public abstract List<Column> toColumns(StrBlankNode bnode);


    /**
     * True if the blank node belongs to this class.
     *
     * @param request the current request
     * @param term the RDF term
     * @return true if the blank node belongs to this class, false otherwise
     */
    public abstract boolean match(Request request, StrBlankNode term);


    @Override
    public final ResourceClass getResultResourceClass()
    {
        return strBlankNode;
    }


    @Override
    public final boolean match(Request request, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case StrBlankNode bnode -> match(request, bnode);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(BlankNode term)
    {
        if(term instanceof StrBlankNode bnode)
            return toColumns(bnode);
        else
            throw new IllegalArgumentException();
    }
}

package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intBlankNode;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Resource class of blank nodes with an integer value; their result class is {@link BuiltinClasses#intBlankNode}.
 */
public sealed abstract class IntBlankNodeClass extends BlankNodeClass
        permits IntBlankNodeCompositeClass, IntBlankNodeInSegmentClass, IntBlankNodeScalarClass
{
    /**
     * Creates the class with its name, column types and superclasses.
     *
     * @param name the name
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected IntBlankNodeClass(String name, List<SqlType> sqlTypes, Set<PrimitiveResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    /**
     * Constant columns representing the blank node.
     *
     * @param bnode the blank node
     * @return the constant columns
     */
    public abstract List<Column> toColumns(IntBlankNode bnode);


    /**
     * True if the blank node belongs to this class.
     *
     * @param request the current request
     * @param term the RDF term
     * @return true if the blank node belongs to this class, false otherwise
     */
    public abstract boolean match(Request request, IntBlankNode term);


    @Override
    public final ResourceClass getResultResourceClass()
    {
        return intBlankNode;
    }


    @Override
    public final boolean match(Request request, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case IntBlankNode bnode -> match(request, bnode);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(BlankNode term)
    {
        if(term instanceof IntBlankNode bnode)
            return toColumns(bnode);
        else
            throw new IllegalArgumentException();
    }
}

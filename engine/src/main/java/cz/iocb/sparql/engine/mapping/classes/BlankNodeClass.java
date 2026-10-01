package cz.iocb.sparql.engine.mapping.classes;

import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Request;



/**
 * Resource class of blank nodes.
 */
public sealed abstract class BlankNodeClass extends PrimitiveResourceClass permits IntBlankNodeClass, StrBlankNodeClass
{
    /**
     * Creates the class with its name, column types and superclasses.
     *
     * @param name the name
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected BlankNodeClass(String name, List<SqlType> sqlTypes, Set<PrimitiveResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    /**
     * Constant columns representing the blank node.
     *
     * @param bnode the blank node
     * @return the constant columns
     */
    public abstract List<Column> toColumns(BlankNode bnode);


    @Override
    public final List<Column> toColumns(Request request, RdfTerm term)
    {
        if(term instanceof BlankNode bnode)
            return toColumns(bnode);
        else
            throw new IllegalArgumentException();
    }
}

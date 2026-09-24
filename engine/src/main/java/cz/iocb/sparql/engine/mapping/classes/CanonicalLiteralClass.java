package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Literal class holding only literals in the canonical lexical form of their datatype, so no lexical column is needed.
 */
public abstract class CanonicalLiteralClass extends LiteralClass
{
    /**
     * Creates the class with its name, datatype, column types and superclasses.
     *
     * @param name the name
     * @param datatype the datatype
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected CanonicalLiteralClass(String name, Datatype datatype, List<String> sqlTypes,
            Set<PrimitiveResourceClass> superClasses)
    {
        super(name, datatype, sqlTypes, superClasses);
    }


    @Override
    public boolean match(Statement statement, Literal literal)
    {
        if(!datatype.getTypeIri().equals(literal.getType()))
            return false;

        if(!datatype.isValidForm(literal.getValue()))
            return false;

        if(!datatype.isCanonicalForm(literal.getValue()))
            return false;

        return true;
    }
}

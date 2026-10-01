package cz.iocb.sparql.engine.mapping.classes;

import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.request.Request;



/**
 * Literal class holding only literals in the canonical lexical form of their datatype, so no lexical column is needed.
 */
public sealed abstract class CanonicalLiteralClass extends LiteralClass
        permits SimpleLiteralClass, DateCompositeClass, DateInZoneClass, DateScalarClass, DateTimeCompositeClass,
        DateTimeInZoneClass, DateTimeScalarClass, DirLangStringClass, DirLangStringWithTagClass, LangStringClass,
        LangStringWithTagClass, UserLiteralClass, SubsetLiteralClass
{
    /**
     * Creates the class with its name, datatype, column types and superclasses.
     *
     * @param name the name
     * @param datatype the datatype
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected CanonicalLiteralClass(String name, Datatype datatype, List<SqlType> sqlTypes,
            Set<PrimitiveResourceClass> superClasses)
    {
        super(name, datatype, sqlTypes, superClasses);
    }


    /**
     * Creates the class with its name, datatype, column types, superclasses and the class that effectively represents
     * it.
     *
     * @param name the name
     * @param datatype the datatype
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     * @param effectiveClass primitive class whose columns store the values, or null for the class itself
     */
    protected CanonicalLiteralClass(String name, Datatype datatype, List<SqlType> sqlTypes,
            Set<PrimitiveResourceClass> superClasses, PrimitiveResourceClass effectiveClass)
    {
        super(name, datatype, sqlTypes, superClasses, effectiveClass);
    }


    @Override
    public boolean match(Request request, Literal literal)
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

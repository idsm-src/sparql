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


    /**
     * Base class of this class: the class holding the literals of the datatype of this class in every valid lexical
     * form, keeping the value in the same leading columns as this class, followed by the lexical column (see
     * {@link BaseLiteralClass}); this class itself when a value of the datatype has a single lexical form, as for the
     * strings and the language-tagged strings. The base class of a subset class is that of its original, because a
     * subset narrows the literals a deployment holds, not the values (see {@link SubsetLiteralClass}).
     *
     * @return base class of this class
     */
    public abstract LiteralClass getBaseClass();
}

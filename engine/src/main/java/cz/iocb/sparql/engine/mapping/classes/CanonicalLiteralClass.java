package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;



public abstract class CanonicalLiteralClass extends LiteralClass
{
    protected CanonicalLiteralClass(String name, Datatype datatype, List<String> sqlTypes,
            Set<PrimitiveResourceClass> superClasses)
    {
        super(name, datatype, sqlTypes, superClasses);
    }


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

package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;



public abstract class BaseLiteralClass extends LiteralClass
{
    protected BaseLiteralClass(String name, Datatype datatype, List<String> sqlTypes,
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

        return true;
    }


    protected Column getLexicalColumn(Literal literal)
    {
        if(datatype.isCanonicalForm(literal.getValue()))
            return constant("", "varchar");

        return constant(literal.getValue(), "varchar");
    }
}

package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;



public abstract class LiteralClass extends PrimitiveResourceClass
{
    final protected Datatype datatype;


    protected LiteralClass(String name, Datatype datatype, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
        this.datatype = datatype;
    }


    public abstract List<Column> toColumns(Literal literal);


    public boolean match(Statement statement, Literal literal)
    {
        if(!datatype.getTypeIri().equals(literal.getType()))
            return false;

        if(!datatype.isValidForm(literal.getValue()))
            return false;

        return true;
    }


    @Override
    public final boolean match(Statement statement, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case Literal literal -> match(statement, literal);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(Statement statement, RdfTerm term)
    {
        if(term instanceof Literal literal)
            return toColumns(literal);
        else
            throw new IllegalArgumentException();
    }


    public final Datatype getDatatype()
    {
        return datatype;
    }


    public final Iri getTypeIri()
    {
        return datatype != null ? datatype.getTypeIri() : null;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        return true;
    }
}

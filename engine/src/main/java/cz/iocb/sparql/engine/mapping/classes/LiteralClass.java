package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class LiteralClass extends PrimitiveResourceClass
{
    protected final IRI typeIri;


    protected LiteralClass(String name, IRI typeIri, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
        this.typeIri = typeIri;
    }


    public abstract List<Column> toColumns(Literal literal);


    public boolean match(Statement statement, Literal literal)
    {
        IRI literalTypeIri = literal.getTypeIri();

        if(typeIri == null)
            return !literal.isTypeSupported() || literal.getValue() == null;
        else if(literal.getValue() == null)
            return false;
        else if(typeIri.equals(literalTypeIri))
            return !typeIri.equals(rdfLangString.getTypeIri()) || literal.getLanguageTag() == null;
        else
            return false;
    }


    @Override
    public final boolean match(Statement statement, Node node)
    {
        return switch(node)
        {
            case VariableOrBlankNode _ -> true;
            case Literal literal -> match(statement, literal);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(Statement statement, Node node)
    {
        if(node instanceof Literal literal)
            return toColumns(literal);
        else
            throw new IllegalArgumentException();
    }


    public final IRI getTypeIri()
    {
        return typeIri;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        LiteralClass other = (LiteralClass) object;

        if(!typeIri.equals(other.typeIri))
            return false;

        return true;
    }
}

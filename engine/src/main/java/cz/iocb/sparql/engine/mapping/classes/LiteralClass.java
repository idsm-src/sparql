package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import java.sql.Statement;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class LiteralClass extends ResourceClass
{
    protected final IRI typeIri;


    protected LiteralClass(String name, List<String> sqlTypes, IRI typeIri)
    {
        super(name, sqlTypes);
        this.typeIri = typeIri;
    }


    public abstract List<Column> toColumns(Node node);


    @Override
    public final List<Column> toColumns(Statement statement, Node node)
    {
        return toColumns(node);
    }


    @Override
    public boolean match(Statement statement, Node node)
    {
        return switch(node)
        {
            case VariableOrBlankNode var -> true;
            case Literal literal ->
            {
                IRI literalTypeIri = literal.getTypeIri();

                if(typeIri == null)
                    yield !literal.isTypeSupported() || literal.getValue() == null;
                else if(literal.getValue() == null)
                    yield false;
                else if(typeIri.equals(literalTypeIri))
                    yield !typeIri.equals(rdfLangString.getTypeIri()) || literal.getLanguageTag() == null;
                else
                    yield false;
            }
            default -> false;
        };
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

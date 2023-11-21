package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.iri;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public abstract class IriClass extends ResourceClass
{
    protected IriClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        super(name, sqlTypes, resultTags);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return iri;
    }


    @Override
    public Column toExpression(Node node)
    {
        return new ConstantColumn("'" + ((IRI) node).getValue().replaceAll("'", "''") + "'::varchar");
    }


    @Override
    public String fromGeneralExpression(String code)
    {
        return code;
    }


    @Override
    public String toGeneralExpression(String code)
    {
        return code;
    }


    @Override
    public String toBoxedExpression(String code)
    {
        return "sparql.rdfbox_create_from_iri(" + code + ")";
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        return "sparql.rdfbox_get_iri(" + code + ")";
    }
}

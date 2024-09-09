package cz.iocb.sparql.engine.translator.imcode.expression;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlIri extends SqlNodeValue
{
    private final IRI iri;
    private final IriClass iriClass;


    protected SqlIri(IRI iri, IriClass resourceClass)
    {
        super(asSet(resourceClass), false);
        this.iriClass = resourceClass;
        this.iri = iri;
    }


    public static SqlExpressionIntercode create(Request request, IRI iri)
    {
        return new SqlIri(iri, request.getIriClass(iri));
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables)
    {
        return this;
    }


    @Override
    public String translate(Request request)
    {
        return BuiltinClasses.iri.toColumns(request.getStatement(), iri).get(0).toString();
    }


    @Override
    public List<Column> asResource(Request request, ResourceClass resourceClass)
    {
        return resourceClass.toColumns(request.getStatement(), iri);
    }


    public IRI getIri()
    {
        return iri;
    }


    public IriClass getIriClass()
    {
        return iriClass;
    }
}

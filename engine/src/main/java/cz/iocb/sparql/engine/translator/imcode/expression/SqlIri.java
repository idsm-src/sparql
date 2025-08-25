package cz.iocb.sparql.engine.translator.imcode.expression;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public final class SqlIri extends SqlNodeValue
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
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        return new Restrictions();
    }

    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        return this;
    }


    @Override
    public String translate(Request request)
    {
        return BuiltinClasses.iri.toExpression(request.getStatement(), iri).toString();
    }


    @Override
    public List<Column> asResource(Request request, ResourceClass resourceClass)
    {
        if(!resourceClass.match(request.getStatement(), iri))
            return resourceClass.getSqlTypes().stream().map(t -> (Column) new ConstantColumn(null, t)).toList();

        return request.getColumns(resourceClass, iri);
    }


    public IRI getIri()
    {
        return iri;
    }


    public IriClass getIriClass()
    {
        return iriClass;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append(iri);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlIri imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(iri, imcode.iri))
            return false;

        if(iriClass != imcode.iriClass)
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(iri);
    }
}

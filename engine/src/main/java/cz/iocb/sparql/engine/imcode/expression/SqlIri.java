package cz.iocb.sparql.engine.imcode.expression;

import static java.util.Collections.singletonMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlIri extends SqlExpressionIntercode
{
    private final Iri iri;


    private SqlIri(Iri iri, Map<ResourceClass, List<Column>> map)
    {
        super(map, false, true);

        this.iri = iri;
    }


    public static SqlExpressionIntercode create(Request request, Iri iri)
    {
        return create(request, iri, Restriction.ALL);
    }


    private static SqlExpressionIntercode create(Request request, Iri iri, Restriction restriction)
    {
        IriClass resClass = request.getIriClass(iri);

        List<Column> columns = restriction.contains(resClass) ? request.getColumns(resClass, iri) : null;

        return new SqlIri(iri, singletonMap(resClass, columns));
    }


    @Override
    public Restrictions getRequirements()
    {
        return new Restrictions();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        if(restriction.isOptimized(variableBinding))
            return this;

        return create(request, iri, restriction);
    }


    public Iri getIri()
    {
        return iri;
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

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(iri);
    }
}

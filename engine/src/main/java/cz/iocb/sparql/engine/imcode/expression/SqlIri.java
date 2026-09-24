package cz.iocb.sparql.engine.imcode.expression;

import static java.util.Collections.singletonMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Constant IRI, represented in its detected IRI class.
 */
public final class SqlIri extends SqlExpressionIntercode
{
    /**
     * The IRI.
     */
    private final Iri iri;


    /**
     * Creates the expression.
     *
     * @param iri the IRI
     * @param map columns per resource class
     */
    private SqlIri(Iri iri, Map<ResourceClass, List<Column>> map)
    {
        super(map, false, true);

        this.iri = iri;
    }


    /**
     * Constant IRI expression.
     *
     * @param request the current request
     * @param iri the IRI
     * @return constant IRI expression
     */
    public static SqlExpressionIntercode create(Request request, Iri iri)
    {
        return create(request, iri, Restriction.ALL);
    }


    /**
     * Constant IRI expression materialising its columns only when its class is needed.
     *
     * @param request the current request
     * @param iri the IRI
     * @param restriction the result classes the parent needs
     * @return constant IRI expression materialising its columns only when its class is needed
     */
    private static SqlExpressionIntercode create(Request request, Iri iri, Restriction restriction)
    {
        ResourceClass resClass = request.getIriClass(iri);

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


    /**
     * The IRI.
     *
     * @return the IRI
     */
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
    public Set<VirtualTable> getVirtualTables()
    {
        return Set.of();
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(iri);
    }
}

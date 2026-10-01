package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



/**
 * Any IRI, stored as its full text in one varchar column; the result class of all IRIs.
 */
public final class IriScalarClass extends IriClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected IriScalarClass()
    {
        super("iri", List.of(VARCHAR), Set.of(box));
    }


    @Override
    public boolean match(Request request, Iri iri)
    {
        return true;
    }


    @Override
    public List<Column> toColumns(Request request, Iri iri)
    {
        return List.of(constant(iri.getValue(), VARCHAR));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column iri = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression(RDFBOX, "sparql.rdfbox_create_from_iri(%s)", iri));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        if(sourceClass.equals(box))
            return List.of(expression(VARCHAR, "sparql.rdfbox_get_iri(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return columns.get(0) instanceof ValueColumn col ? col.getValue() : "";
    }
}

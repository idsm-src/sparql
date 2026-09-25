package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * IRIs that belong to no user IRI class, stored as full text. It is never matched against terms directly (the request
 * assigns it when detection by the user classes fails), and a value of a more general class can be narrowed to it only
 * when representability need not be checked.
 */
public class UnsupportedIriClass extends IriClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected UnsupportedIriClass()
    {
        super("unsupported", List.of(VARCHAR), Set.of(box, iri));
    }


    @Override
    public boolean match(Statement statement, Iri iri)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        if(columns.get(0) instanceof ValueColumn col)
            return col.getValue();

        return "";
    }


    @Override
    public List<Column> toColumns(Statement statement, Iri iri)
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

        Column value = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_iri(%s)", value));

        if(targetClass.equals(iri))
            return columns;

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
        {
            if(checkOptional)
                return List.of(expression("sparql.rdfbox_get_iri(%s)", columns.get(0)));
            else
                throw new UnsupportedOperationException();
        }

        if(sourceClass.equals(iri))
        {
            if(checkOptional)
                return columns;
            else
                throw new UnsupportedOperationException();
        }

        throw new IllegalArgumentException();
    }
}

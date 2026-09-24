package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;



/**
 * User IRI class with a single column; subclasses provide the SQL expressions rebuilding the IRI from the column and
 * extracting the column from the IRI.
 */
public abstract class SimpleUserIriClass extends UserIriClass
{
    /**
     * Creates the class with a single column of the SQL type.
     *
     * @param name the name
     * @param sqlType the SQL type
     */
    protected SimpleUserIriClass(String name, String sqlType)
    {
        super(name, List.of(sqlType), Set.of(iri, box));
    }


    /**
     * SQL expression computing the full IRI from the column value.
     *
     * @param column the column
     * @return SQL expression computing the full IRI from the column value
     */
    protected abstract Column generateFunction(Column column);


    /**
     * SQL expression extracting the column value from an IRI; with {@code check}, it yields NULL for IRIs outside the
     * class.
     *
     * @param column the column
     * @param check whether IRIs outside the class must yield NULL
     * @return SQL expression extracting the column value from an IRI; with {@code check}, it yields NULL for IRIs
     *         outside the class
     */
    protected abstract Column generateInverseFunction(Column column, boolean check);


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column value = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_iri(%s)", generateFunction(value)));

        if(targetClass.equals(iri))
            return List.of(generateFunction(value));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        boolean check = !checkOptional && !getIntersectionClass(Set.of(superClass, iri)).equals(this);

        if(sourceClass.equals(box))
            List.of(generateInverseFunction(expression("sparql.rdfbox_get_iri(%s)", columns.get(0)), check));

        if(sourceClass.equals(iri))
            return List.of(generateInverseFunction(columns.get(0), check));

        throw new IllegalArgumentException();
    }
}

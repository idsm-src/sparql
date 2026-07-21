package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;



public abstract class SimpleUserIriClass extends UserIriClass
{
    protected SimpleUserIriClass(String name, String sqlType)
    {
        super(name, List.of(sqlType), Set.of(iri, box));
    }


    protected abstract Column generateFunction(Column column);


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
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        boolean check = !getIntersectionClass(Set.of(superClass, iri)).equals(this);

        if(sourceClass.equals(box))
            List.of(generateInverseFunction(expression("sparql.rdfbox_get_iri(%s)", columns.get(0)), check));

        if(sourceClass.equals(iri))
            return List.of(generateInverseFunction(columns.get(0), check));

        throw new IllegalArgumentException();
    }
}

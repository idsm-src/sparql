package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.rdf.Iri;



public final class CommonIriClass extends IriClass implements ResultResourceClass
{
    protected CommonIriClass()
    {
        super("iri", List.of("varchar"), Set.of(box));
    }


    @Override
    public boolean match(Statement statement, Iri iri)
    {
        return true;
    }


    @Override
    public List<Column> toColumns(Statement statement, Iri iri)
    {
        return List.of(constant(iri.getValue(), "varchar"));
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
            return List.of(expression("sparql.rdfbox_create_from_iri(%s)", iri));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        if(sourceClass.equals(box))
            return List.of(expression("sparql.rdfbox_get_iri(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return columns.get(0) instanceof ConstantColumn col ? col.getValue() : "";
    }
}

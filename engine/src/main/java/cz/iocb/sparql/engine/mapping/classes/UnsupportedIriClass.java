package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.parser.model.IRI;



public class UnsupportedIriClass extends IriClass
{
    protected UnsupportedIriClass()
    {
        super("unsupported", List.of("varchar"), Set.of(box, iri));
    }


    @Override
    public boolean match(Statement statement, IRI iri)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        if(columns.get(0) instanceof ConstantColumn col)
            return col.getValue();

        return "";
    }


    @Override
    public List<Column> toColumns(Statement statement, IRI iri)
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

        Column value = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_iri(%s)", value));

        if(targetClass.equals(iri))
            return columns;

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
            throw new UnsupportedOperationException();

        if(sourceClass.equals(iri))
            throw new UnsupportedOperationException();

        throw new IllegalArgumentException();
    }
}

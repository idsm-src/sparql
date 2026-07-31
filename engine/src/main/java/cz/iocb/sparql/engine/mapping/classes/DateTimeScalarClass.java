package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDateTime;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;



public final class DateTimeScalarClass extends LiteralClass
{
    protected DateTimeScalarClass()
    {
        super("datetime", xsdDateTimeType, List.of("sparql.zoneddatetime"), Set.of(box/*, xsdCompositeDateTime*/));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(xsdCompositeDateTime);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        //TODO: canonization will not be needed when special resource classes for canonical literals are introduced
        return List.of(constant(datatype.getCanonicalLexicalForm(literal.getValue()), sqlTypes.get(0)));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column time = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_datetime(%s)", time));

        //if(targetClass.equals(xsdCompositeDateTime))
        //    return List.of(expression("sparql.zoneddatetime_get_value(%s)", time),
        //            expression("sparql.zoneddatetime_get_zone(%s)", time));

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
            return List.of(expression("sparql.rdfbox_get_datetime(%s)", columns.get(0)));

        //if(sourceClass.equals(xsdCompositeDateTime))
        //    return List.of(expression("sparql.zoneddatetime_create(%s, %s)", columns.get(0), columns.get(1)));

        throw new IllegalArgumentException();
    }
}

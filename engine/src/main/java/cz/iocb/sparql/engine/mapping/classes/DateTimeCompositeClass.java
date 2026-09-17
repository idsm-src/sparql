package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.DateTimeDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



public final class DateTimeCompositeClass extends CanonicalLiteralClass implements ResultResourceClass
{
    protected DateTimeCompositeClass()
    {
        super("datetime@2c", xsdDateTimeType, List.of("timestamptz", "int4"),
                Set.of(box, genScalarDateTime, genDateTime, xsdScalarDateTime));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(DateTimeDatatype.getDateTime(literal), sqlTypes.get(0)),
                constant(DateTimeDatatype.getZone(literal), sqlTypes.get(1)));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column time = columns.get(0);
        Column zone = columns.get(1);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_datetime(%s, %s)", time, zone));

        if(targetClass.equals(genScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_create(%s, %s)", time, zone), !canBeNull ?
                    constant("", "varchar") : expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", time));

        if(targetClass.equals(genDateTime))
            return List.of(time, zone, !canBeNull ? constant("", "varchar") :
                    expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", time));

        if(targetClass.equals(xsdScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_create(%s, %s)", time, zone));

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
            return List.of(expression("sparql.rdfbox_get_datetime_value(%s, false)", columns.get(0)),
                    expression("sparql.rdfbox_get_datetime_zone(%s, false)", columns.get(0)));

        if(sourceClass.equals(genScalarDateTime))
            return List.of(
                    expression("(CASE %s WHEN '' THEN sparql.zoneddatetime_get_value(%s) END)", columns.get(1),
                            columns.get(0)),
                    expression("(CASE %s WHEN '' THEN sparql.zoneddatetime_get_zone(%s) END)", columns.get(1),
                            columns.get(0)));

        if(sourceClass.equals(genDateTime))
            return List.of(expression("(CASE %s WHEN '' THEN %s END)", columns.get(2), columns.get(0)),
                    expression("(CASE %s WHEN '' THEN %s END)", columns.get(2), columns.get(1)));

        if(sourceClass.equals(xsdScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_get_value(%s)", columns.get(0)),
                    expression("sparql.zoneddatetime_get_zone(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }
}

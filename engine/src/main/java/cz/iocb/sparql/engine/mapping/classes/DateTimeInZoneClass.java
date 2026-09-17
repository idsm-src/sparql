package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.DateTimeDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



public final class DateTimeInZoneClass extends CanonicalLiteralClass implements DateTimeInZone
{
    private static final ConcurrentMap<Integer, DateTimeInZoneClass> instances = new ConcurrentHashMap<>();

    private final LiteralClass base;
    private final int zone;


    private DateTimeInZoneClass(int zone)
    {
        super("datetime$" + zone, xsdDateTimeType, List.of("timestamptz"),
                Set.of(box, genScalarDateTime, genDateTime, xsdScalarDateTime, xsdDateTime));

        this.base = DateTimeInZoneBaseClass.get(zone);
        this.zone = zone;
    }


    public static DateTimeInZoneClass get(int zone)
    {
        return instances.computeIfAbsent(zone, DateTimeInZoneClass::new);
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(xsdDateTime);
    }


    @Override
    public boolean match(Statement statement, Literal literal)
    {
        return super.match(statement, literal) && DateTimeDatatype.getZone(literal) == zone;
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(DateTimeDatatype.getDateTime(literal), sqlTypes.get(0)));
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
            return List.of(expression("sparql.rdfbox_create_from_datetime(%s, '%d'::int4)", time, zone));

        if(targetClass.equals(genScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_create(%s, '%d'::int4)", time, zone), !canBeNull ?
                    constant("", "varchar") : expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", time));

        if(targetClass.equals(genDateTime))
            return List.of(time,
                    !canBeNull ? constant(zone, "int4") :
                            expression("CASE WHEN %s IS NOT NULL THEN '%d'::int4 END", time, zone),
                    !canBeNull ? constant("", "varchar") :
                            expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", time));

        if(targetClass.equals(xsdScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_create(%s, '%d'::int4)", time, zone));

        if(targetClass.equals(xsdDateTime))
            return List.of(time, !canBeNull ? constant(zone, "int4") :
                    expression("CASE WHEN %s IS NOT NULL THEN '%d'::int4 END", time, zone));

        if(targetClass.equals(base))
            return List.of(time, !canBeNull ? constant("", "varchar") :
                    expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", time));

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
            return List.of(expression("sparql.rdfbox_get_datetime_value_of_zone(%s, '%d'::int4, false)", columns.get(0),
                    zone));

        if(sourceClass.equals(genScalarDateTime))
            return List
                    .of(expression("CASE WHEN %s = '' THEN sparql.zoneddatetime_get_value_of_zone(%s, '%d'::int4) END",
                            columns.get(1), columns.get(0), zone));

        if(sourceClass.equals(genDateTime))
            return List.of(expression("CASE WHEN %s = '' AND %s = '%d'::int4 THEN %s END", columns.get(2),
                    columns.get(1), zone, columns.get(0)));

        if(sourceClass.equals(xsdScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_get_value_of_zone(%s, '%d'::int4)", columns.get(0), zone));

        if(sourceClass.equals(xsdDateTime))
            return List.of(expression("CASE WHEN %s = '%d'::int4 THEN %s END", columns.get(1), zone, columns.get(0)));

        if(sourceClass.equals(base))
            return List.of(expression("CASE WHEN %s = '' THEN %s END", columns.get(1), columns.get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public int getZone()
    {
        return zone;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        DateTimeInZoneClass other = (DateTimeInZoneClass) object;

        return Objects.equals(zone, other.zone);
    }
}

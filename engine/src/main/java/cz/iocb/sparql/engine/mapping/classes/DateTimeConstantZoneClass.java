package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdDateTimeIri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public final class DateTimeConstantZoneClass extends LiteralClass
{
    private static final ConcurrentMap<Integer, DateTimeConstantZoneClass> instances = new ConcurrentHashMap<>();

    private final int zone;


    private DateTimeConstantZoneClass(int zone)
    {
        super("datetime$" + zone, xsdDateTimeIri, List.of("timestamptz"),
                Set.of(box, xsdScalarDateTime, xsdCompositeDateTime));
        this.zone = zone;
    }


    public static DateTimeConstantZoneClass get(int zone)
    {
        return instances.computeIfAbsent(zone, DateTimeConstantZoneClass::new);
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(xsdCompositeDateTime);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(DateTimeCompositeClass.getDateTime(literal), "timestamptz"));
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

        if(targetClass.equals(xsdScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_create(%s, '%d'::int4)", time, zone));

        if(targetClass.equals(xsdCompositeDateTime))
            return List.of(time, !canBeNull ? constant(zone, "int4") :
                    expression("CASE WHEN %s IS NOT NULL THEN '%d'::int4 END", time, zone));

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
            return List
                    .of(expression("sparql.rdfbox_get_datetime_value_of_zone(%s, '%d'::int4)", columns.get(0), zone));

        if(sourceClass.equals(xsdScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_get_value_of_zone(%s, '%d'::int4)", columns.get(0), zone));

        if(sourceClass.equals(xsdCompositeDateTime))
            return List.of(expression("CASE WHEN %s = '%d'::int4 THEN %s END", columns.get(1), zone, columns.get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public boolean match(Statement statement, Literal literal)
    {
        return super.match(statement, literal) && DateTimeCompositeClass.getZone(literal) == zone;
    }


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

        DateTimeConstantZoneClass other = (DateTimeConstantZoneClass) object;

        return Objects.equals(zone, other.zone);
    }
}

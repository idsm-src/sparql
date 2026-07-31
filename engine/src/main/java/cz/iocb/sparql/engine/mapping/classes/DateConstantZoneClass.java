package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateType;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;



public final class DateConstantZoneClass extends LiteralClass
{
    private static final ConcurrentMap<Integer, DateConstantZoneClass> instances = new ConcurrentHashMap<>();

    private final int zone;


    private DateConstantZoneClass(int zone)
    {
        super("date$" + zone, xsdDateType, List.of("date"), Set.of(box, xsdScalarDate, xsdCompositeDate));
        this.zone = zone;
    }


    public static DateConstantZoneClass get(int zone)
    {
        return instances.computeIfAbsent(zone, DateConstantZoneClass::new);
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(xsdCompositeDate);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(DateCompositeClass.getDate(literal), "date"));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column date = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_date(%s, '%d'::int4)", date, zone));

        if(targetClass.equals(xsdScalarDate))
            return List.of(expression("sparql.zoneddate_create(%s, '%d'::int4)", date, zone));

        if(targetClass.equals(xsdCompositeDate))
            return List.of(date, !canBeNull ? constant(zone, "int4") :
                    expression("CASE WHEN %s IS NOT NULL THEN '%d'::int4 END", date, zone));

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
            return List.of(expression("sparql.rdfbox_get_date_value_of_zone(%s, '%d'::int4)", columns.get(0), zone));

        if(sourceClass.equals(xsdScalarDate))
            return List.of(expression("sparql.zoneddate_get_value_of_zone(%s, '%d'::int4)", columns.get(0), zone));

        if(sourceClass.equals(xsdCompositeDate))
            return List.of(expression("CASE WHEN %s = '%d'::int4 THEN %s END", columns.get(1), zone, columns.get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public boolean match(Statement statement, Literal literal)
    {
        return super.match(statement, literal) && DateCompositeClass.getZone(literal) == zone;
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

        DateConstantZoneClass other = (DateConstantZoneClass) object;

        return Objects.equals(zone, other.zone);
    }
}

package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
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



public final class DateTimeInZoneBaseClass extends BaseLiteralClass implements DateTimeInZone
{
    private static final ConcurrentMap<Integer, DateTimeInZoneBaseClass> instances = new ConcurrentHashMap<>();

    private final int zone;


    private DateTimeInZoneBaseClass(int zone)
    {
        super("base-datetime$" + zone, xsdDateTimeType, List.of("timestamptz", "varchar"),
                Set.of(box, genScalarDateTime, genDateTime));
        this.zone = zone;
    }


    public static DateTimeInZoneBaseClass get(int zone)
    {
        return instances.computeIfAbsent(zone, DateTimeInZoneBaseClass::new);
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(genDateTime);
    }


    @Override
    public boolean match(Statement statement, Literal literal)
    {
        return super.match(statement, literal) && DateTimeDatatype.getZone(literal) == zone;
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(DateTimeDatatype.getDateTime(literal), sqlTypes.get(0)), getLexicalColumn(literal));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column time = columns.get(0);
        Column lexical = columns.get(1);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_datetime_with_lexical(%s, '%d'::int4, %s)", time, zone,
                    lexical));

        if(targetClass.equals(genScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_create(%s, '%d'::int4)", time, zone), lexical);

        if(targetClass.equals(genDateTime))
            return List.of(time, !canBeNull ? constant(zone, "int4") :
                    expression("CASE WHEN %s IS NOT NULL THEN '%d'::int4 END", time, zone), lexical);

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
            return List.of(expression("sparql.rdfbox_get_datetime_value_of_zone(%s, '%d'::int4)", columns.get(0), zone),
                    expression("sparql.rdfbox_get_datetime_lexical_of_zone(%s, '%d'::int4)", columns.get(0), zone));

        if(sourceClass.equals(genScalarDateTime))
            return List
                    .of(expression("CASE WHEN %s != '' THEN sparql.zoneddatetime_get_value_of_zone(%s, '%d'::int4) END",
                            columns.get(1), columns.get(0), zone), columns.get(1));

        if(sourceClass.equals(genDateTime))
            return List.of(expression("CASE WHEN %s != '' AND %s = '%d'::int4 THEN %s END", columns.get(2),
                    columns.get(1), zone, columns.get(0)), columns.get(2));

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

        DateTimeInZoneBaseClass other = (DateTimeInZoneBaseClass) object;

        return Objects.equals(zone, other.zone);
    }
}

package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
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
import cz.iocb.sparql.engine.mapping.datatypes.DateDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Any valid xsd:date value of one fixed timezone offset, stored as a {@code date} plus its lexical form; instances are
 * cached per offset.
 */
public final class DateInZoneBaseClass extends BaseLiteralClass implements DateInZone
{
    /**
     * Instances by timezone offset.
     */
    private static final ConcurrentMap<Integer, DateInZoneBaseClass> instances = new ConcurrentHashMap<>();

    /**
     * Timezone offset in seconds.
     */
    private final int zone;


    /**
     * Creates the class of the offset; use {@link #get}.
     *
     * @param zone the timezone offset in seconds
     */
    private DateInZoneBaseClass(int zone)
    {
        super("base-date$" + zone, xsdDateType, List.of("date", "varchar"), Set.of(box, genScalarDate, genDate));
        this.zone = zone;
    }


    /**
     * The class of the given timezone offset (see {@link #getZone}).
     *
     * @param zone the timezone offset in seconds
     * @return the class of the given timezone offset (see {@link #getZone})
     */
    public static DateInZoneBaseClass get(int zone)
    {
        return instances.computeIfAbsent(zone, DateInZoneBaseClass::new);
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(genDate);
    }


    @Override
    public boolean match(Statement statement, Literal literal)
    {
        return super.match(statement, literal) && DateDatatype.getZone(literal) == zone;
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(DateDatatype.getDate(literal), sqlTypes.get(0)), getLexicalColumn(literal));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column date = columns.get(0);
        Column lexical = columns.get(1);

        if(targetClass.equals(box))
            return List.of(
                    expression("sparql.rdfbox_create_from_date_with_lexical(%s, '%d'::int4, %s)", date, zone, lexical));

        if(targetClass.equals(genScalarDate))
            return List.of(expression("sparql.zoneddate_create(%s, '%d'::int4)", date, zone), lexical);

        if(targetClass.equals(genDate))
            return List.of(date, !canBeNull ? constant(zone, "int4") :
                    expression("CASE WHEN %s IS NOT NULL THEN '%d'::int4 END", date, zone), lexical);

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
            return List.of(
                    expression("sparql.rdfbox_get_date_value_of_zone(%s, '%d'::int4, true)", columns.get(0), zone),
                    expression("sparql.rdfbox_get_date_lexical_of_zone(%s, '%d'::int4)", columns.get(0), zone));

        if(sourceClass.equals(genScalarDate))
            return List.of(expression("CASE WHEN %s != '' THEN sparql.zoneddate_get_value_of_zone(%s, '%d'::int4) END",
                    columns.get(1), columns.get(0), zone), columns.get(1));

        if(sourceClass.equals(genDate))
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

        DateInZoneBaseClass other = (DateInZoneBaseClass) object;

        return Objects.equals(zone, other.zone);
    }
}

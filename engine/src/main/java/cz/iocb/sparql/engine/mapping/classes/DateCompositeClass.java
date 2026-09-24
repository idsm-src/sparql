package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.DateDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Canonical xsd:date values as a {@code date} and an {@code int4} timezone offset; the result class of canonical dates.
 */
public final class DateCompositeClass extends CanonicalLiteralClass implements ResultResourceClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected DateCompositeClass()
    {
        super("date@2c", xsdDateType, List.of("date", "int4"), Set.of(box, genScalarDate, genDate, xsdScalarDate));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(DateDatatype.getDate(literal), sqlTypes.get(0)),
                constant(DateDatatype.getZone(literal), sqlTypes.get(1)));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column date = columns.get(0);
        Column zone = columns.get(1);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_date(%s, %s)", date, zone));

        if(targetClass.equals(genScalarDate))
            return List.of(expression("sparql.zoneddate_create(%s, %s)", date, zone), !canBeNull ?
                    constant("", "varchar") : expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", date));

        if(targetClass.equals(genDate))
            return List.of(date, zone, !canBeNull ? constant("", "varchar") :
                    expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", date));

        if(targetClass.equals(xsdScalarDate))
            return List.of(expression("sparql.zoneddate_create(%s, %s)", date, zone));

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
            return List.of(expression("sparql.rdfbox_get_date_value(%s, false)", columns.get(0)),
                    expression("sparql.rdfbox_get_date_zone(%s, false)", columns.get(0)));

        if(sourceClass.equals(genScalarDate))
            return List.of(
                    expression("(CASE %s WHEN '' THEN sparql.zoneddate_get_value(%s) END)", columns.get(1),
                            columns.get(0)),
                    expression("(CASE %s WHEN '' THEN sparql.zoneddate_get_zone(%s) END)", columns.get(1),
                            columns.get(0)));

        if(sourceClass.equals(genDate))
            return List.of(expression("(CASE %s WHEN '' THEN %s END)", columns.get(2), columns.get(0)),
                    expression("(CASE %s WHEN '' THEN %s END)", columns.get(2), columns.get(1)));

        if(sourceClass.equals(xsdScalarDate))
            return List.of(expression("sparql.zoneddate_get_value(%s)", columns.get(0)),
                    expression("sparql.zoneddate_get_zone(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }
}

package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.TIMESTAMPTZ;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.database.SqlType.ZONEDDATETIME;
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
import cz.iocb.sparql.engine.request.Request;



/**
 * Canonical xsd:dateTime values as a UTC {@code timestamptz} and an {@code int4} timezone offset; the result class of
 * canonical date-times.
 */
public final class DateTimeCompositeClass extends CanonicalLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected DateTimeCompositeClass()
    {
        super("datetime@2c", xsdDateTimeType, List.of(TIMESTAMPTZ, INT4),
                Set.of(box, genScalarDateTime, genDateTime, xsdScalarDateTime));
    }


    @Override
    public ResourceClass getResultResourceClass()
    {
        return this;
    }


    @Override
    public List<Column> toColumns(Request request, Literal literal)
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
            return List.of(expression(RDFBOX, "sparql.rdfbox_create_from_datetime(%s, %s)", time, zone));

        if(targetClass.equals(genScalarDateTime))
            return List.of(expression(ZONEDDATETIME, "sparql.zoneddatetime_create(%s, %s)", time, zone), !canBeNull ?
                    constant("", VARCHAR) : expression(VARCHAR, "CASE WHEN %s IS NOT NULL THEN ''::varchar END", time));

        if(targetClass.equals(genDateTime))
            return List.of(time, zone, !canBeNull ? constant("", VARCHAR) :
                    expression(VARCHAR, "CASE WHEN %s IS NOT NULL THEN ''::varchar END", time));

        if(targetClass.equals(xsdScalarDateTime))
            return List.of(expression(ZONEDDATETIME, "sparql.zoneddatetime_create(%s, %s)", time, zone));

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
            return List.of(expression(TIMESTAMPTZ, "sparql.rdfbox_get_datetime_value(%s, false)", columns.get(0)),
                    expression(INT4, "sparql.rdfbox_get_datetime_zone(%s, false)", columns.get(0)));

        if(sourceClass.equals(genScalarDateTime))
            return List.of(
                    expression(TIMESTAMPTZ, "(CASE %s WHEN '' THEN sparql.zoneddatetime_get_value(%s) END)",
                            columns.get(1), columns.get(0)),
                    expression(INT4, "(CASE %s WHEN '' THEN sparql.zoneddatetime_get_zone(%s) END)", columns.get(1),
                            columns.get(0)));

        if(sourceClass.equals(genDateTime))
            return List.of(expression(TIMESTAMPTZ, "(CASE %s WHEN '' THEN %s END)", columns.get(2), columns.get(0)),
                    expression(INT4, "(CASE %s WHEN '' THEN %s END)", columns.get(2), columns.get(1)));

        if(sourceClass.equals(xsdScalarDateTime))
            return List.of(expression(TIMESTAMPTZ, "sparql.zoneddatetime_get_value(%s)", columns.get(0)),
                    expression(INT4, "sparql.zoneddatetime_get_zone(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }
}

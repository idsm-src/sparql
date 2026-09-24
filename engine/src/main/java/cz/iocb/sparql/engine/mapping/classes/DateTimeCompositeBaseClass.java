package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.TIMESTAMPTZ;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.DateTimeDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Any valid xsd:dateTime value as a UTC {@code timestamptz}, an {@code int4} timezone offset and its lexical form; the
 * result class of non-canonical date-times.
 */
public final class DateTimeCompositeBaseClass extends BaseLiteralClass implements ResultResourceClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected DateTimeCompositeBaseClass()
    {
        super("base-datetime@2c", xsdDateTimeType, List.of(TIMESTAMPTZ, INT4, VARCHAR), Set.of(box, genScalarDateTime));
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(DateTimeDatatype.getDateTime(literal), sqlTypes.get(0)),
                constant(DateTimeDatatype.getZone(literal), sqlTypes.get(1)), getLexicalColumn(literal));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
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
        Column lexical = columns.get(2);

        if(targetClass.equals(box))
            return List
                    .of(expression("sparql.rdfbox_create_from_datetime_with_lexical(%s, %s, %s)", time, zone, lexical));

        if(targetClass.equals(genScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_create(%s, %s)", time, zone), lexical);

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
            return List.of(expression("sparql.rdfbox_get_datetime_value(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_datetime_zone(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_datetime_lexical(%s)", columns.get(0)));

        if(sourceClass.equals(genScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_get_value(%s)", columns.get(0)),
                    expression("sparql.zoneddatetime_get_zone(%s)", columns.get(0)), columns.get(1));

        throw new IllegalArgumentException();
    }
}

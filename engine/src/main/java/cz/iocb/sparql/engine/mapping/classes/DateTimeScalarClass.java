package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Canonical xsd:dateTime values in one {@code sparql.zoneddatetime} column.
 */
public final class DateTimeScalarClass extends CanonicalLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected DateTimeScalarClass()
    {
        super("datetime@1c", xsdDateTimeType, List.of("sparql.zoneddatetime"),
                Set.of(box, genScalarDateTime, genDateTime/*, xsdDateTime*/));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(xsdDateTime);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(xsdDateTimeType.getCanonicalLexicalForm(literal.getValue()), sqlTypes.get(0)));
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

        if(targetClass.equals(genScalarDateTime))
            return List.of(time, !canBeNull ? constant("", "varchar") :
                    expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", time));

        if(targetClass.equals(genDateTime))
            return List.of(expression("sparql.zoneddatetime_get_value(%s)", time),
                    expression("sparql.zoneddatetime_get_zone(%s)", time), !canBeNull ? constant("", "varchar") :
                            expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", time));

        //if(targetClass.equals(xsdDateTime))
        //    return List.of(expression("sparql.zoneddatetime_get_value(%s)", time),
        //            expression("sparql.zoneddatetime_get_zone(%s)", time));

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
            return List.of(expression("sparql.rdfbox_get_datetime(%s, false)", columns.get(0)));

        if(sourceClass.equals(genScalarDateTime))
            return List.of(expression("(CASE %s WHEN '' THEN %s END)", columns.get(1), columns.get(0)));

        if(sourceClass.equals(genDateTime))
            return List.of(expression("(CASE %s WHEN '' THEN sparql.zoneddatetime_create(%s, %s) END)", columns.get(2),
                    columns.get(0), columns.get(1)));

        //if(sourceClass.equals(xsdDateTime))
        //    return List.of(expression("sparql.zoneddatetime_create(%s, %s)", columns.get(0), columns.get(1)));

        throw new IllegalArgumentException();
    }
}

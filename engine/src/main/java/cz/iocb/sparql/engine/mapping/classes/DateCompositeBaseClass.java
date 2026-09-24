package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.DateDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Any valid xsd:date value as a {@code date}, an {@code int4} timezone offset and its lexical form; the result class of
 * non-canonical dates.
 */
public final class DateCompositeBaseClass extends BaseLiteralClass implements ResultResourceClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected DateCompositeBaseClass()
    {
        super("base-date@1c", xsdDateType, List.of("date", "int4", "varchar"), Set.of(box, genScalarDate));
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(DateDatatype.getDate(literal), sqlTypes.get(0)),
                constant(DateDatatype.getZone(literal), sqlTypes.get(1)), getLexicalColumn(literal));
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

        Column date = columns.get(0);
        Column zone = columns.get(1);
        Column lexical = columns.get(2);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_date_with_lexical(%s, %s, %s)", date, zone, lexical));

        if(targetClass.equals(genScalarDate))
            return List.of(expression("sparql.zoneddate_create(%s, %s)", date, zone), lexical);

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
            return List.of(expression("sparql.rdfbox_get_date_value(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_date_zone(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_date_lexical(%s)", columns.get(0)));

        if(sourceClass.equals(genScalarDate))
            return List.of(expression("sparql.zoneddate_get_value(%s)", columns.get(0)),
                    expression("sparql.zoneddate_get_zone(%s)", columns.get(0)), columns.get(1));

        throw new IllegalArgumentException();
    }
}

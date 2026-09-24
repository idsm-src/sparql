package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDate;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Any valid xsd:date value as a {@code sparql.zoneddate} column plus its lexical form.
 */
public final class DateScalarBaseClass extends BaseLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected DateScalarBaseClass()
    {
        super("base-date@1c", xsdDateType, List.of("sparql.zoneddate", "varchar"), Set.of(box/*, genDate*/));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(genDate);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(xsdDateType.getCanonicalLexicalForm(literal.getValue()), sqlTypes.get(0)),
                getLexicalColumn(literal));
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
            return List.of(expression("sparql.rdfbox_create_from_date_with_lexical(%s, %s)", date, lexical));

        //if(targetClass.equals(genDate))
        //    return List.of(expression("sparql.zoneddate_get_value(%s)", date),
        //            expression("sparql.zoneddate_get_zone(%s)", date), lexical);

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
            return List.of(expression("sparql.rdfbox_get_date(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_date_lexical(%s)", columns.get(0)));

        //if(sourceClass.equals(genDate))
        //    return List.of(expression("sparql.zoneddate_create(%s, %s)", columns.get(0), columns.get(1)),
        //            columns.get(2));

        throw new IllegalArgumentException();
    }
}

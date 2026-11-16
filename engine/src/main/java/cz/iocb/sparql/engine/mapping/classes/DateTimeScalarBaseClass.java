package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.database.SqlType.ZONEDDATETIME;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.request.Request;



/**
 * Any valid xsd:dateTime value as a {@code sparql.zoneddatetime} column plus its lexical form. The class holds the same
 * values as {@link DateTimeCompositeBaseClass}: a {@code zoneddatetime} of pgsparql is the instant as a
 * {@code timestamptz} with the timezone offset, a date-time without a timezone being its local time taken as UTC with
 * no offset, which is what the columns of that class hold, so the conversions between the two need no check. The
 * hierarchy must not be cyclic, so only this class is declared as the superclass of the other one. The value operators
 * of {@code zoneddatetime} compare the instants (the implicit timezone of pgsparql being UTC), so comparing the
 * {@code timestamptz} columns of the other class gives the same, by built-in operators that an index can serve.
 */
public final class DateTimeScalarBaseClass extends BaseLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected DateTimeScalarBaseClass()
    {
        super("base-datetime@1c", xsdDateTimeType, List.of(ZONEDDATETIME, VARCHAR), Set.of(box));
    }


    @Override
    public ResourceClass getResultResourceClass()
    {
        return genDateTime;
    }


    @Override
    public List<Column> toColumns(Request request, Literal literal)
    {
        return List.of(constant(xsdDateTimeType.getCanonicalLexicalForm(literal.getValue()), sqlTypes.get(0)),
                getLexicalColumn(literal));
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
            return List
                    .of(expression(RDFBOX, "sparql.rdfbox_create_from_datetime_with_lexical(%s, %s)", time, lexical));

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
            return List.of(expression(ZONEDDATETIME, "sparql.rdfbox_get_datetime(%s)", columns.get(0)),
                    expression(VARCHAR, "sparql.rdfbox_get_datetime_lexical(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }
}

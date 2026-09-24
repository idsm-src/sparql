package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Literals of one datatype in any valid lexical form: the native SQL value plus a varchar lexical column.
 */
public sealed abstract class SimpleLiteralBaseClass extends BaseLiteralClass implements ResultResourceClass
        permits BooleanBaseClass, ByteBaseClass, UnsignedByteBaseClass, ShortBaseClass, UnsignedShortBaseClass,
        IntBaseClass, UnsignedIntBaseClass, LongBaseClass, UnsignedLongBaseClass, IntegerBaseClass,
        NonPositiveIntegerBaseClass, NegativeIntegerBaseClass, NonNegativeIntegerBaseClass, PositiveIntegerBaseClass,
        DecimalBaseClass, FloatBaseClass, DoubleBaseClass, DayTimeDurationBaseClass
{
    /**
     * Name used in the {@code sparql.rdfbox_*} function names.
     */
    private final String fname;


    /**
     * Creates the class named {@code base-name} with the value column of the SQL type and a varchar lexical column.
     *
     * @param name the name
     * @param datatype the datatype
     * @param sqlType the SQL type
     */
    protected SimpleLiteralBaseClass(String name, Datatype datatype, String sqlType)
    {
        super("base-" + name, datatype, List.of(sqlType, "varchar"), Set.of(box));

        this.fname = name;
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(datatype.getCanonicalLexicalForm(literal.getValue()), sqlTypes.get(0)),
                getLexicalColumn(literal));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_%s_with_lexical(%s, %s)", fname, columns.get(0),
                    columns.get(1)));

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
            return List.of(expression("sparql.rdfbox_get_%s(%s)", fname, columns.get(0)),
                    expression("sparql.rdfbox_get_%s_lexical(%s)", fname, columns.get(0)));

        throw new IllegalArgumentException();
    }
}

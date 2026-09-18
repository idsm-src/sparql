package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;



public sealed abstract class SimpleLiteralClass extends CanonicalLiteralClass implements ResultResourceClass
        permits BooleanClass, ByteClass, UnsignedByteClass, ShortClass, UnsignedShortClass, IntClass, UnsignedIntClass,
        LongClass, UnsignedLongClass, IntegerClass, NonPositiveIntegerClass, NegativeIntegerClass,
        NonNegativeIntegerClass, PositiveIntegerClass, DecimalClass, FloatClass, DoubleClass, DayTimeDurationClass,
        StringClass
{
    private final LiteralClass base;


    protected SimpleLiteralClass(String name, Datatype datatype, String sqlType, LiteralClass base)
    {
        super(name, datatype, List.of(sqlType), Set.of(box, base));

        this.base = base;
    }


    protected SimpleLiteralClass(String name, Datatype datatype, String sqlType)
    {
        super(name, datatype, List.of(sqlType), Set.of(box));

        this.base = null;
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        assert literal.getValue().equals(datatype.getCanonicalLexicalForm(literal.getValue()));

        return List.of(constant(datatype.getCanonicalLexicalForm(literal.getValue()), sqlTypes.get(0)));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column value = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_%s(%s)", name, value));

        if(targetClass.equals(base))
            return List.of(value, !canBeNull ? constant("", "varchar") :
                    expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", value));

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
            if(base == null)
                return List.of(expression("sparql.rdfbox_get_%s(%s)", name, columns.get(0)));
            else
                return List.of(expression("sparql.rdfbox_get_%s(%s, false)", name, columns.get(0)));

        if(sourceClass.equals(base))
            return List.of(expression("(CASE %s WHEN '' THEN %s END)", columns.get(1), columns.get(0)));

        throw new IllegalArgumentException();
    }
}

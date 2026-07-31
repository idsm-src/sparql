package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;



public sealed abstract class SimpleLiteralClass extends LiteralClass implements ResultResourceClass
        permits BooleanClass, ShortClass, IntClass, LongClass, IntegerClass, DecimalClass, FloatClass, DoubleClass,
        StringClass, DayTimeDurationClass
{
    protected SimpleLiteralClass(String name, Datatype datatype, String sqlType)
    {
        super(name, datatype, List.of(sqlType), Set.of(box));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        //TODO: canonization will not be needed when special resource classes for canonical literals are introduced
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

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        if(sourceClass.equals(box))
            return List.of(expression("sparql.rdfbox_get_%s(%s)", name, columns.get(0)));

        throw new IllegalArgumentException();
    }
}

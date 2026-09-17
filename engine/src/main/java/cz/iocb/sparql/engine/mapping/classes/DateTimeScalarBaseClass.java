package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;



public final class DateTimeScalarBaseClass extends BaseLiteralClass
{
    protected DateTimeScalarBaseClass()
    {
        super("base-datetime@1c", xsdDateTimeType, List.of("sparql.zoneddatetime", "varchar"),
                Set.of(box /*, genDateTime*/));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(genDateTime);
    }


    @Override
    public List<Column> toColumns(Literal literal)
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
            return List.of(expression("sparql.rdfbox_create_from_datetime_with_lexical(%s, %s)", time, lexical));

        //if(targetClass.equals(genDateTime))
        //    return List.of(expression("sparql.zoneddatetime_get_value(%s)", time),
        //            expression("sparql.zoneddatetime_get_zone(%s)", time), lexical);

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
            return List.of(expression("sparql.rdfbox_get_datetime(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_datetime_lexical(%s)", columns.get(0)));

        //if(sourceClass.equals(genDateTime))
        //    return List.of(expression("sparql.zoneddatetime_create(%s, %s)", columns.get(0), columns.get(1)),
        //            columns.get(2));

        throw new IllegalArgumentException();
    }
}

package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;



public final class DateScalarClass extends CanonicalLiteralClass
{
    protected DateScalarClass()
    {
        super("date@1c", xsdDateType, List.of("sparql.zoneddate"), Set.of(box, genScalarDate, genDate /*, xsdDate*/));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(xsdDate);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(xsdDateType.getCanonicalLexicalForm(literal.getValue()), sqlTypes.get(0)));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column date = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_date(%s)", date));

        if(targetClass.equals(genScalarDate))
            return List.of(date, !canBeNull ? constant("", "varchar") :
                    expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", date));

        if(targetClass.equals(genDate))
            return List.of(expression("sparql.zoneddate_get_value(%s)", date),
                    expression("sparql.zoneddate_get_zone(%s)", date), !canBeNull ? constant("", "varchar") :
                            expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", date));

        //if(targetClass.equals(xsdDate))
        //    return List.of(expression("sparql.zoneddate_get_value(%s)", date),
        //           expression("sparql.zoneddate_get_zone(%s)", date));

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
            return List.of(expression("sparql.rdfbox_get_date(%s, false)", columns.get(0)));

        if(sourceClass.equals(genScalarDate))
            return List.of(expression("(CASE %s WHEN '' THEN %s END)", columns.get(1), columns.get(0)));

        if(sourceClass.equals(genDate))
            return List.of(expression("(CASE %s WHEN '' THEN sparql.zoneddate_create(%s, %s) END)", columns.get(2),
                    columns.get(0), columns.get(1)));

        //if(sourceClass.equals(xsdDate))
        //    return List.of(expression("sparql.zoneddate_create(%s, %s)", columns.get(0), columns.get(1)));

        throw new IllegalArgumentException();
    }
}

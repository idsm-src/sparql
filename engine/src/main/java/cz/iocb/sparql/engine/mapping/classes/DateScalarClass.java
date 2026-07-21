package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public final class DateScalarClass extends LiteralClass
{
    protected DateScalarClass()
    {
        super("date", xsdDateIri, List.of("sparql.zoneddate"), Set.of(box/*, xsdCompositeDate*/));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(xsdCompositeDate);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(literal.getValue(), "sparql.zoneddate"));
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

        //if(targetClass.equals(xsdCompositeDate))
        //    return List.of(expression("sparql.zoneddate_get_value(%s)", date),
        //           expression("sparql.zoneddate_get_zone(%s)", date));

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
            return List.of(expression("sparql.rdfbox_get_date(%s)", columns.get(0)));

        //if(sourceClass.equals(xsdCompositeDate))
        //    return List.of(expression("sparql.zoneddate_create(%s, %s)", columns.get(0), columns.get(1)));

        throw new IllegalArgumentException();
    }
}

package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public final class DateCompositeClass extends LiteralClass implements ResultResourceClass
{
    protected DateCompositeClass()
    {
        super("date@2c", xsdDateIri, List.of("date", "int4"), Set.of(box, xsdScalarDate));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
    }


    @Override
    public List<Column> toColumns(Literal lieral)
    {
        return List.of(constant(getDate(lieral), "date"), constant(getZone(lieral), "int4"));
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

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_date(%s, %s)", zone, date));

        if(targetClass.equals(xsdScalarDate))
            return List.of(expression("sparql.zoneddate_create(%s, %s)", zone, date));

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
            return List.of(expression("sparql.rdfbox_get_date_value(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_date_zone(%s)", columns.get(0)));

        if(sourceClass.equals(xsdScalarDate))
            return List.of(expression("sparql.zoneddate_get_value(%s)", columns.get(0)),
                    expression("sparql.zoneddate_get_zone(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }


    protected static String getDate(Literal literal)
    {
        return ((String) literal.getValue()).replaceFirst("(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))$", "");
    }


    protected static int getZone(Literal literal)
    {
        String value = (String) literal.getValue();

        String[] parts = value.replaceFirst(".*(Z|(([+-])([0-9][0-9]):([0-9][0-9])))$", "$31#0$4#0$5").split("#");

        if(parts.length == 3)
            return Integer.parseInt(parts[0]) * (Integer.parseInt(parts[1]) * 3600 + Integer.parseInt(parts[2]) * 60);

        return Integer.MIN_VALUE;
    }
}

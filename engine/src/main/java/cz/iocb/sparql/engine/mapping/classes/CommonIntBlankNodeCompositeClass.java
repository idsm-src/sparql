package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intScalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.IntBlankNode;



public final class CommonIntBlankNodeCompositeClass extends IntBlankNodeClass implements ResultResourceClass
{
    protected CommonIntBlankNodeCompositeClass()
    {
        super("iblanknode@2c", List.of("int4", "int4"), Set.of(box, intScalarBlankNode));
    }


    @Override
    public boolean match(Statement statement, IntBlankNode term)
    {
        return true;
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column bnvalue = columns.get(0);
        Column segment = columns.get(1);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_iblanknode(%s, %s)", bnvalue, segment));

        if(targetClass.equals(intScalarBlankNode))
            return List.of(expression("sparql.iblanknode_create(%s, %s)", bnvalue, segment));

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
            return List.of(expression("sparql.rdfbox_get_iblanknode_value(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_iblanknode_segment(%s)", columns.get(0)));

        if(sourceClass.equals(intScalarBlankNode))
            return List.of(expression("sparql.iblanknode_get_value(%s)", columns.get(0)),
                    expression("sparql.iblanknode_get_segment(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(IntBlankNode bnode)
    {
        int segment = bnode.getSegment();
        return List.of(constant(bnode.getValue(), sqlTypes.get(0)), constant(segment, sqlTypes.get(1)));
    }
}

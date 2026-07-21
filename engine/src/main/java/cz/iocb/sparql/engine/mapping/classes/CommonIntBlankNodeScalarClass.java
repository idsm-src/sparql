package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.BlankNodeLiteral;



public final class CommonIntBlankNodeScalarClass extends IntBlankNodeClass
{
    protected CommonIntBlankNodeScalarClass()
    {
        super("iblanknode@1c", List.of("int8"), Set.of(box/*, intCompositeBlankNode*/));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column bnvalue = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_iblanknode(%s)", bnvalue));

        //if(targetClass.equals(intCompositeBlankNode))
        //    return List.of(expression("sparql.iblanknode_get_value(%s)", bnvalue),
        //            expression("sparql.iblanknode_get_segment(%s)", bnvalue));

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
            return List.of(expression("sparql.rdfbox_get_iblanknode(%s)", columns.get(0)));

        //if(sourceClass.equals(intCompositeBlankNode))
        //    return List.of(expression("sparql.iblanknode_create(%s, %s)", columns.get(0), columns.get(1)));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(BlankNodeLiteral bnode)
    {
        return List.of(expression("sparql.iblanknode_create('%s'::int4, '%s'::int4)", bnode.getLabel(),
                ((UserIntBlankNodeClass) bnode.getResourceClass()).getSegment()));
    }
}

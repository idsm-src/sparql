package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strScalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.StrBlankNode;



public final class CommonStrBlankNodeCompositeClass extends StrBlankNodeClass implements ResultResourceClass
{
    protected CommonStrBlankNodeCompositeClass()
    {
        super("sblanknode@2c", List.of("varchar", "int4"), Set.of(box, strScalarBlankNode));
    }


    @Override
    public boolean match(Statement statement, StrBlankNode term)
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
            return List.of(expression("sparql.rdfbox_create_from_sblanknode(%s, %s)", bnvalue, segment));

        if(targetClass.equals(strScalarBlankNode))
            return List.of(expression("sparql.sblanknode_create(%s, %s)", bnvalue, segment));

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
            return List.of(expression("sparql.rdfbox_get_sblanknode_value(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_sblanknode_segment(%s)", columns.get(0)));

        if(sourceClass.equals(strScalarBlankNode))
            return List.of(expression("sparql.sblanknode_get_value(%s)", columns.get(0)),
                    expression("sparql.sblanknode_get_segment(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(StrBlankNode bnode)
    {
        int segment = bnode.getSegment();
        return List.of(constant(bnode.getValue(), sqlTypes.get(0)), constant(segment, sqlTypes.get(1)));
    }
}

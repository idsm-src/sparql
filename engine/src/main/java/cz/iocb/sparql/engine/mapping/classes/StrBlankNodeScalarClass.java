package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.StrBlankNode;



/**
 * String blank nodes in one varchar column encoding both value and segment.
 */
public final class StrBlankNodeScalarClass extends StrBlankNodeClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected StrBlankNodeScalarClass()
    {
        super("sblanknode@1c", List.of("varchar"), Set.of(box/*, strBlankNode*/));
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

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_sblanknode(%s)", bnvalue));

        //if(targetClass.equals(strBlankNode))
        //    return List.of(expression("sparql.sblanknode_get_value(%s)", bnvalue),
        //            expression("sparql.sblanknode_get_segment(%s)", bnvalue));

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
            return List.of(expression("sparql.rdfbox_get_sblanknode(%s)", columns.get(0)));

        //if(sourceClass.equals(strBlankNode))
        //    return List.of(expression("sparql.sblanknode_create(%s, %s)", columns.get(0), columns.get(1)));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(StrBlankNode bnode)
    {
        return List.of(expression("sparql.sblanknode_create('%s'::varchar, '%s'::int4)", bnode.getValue(),
                bnode.getSegment()));
    }
}

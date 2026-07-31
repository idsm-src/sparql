package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intCompositeBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intScalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strCompositeBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strScalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.StrBlankNode;



public class StrBlankNodeConstantSegmentClass extends StrBlankNodeClass
{
    private final int segment;


    public StrBlankNodeConstantSegmentClass(int segment)
    {
        super("sblanknode-" + Integer.toHexString(segment), List.of("varchar"),
                Set.of(box, strScalarBlankNode, strCompositeBlankNode));

        this.segment = segment;
    }


    @Override
    public boolean match(Statement statement, StrBlankNode term)
    {
        return term.getSegment() == segment;
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
            return List.of(expression("sparql.rdfbox_create_from_sblanknode(%s, '%d'::int4)", bnvalue, segment));

        if(targetClass.equals(strScalarBlankNode))
            return List.of(expression("sparql.sblanknode_create(%s, '%d'::int4)", bnvalue, segment));

        if(targetClass.equals(strCompositeBlankNode))
            return List.of(bnvalue, !canBeNull ? constant(segment, "int4") :
                    expression("CASE WHEN %s IS NOT NULL THEN '%d'::int4 END", bnvalue, segment));

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
            return List.of(expression("sparql.rdfbox_get_sblanknode_value_of_segment(%s, '%d'::int4)", columns.get(0),
                    segment));

        if(sourceClass.equals(intScalarBlankNode))
            return List
                    .of(expression("sparql.sblanknode_get_value_of_segment(%s, '%d'::int4)", columns.get(0), segment));

        if(sourceClass.equals(intCompositeBlankNode))
            return List
                    .of(expression("CASE WHEN %s = '%d'::int4 THEN %s END", columns.get(1), segment, columns.get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(StrBlankNode bnode)
    {
        return List.of(constant(bnode.getValue(), sqlTypes.get(0)));
    }


    public int getSegment()
    {
        return segment;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        StrBlankNodeConstantSegmentClass other = (StrBlankNodeConstantSegmentClass) object;

        return segment == other.segment;
    }
}

package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intScalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.IntBlankNode;



/**
 * Integer blank nodes of one fixed segment, stored as the {@code int4} value only.
 */
public class IntBlankNodeInSegmentClass extends IntBlankNodeClass
{
    /**
     * The fixed segment.
     */
    private final int segment;


    /**
     * Creates the class of the segment.
     *
     * @param segment the segment
     */
    public IntBlankNodeInSegmentClass(int segment)
    {
        super("iblanknode-" + Integer.toHexString(segment), List.of("int4"),
                Set.of(box, intScalarBlankNode, intBlankNode));

        this.segment = segment;
    }


    @Override
    public boolean match(Statement statement, IntBlankNode term)
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
            return List.of(expression("sparql.rdfbox_create_from_iblanknode(%s, '%d'::int4)", bnvalue, segment));

        if(targetClass.equals(intScalarBlankNode))
            return List.of(expression("sparql.iblanknode_create(%s, '%d'::int4)", bnvalue, segment));

        if(targetClass.equals(intBlankNode))
            return List.of(bnvalue, !canBeNull ? constant(segment, "int4") :
                    expression("CASE WHEN %s IS NOT NULL THEN '%d'::int4 END", bnvalue, segment));

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
            return List.of(expression("sparql.rdfbox_get_iblanknode_value_of_segment(%s, '%d'::int4)", columns.get(0),
                    segment));

        if(sourceClass.equals(intScalarBlankNode))
            return List
                    .of(expression("sparql.iblanknode_get_value_of_segment(%s, '%d'::int4)", columns.get(0), segment));

        if(sourceClass.equals(intBlankNode))
            return List
                    .of(expression("CASE WHEN %s = '%d'::int4 THEN %s END", columns.get(1), segment, columns.get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(IntBlankNode bnode)
    {
        return List.of(constant(bnode.getValue(), sqlTypes.get(0)));
    }


    /**
     * The fixed segment.
     *
     * @return the fixed segment
     */
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

        IntBlankNodeInSegmentClass other = (IntBlankNodeInSegmentClass) object;

        return segment == other.segment;
    }
}

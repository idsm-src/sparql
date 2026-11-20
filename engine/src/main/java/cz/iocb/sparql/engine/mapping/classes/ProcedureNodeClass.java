package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strScalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.request.Request;



/**
 * String blank nodes held by the values of columns of their own, by which the nodes of the class are compared: the
 * nodes standing for the parameters of the calls of one procedure, held by the values the calls pass to the parameters,
 * and the nodes standing for the rows of results of such calls, held by those values and the text of the row (see
 * {@link cz.iocb.sparql.engine.imcode.SqlProcedureCall}). A node has its value, the label within its segment, only when
 * it is converted to a general class of string blank nodes or to the box, to be returned, say: the texts of its columns
 * separated by commas, with a percent sign and a comma in a text written as {@code %25} and {@code %2C}, and with a
 * NULL of an optional column written as a lone percent sign. A node converted back reads its columns from the label.
 * Each class has a segment of its own, so that the labels of the nodes of different classes never coincide.
 */
public final class ProcedureNodeClass extends StrBlankNodeClass
{
    /**
     * The segment of the class.
     */
    private final int segment;

    /**
     * Whether each column may be NULL while the node is bound (see {@link #isOptionalColumn}).
     */
    private final List<Boolean> optional;


    /**
     * Creates the class of the segment.
     *
     * @param segment the segment
     * @param sqlTypes the SQL types of the columns holding a node
     * @param optional whether each column may be NULL while the node is bound
     * @throws IllegalArgumentException if the columns are missing, their flags do not match them, or every column is
     *             optional
     */
    public ProcedureNodeClass(int segment, List<SqlType> sqlTypes, List<Boolean> optional)
    {
        super("pblanknode-" + Integer.toHexString(segment), List.copyOf(sqlTypes),
                Set.of(box, strScalarBlankNode, strBlankNode));

        if(sqlTypes.isEmpty() || sqlTypes.size() != optional.size() || !optional.contains(false))
            throw new IllegalArgumentException("a node class needs columns, one of them not optional");

        this.segment = segment;
        this.optional = List.copyOf(optional);
    }


    @Override
    public boolean isOptionalColumn(int index)
    {
        return optional.get(index);
    }


    @Override
    public boolean match(Request request, StrBlankNode term)
    {
        return term.getSegment() == segment && parse(term.getValue()) != null;
    }


    @Override
    public List<Column> toColumns(StrBlankNode bnode)
    {
        List<String> texts = parse(bnode.getValue());

        if(bnode.getSegment() != segment || texts == null)
            throw new IllegalArgumentException();

        List<Column> columns = new ArrayList<>();

        for(int i = 0; i < texts.size(); i++)
        {
            SqlType type = sqlTypes.get(i);
            columns.add(texts.get(i) == null ? new NullColumn(type) : constant(texts.get(i), type));
        }

        return columns;
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column label = getLabel(columns);

        if(targetClass.equals(box))
            return List.of(expression(RDFBOX, "sparql.rdfbox_create_from_sblanknode(%s, '%d'::int4)", label, segment));

        if(targetClass.equals(strScalarBlankNode))
            return List.of(expression(VARCHAR, "sparql.sblanknode_create(%s, '%d'::int4)", label, segment));

        if(targetClass.equals(strBlankNode))
            return List.of(label, !canBeNull ? constant(segment, INT4) :
                    expression(INT4, "CASE WHEN %s IS NOT NULL THEN '%d'::int4 END", label, segment));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        // the label of a node of another segment is NULL, so the columns are NULL too
        if(sourceClass.equals(box))
            return getColumns(expression(VARCHAR, "sparql.rdfbox_get_sblanknode_value_of_segment(%s, '%d'::int4)",
                    columns.get(0), segment));

        if(sourceClass.equals(strScalarBlankNode))
            return getColumns(expression(VARCHAR, "sparql.sblanknode_get_value_of_segment(%s, '%d'::int4)",
                    columns.get(0), segment));

        if(sourceClass.equals(strBlankNode))
            return getColumns(expression(VARCHAR, "CASE WHEN %s = '%d'::int4 THEN %s END", columns.get(1), segment,
                    columns.get(0)));

        throw new IllegalArgumentException();
    }


    /**
     * Column of the label of the node held by the columns: the texts of the columns separated by commas, with a percent
     * sign and a comma written as {@code %25} and {@code %2C}, and a NULL of an optional column written as a lone
     * percent sign; NULL when the node is not bound, as a column that is not optional is NULL then.
     *
     * @param columns the columns holding the node
     * @return column of the label of the node
     */
    private Column getLabel(List<Column> columns)
    {
        List<SqlCode> parts = new ArrayList<>();

        for(int i = 0; i < columns.size(); i++)
        {
            SqlCode text = SqlCode.of("replace(replace((", columns.get(i), ")::text, '%', '%25'), ',', '%2C')");
            parts.add(optional.get(i) ? SqlCode.of("COALESCE(", text, ", '%')") : text);
        }

        return new ExpressionColumn(parts.stream().collect(SqlCode.joining(" || ',' || ", "(", ")")), VARCHAR);
    }


    /**
     * Columns holding the node of the label (see {@link #getLabel}), NULL for a NULL label.
     *
     * @param label column of the label
     * @return columns holding the node of the label
     */
    private List<Column> getColumns(Column label)
    {
        List<Column> columns = new ArrayList<>();

        for(int i = 0; i < sqlTypes.size(); i++)
        {
            SqlCode part = sqlTypes.size() == 1 ? label.getCode() :
                    SqlCode.of("split_part(", label, ", ',', ", Integer.toString(i + 1), ")");
            SqlCode value = SqlCode.of("(replace(replace(", part, ", '%2C', ','), '%25', '%'))::", sqlTypes.get(i));

            if(optional.get(i))
                value = SqlCode.of("(CASE WHEN ", part, " = '%' THEN NULL ELSE ", value, " END)");

            columns.add(new ExpressionColumn(value, sqlTypes.get(i)));
        }

        return columns;
    }


    /**
     * Texts of the columns of the node of the label (see {@link #getLabel}), null for a NULL of an optional column.
     *
     * @param label the label
     * @return texts of the columns of the node of the label, or null if the label is no label of a node of the class
     */
    private List<String> parse(String label)
    {
        String[] parts = label.split(",", -1);

        if(parts.length != sqlTypes.size())
            return null;

        List<String> texts = new ArrayList<>();

        for(int i = 0; i < parts.length; i++)
        {
            if(parts[i].equals("%") && !optional.get(i))
                return null;

            texts.add(parts[i].equals("%") ? null : parts[i].replace("%2C", ",").replace("%25", "%"));
        }

        return texts;
    }


    /**
     * The segment of the class.
     *
     * @return the segment of the class
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

        ProcedureNodeClass other = (ProcedureNodeClass) object;

        return segment == other.segment && Objects.equals(optional, other.optional);
    }
}

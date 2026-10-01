package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLtrLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfRtlLangString;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfDirLangStringType;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.request.Request;



/**
 * Directional language-tagged strings of one base direction and one fixed tag, stored as the value only; instances are
 * cached per direction and tag.
 */
public final class DirLangStringWithTagClass extends CanonicalLiteralClass
{
    /**
     * Instances by name (the direction and the lower-cased tag).
     */
    private static final ConcurrentMap<String, DirLangStringWithTagClass> instances = new ConcurrentHashMap<>();

    /**
     * The base direction.
     */
    private final Direction direction;

    /**
     * The fixed lower-cased tag.
     */
    private final String tag;


    /**
     * Creates the class of the direction and tag; use {@link #get}.
     *
     * @param direction the base direction
     * @param tag the lower-cased language tag
     */
    private DirLangStringWithTagClass(Direction direction, String tag)
    {
        super(getName(direction, tag), rdfDirLangStringType, List.of(VARCHAR),
                Set.of(box, getDirectionClass(direction)));

        this.direction = direction;
        this.tag = tag;
    }


    /**
     * The class of the given direction and tag (case-insensitive).
     *
     * @param direction the base direction
     * @param tag the language tag
     * @return the class of the given direction and tag (case-insensitive)
     */
    public static DirLangStringWithTagClass get(Direction direction, String tag)
    {
        String name = getName(direction, tag.toLowerCase());

        return instances.computeIfAbsent(name, _ -> new DirLangStringWithTagClass(direction, tag.toLowerCase()));
    }


    /**
     * Name of the class of the direction and tag.
     *
     * @param direction the base direction
     * @param tag the lower-cased language tag
     * @return name of the class of the direction and tag
     */
    private static String getName(Direction direction, String tag)
    {
        return direction.getText() + "-lang-" + tag;
    }


    /**
     * Class of all directional language-tagged strings of the direction.
     *
     * @param direction the base direction
     * @return class of all directional language-tagged strings of the direction
     */
    private static DirLangStringClass getDirectionClass(Direction direction)
    {
        return direction == Direction.LTR ? rdfLtrLangString : rdfRtlLangString;
    }


    @Override
    public ResourceClass getResultResourceClass()
    {
        return getDirectionClass();
    }


    @Override
    public boolean match(Request request, Literal literal)
    {
        if(!super.match(request, literal))
            return false;

        return literal instanceof DirLangStringLiteral dirLiteral && dirLiteral.getDirection() == direction
                && Objects.equals(dirLiteral.getTag(), tag);
    }


    @Override
    public List<Column> toColumns(Request request, Literal literal)
    {
        return List.of(constant(literal.getValue(), VARCHAR));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column string = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_%slangstring(%s, '%s'::varchar)", direction.getText(),
                    string, tag));

        if(targetClass.equals(getDirectionClass()))
            return List.of(string, !canBeNull ? constant(tag, VARCHAR) :
                    expression("CASE WHEN %s IS NOT NULL THEN '%s'::varchar END", string, tag));

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
            return List.of(expression("sparql.rdfbox_get_%slangstring_value_of_lang(%s, '%s'::varchar)",
                    direction.getText(), columns.get(0), tag));

        if(sourceClass.equals(getDirectionClass()))
            return List.of(expression("CASE WHEN %s = '%s'::varchar THEN %s END", columns.get(1), tag, columns.get(0)));

        throw new IllegalArgumentException();
    }


    /**
     * The base direction.
     *
     * @return the base direction
     */
    public Direction getDirection()
    {
        return direction;
    }


    /**
     * The fixed lower-cased tag.
     *
     * @return the fixed lower-cased tag
     */
    public String getTag()
    {
        return tag;
    }


    /**
     * Class of all directional language-tagged strings of the same base direction, in which the values of this class
     * are delivered.
     *
     * @return class of all directional language-tagged strings of the same base direction
     */
    public DirLangStringClass getDirectionClass()
    {
        return getDirectionClass(direction);
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        DirLangStringWithTagClass other = (DirLangStringWithTagClass) object;

        return direction == other.direction && Objects.equals(tag, other.tag);
    }
}

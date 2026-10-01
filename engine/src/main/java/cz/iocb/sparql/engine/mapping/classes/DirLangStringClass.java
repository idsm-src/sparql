package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfDirLangStringType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.request.Request;



/**
 * Directional language-tagged strings of one base direction stored as value and tag columns; the result class of all
 * directional language-tagged strings of that direction. The direction is a part of the class rather than a column, as
 * the extension keeps the strings of the two directions in two box types with separate constructors and getters.
 */
public final class DirLangStringClass extends CanonicalLiteralClass
{
    /**
     * The base direction.
     */
    private final Direction direction;


    /**
     * Creates the singleton instance of the direction, see {@link BuiltinClasses}.
     *
     * @param direction the base direction
     */
    protected DirLangStringClass(Direction direction)
    {
        super(direction.getText() + "-lang", rdfDirLangStringType, List.of(VARCHAR, VARCHAR), Set.of(box));
        this.direction = direction;
    }


    @Override
    public ResourceClass getResultResourceClass()
    {
        return this;
    }


    @Override
    public boolean match(Request request, Literal literal)
    {
        if(!super.match(request, literal))
            return false;

        return literal instanceof DirLangStringLiteral dirLiteral && dirLiteral.getDirection() == direction;
    }


    @Override
    public List<Column> toColumns(Request request, Literal literal)
    {
        DirLangStringLiteral dirLiteral = (DirLangStringLiteral) literal;

        return List.of(constant(dirLiteral.getValue(), VARCHAR), constant(dirLiteral.getTag(), VARCHAR));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column string = columns.get(0);
        Column lang = columns.get(1);

        if(targetClass.equals(box))
            return List.of(expression(RDFBOX, "sparql.rdfbox_create_from_%slangstring(%s, %s)", direction.getText(),
                    string, lang));

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
            return List.of(
                    expression(VARCHAR, "sparql.rdfbox_get_%slangstring_value(%s)", direction.getText(),
                            columns.get(0)),
                    expression(VARCHAR, "sparql.rdfbox_get_%slangstring_lang(%s)", direction.getText(),
                            columns.get(0)));

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


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        DirLangStringClass other = (DirLangStringClass) object;

        return direction == other.direction;
    }
}

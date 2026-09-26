package cz.iocb.sparql.engine.rdf;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfDirLangStringIri;
import java.util.Objects;



/**
 * Language-tagged literal with a base direction ({@code rdf:dirLangString}); the tag is normalised to lower case. It is
 * a sibling of {@link LangStringLiteral}, not a subclass, because the two datatypes are distinct and a directional
 * literal must not be mistaken for a plain language-tagged one.
 */
public class DirLangStringLiteral extends Literal
{
    /**
     * Base direction of a language-tagged literal.
     */
    public enum Direction
    {
        /**
         * Left to right ({@code ltr}).
         */
        LTR("ltr"),

        /**
         * Right to left ({@code rtl}).
         */
        RTL("rtl");

        /**
         * SPARQL spelling.
         */
        private final String text;

        /**
         * Creates the direction with its SPARQL spelling.
         *
         * @param text the text
         */
        Direction(String text)
        {
            this.text = text;
        }

        /**
         * SPARQL spelling of the direction, as written after {@code --}.
         *
         * @return SPARQL spelling of the direction, as written after {@code --}
         */
        public String getText()
        {
            return text;
        }

        /**
         * Direction written by the text; null if the text is neither {@code ltr} nor {@code rtl}.
         *
         * @param text the text
         * @return direction written by the text; null if the text is neither {@code ltr} nor {@code rtl}
         */
        public static Direction fromText(String text)
        {
            for(Direction direction : values())
                if(direction.text.equals(text))
                    return direction;

            return null;
        }
    }


    /**
     * Lower-cased language tag.
     */
    protected final String tag;

    /**
     * Base direction.
     */
    protected final Direction direction;


    /**
     * Creates the literal; the tag is lower-cased.
     *
     * @param value the lexical form
     * @param tag the language tag
     * @param direction the base direction (required)
     */
    public DirLangStringLiteral(String value, String tag, Direction direction)
    {
        super(value);

        if(direction == null)
            throw new IllegalArgumentException();

        this.tag = tag.toLowerCase();
        this.direction = direction;
    }


    /**
     * Lower-cased language tag.
     *
     * @return lower-cased language tag
     */
    public String getTag()
    {
        return tag;
    }


    /**
     * Base direction.
     *
     * @return base direction
     */
    public Direction getDirection()
    {
        return direction;
    }


    @Override
    public Iri getType()
    {
        return rdfDirLangStringIri;
    }


    @Override
    public String toString()
    {
        return super.toString() + "@" + tag + "--" + direction.getText();
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(value, tag, direction);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass() || !super.equals(object))
            return false;

        DirLangStringLiteral other = (DirLangStringLiteral) object;

        return Objects.equals(tag, other.tag) && Objects.equals(direction, other.direction);
    }
}

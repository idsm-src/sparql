package cz.iocb.sparql.engine.model.triple;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Path that contains a path ({@link #getChild}) which has to be traversed the given number of times
 * ({@link #getKind()}).
 *
 * <p>
 * Corresponds to the rule [91] PathElt with {@code PathMod} present in the SPARQL grammar.
 */
public class RepeatedPath extends BaseElement implements Path
{
    /**
     * Repetition modifier of a path element.
     */
    public enum Kind
    {
        /**
         * Zero or one traversal ({@code ?}).
         */
        ZeroOrOne("?"),

        /**
         * Any number of traversals ({@code *}).
         */
        ZeroOrMore("*"),

        /**
         * At least one traversal ({@code +}).
         */
        OneOrMore("+");

        /**
         * SPARQL spelling.
         */
        private final String text;

        /**
         * Creates the kind with its SPARQL spelling.
         *
         * @param text the text
         */
        Kind(String text)
        {
            this.text = text;
        }


        /**
         * SPARQL spelling of the modifier.
         *
         * @return SPARQL spelling of the modifier
         */
        public String getText()
        {
            return text;
        }
    }


    /**
     * Repetition kind.
     */
    private Kind kind;

    /**
     * The repeated path.
     */
    private Path child;


    /**
     * Creates the repetition of the child.
     *
     * @param kind the repetition kind
     * @param child the child path
     */
    public RepeatedPath(Kind kind, Path child)
    {
        setKind(kind);
        setChild(child);
    }


    /**
     * Repetition kind.
     *
     * @return repetition kind
     */
    public Kind getKind()
    {
        return kind;
    }


    /**
     * Sets the repetition kind (required).
     *
     * @param kind the repetition kind
     */
    public void setKind(Kind kind)
    {
        if(kind == null)
            throw new IllegalArgumentException();

        this.kind = kind;
    }


    /**
     * The repeated path.
     *
     * @return the repeated path
     */
    public Path getChild()
    {
        return child;
    }


    /**
     * Sets the repeated path.
     *
     * @param child the child path
     */
    public void setChild(Path child)
    {
        this.child = child;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}

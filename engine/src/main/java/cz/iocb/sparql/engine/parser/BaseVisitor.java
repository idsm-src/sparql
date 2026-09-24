package cz.iocb.sparql.engine.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import cz.iocb.sparql.engine.grammar.SparqlParserBaseVisitor;
import cz.iocb.sparql.engine.model.base.Element;
import cz.iocb.sparql.engine.model.base.Range;



/**
 * Base visitor, which sets the range of each returned Element.
 *
 * @param <T> type of the produced elements
 */
public class BaseVisitor<T> extends SparqlParserBaseVisitor<T>
{
    /**
     * Creates the visitor.
     */
    public BaseVisitor()
    {
    }


    /**
     * Assigns the source range of {@code tree} to {@code element} unless it already has one, and returns the element.
     *
     * @param <TElement> the element type
     * @param element the element
     * @param tree the parse tree
     * @return the element
     */
    public static <TElement extends Element> TElement withRange(TElement element, ParserRuleContext tree)
    {
        if(element.getRange() == null)
            element.setRange(Range.compute(tree));

        return element;
    }


    @Override
    public T visit(ParseTree tree)
    {
        T result = super.visit(tree);

        if(result instanceof Element element)
            withRange(element, (ParserRuleContext) tree);

        return result;
    }
}

package cz.iocb.sparql.engine.model.base;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import cz.iocb.sparql.engine.parser.Position;



/**
 * A range of characters in a text file or string.
 *
 * Both start and end are inclusive.
 */
public class Range
{
    /**
     * First character of the range.
     */
    private Position start;

    /**
     * Last character of the range; null for a zero-length range.
     */
    private Position end;


    /**
     * Range covered by the parse tree node.
     *
     * @param tree the parse tree
     * @return range covered by the parse tree node
     */
    public static Range compute(ParserRuleContext tree)
    {
        Token start = tree.getStart();
        Token stop = tree.getStop();

        return compute(start, stop);
    }


    /**
     * Range from the first character of {@code start} to the last character of {@code stop}; a null {@code stop} (a
     * rule that matched nothing) gives a zero-length range.
     *
     * @param start the first token
     * @param stop the last token
     * @return range from the first character of {@code start} to the last character of {@code stop}; a null {@code
     *         stop} (a rule that matched nothing) gives a zero-length range
     */
    public static Range compute(Token start, Token stop)
    {
        Position startPosition = new Position(start.getLine(), start.getCharPositionInLine());

        Position endPosition;
        // this happens when the rule matched nothing
        if(stop == null)
            endPosition = null;
        else
            endPosition = new Position(stop.getLine(),
                    stop.getCharPositionInLine() + stop.getStopIndex() - stop.getStartIndex());

        return new Range(startPosition, endPosition);
    }


    /**
     * Creates the range; {@code end} may be null for a zero-length range.
     *
     * @param start the first token
     * @param end the end position
     */
    public Range(Position start, Position end)
    {
        this.start = start;
        this.end = end;
    }


    /**
     * First character of the range.
     *
     * @return first character of the range
     */
    public Position getStart()
    {
        return start;
    }


    /**
     * Is null for zero-length ranges.
     *
     * @return the end position; null for zero-length ranges
     */
    public Position getEnd()
    {
        return end;
    }


    @Override
    public String toString()
    {
        if(end == null)
            return start.toString();

        if(start.getLineNumber() == end.getLineNumber())
            return String.format("%d:%d-%d", start.getLineNumber(), start.getPositionInLine(), end.getPositionInLine());

        return String.format("%s-%s", start.toString(), end.toString());
    }
}

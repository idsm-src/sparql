package cz.iocb.sparql.engine.parser;



/**
 * Position in a text file or string.
 */
public class Position
{
    /**
     * Line number, starting at one.
     */
    private int lineNumber;

    /**
     * Character position in the line, starting at zero.
     */
    private int positionInLine;


    /**
     * Creates the position.
     *
     * @param lineNumber line number, starting at one
     * @param positionInLine position in the line, starting at zero
     */
    public Position(int lineNumber, int positionInLine)
    {
        this.lineNumber = lineNumber;
        this.positionInLine = positionInLine;
    }


    /**
     * Line number, starting at one.
     *
     * @return line number, starting at one
     */
    public int getLineNumber()
    {
        return lineNumber;
    }


    /**
     * Character position in the line, starting at zero.
     *
     * @return character position in the line, starting at zero
     */
    public int getPositionInLine()
    {
        return positionInLine;
    }


    @Override
    public String toString()
    {
        return String.format("%d:%d", lineNumber, positionInLine);
    }
}

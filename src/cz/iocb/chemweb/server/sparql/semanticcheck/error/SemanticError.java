package cz.iocb.chemweb.server.sparql.semanticcheck.error;

import cz.iocb.chemweb.server.sparql.parser.Range;



/**
 *
 * Semantic error object with full information.
 *
 */
public class SemanticError
{
    /** Type of an error */
    private final ErrorType erType;
    /** Error message */
    private final String erText;

    private final Range range;

    /**
     * Construct a semantic error, formats an error message from template in {@link ErrorType}
     *
     * @param erType Type of an error
     * @param o Fields to be added into an error message
     */
    public SemanticError(ErrorType erType, Range range, Object... o)
    {
        this.erType = erType;
        this.range = range;
        this.erText = String.format(erType.getText(), o);
    }

    @Override
    public boolean equals(Object s)
    {
        if(s == null || !(s instanceof SemanticError))
            return false;


        SemanticError se = (SemanticError) s;

        if(!this.getErrorType().equals(se.getErrorType()))
            return false;

        if(!this.getErrorMessage().equals(se.getErrorMessage()))
            return false;

        Range a = this.getRange();
        Range b = se.getRange();

        if(a.getStart().getLineNumber() != b.getStart().getLineNumber()
                || a.getStart().getPositionInLine() != b.getStart().getPositionInLine()
                || a.getEnd().getLineNumber() != b.getEnd().getLineNumber()
                || a.getEnd().getPositionInLine() != b.getEnd().getPositionInLine())
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int code = 1;
        code = code * 31 + this.getErrorType().ordinal() + 1;
        code = code * 7;
        code += this.getErrorMessage() == null ? 0 : this.getErrorMessage().hashCode();
        return code;
    }

    /**
     * @return The error type
     */
    public ErrorType getErrorType()
    {
        return erType;
    }

    /**
     * @return The error message
     */
    public String getErrorMessage()
    {
        return erText;
    }

    public Range getRange()
    {
        return this.range;
    }
}

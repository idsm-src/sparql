package cz.iocb.sparql.engine.error;



/**
 * Severity of a {@link TranslateMessage}: an error prevents the query from being executed, a warning does not.
 */
public enum MessageCategory
{
    /**
     * The query cannot be executed.
     */
    ERROR("error"),

    /**
     * The query is executed, but something is suspicious.
     */
    WARNING("warning");

    /**
     * Lower-case name used in the protocol output.
     */
    private String text;

    /**
     * Creates the category with its output name.
     *
     * @param text the lower-case output name
     */
    MessageCategory(String text)
    {
        this.text = text;
    }


    /**
     * Lower-case name of the category used in the protocol output.
     *
     * @return lower-case name of the category used in the protocol output
     */
    public String getText()
    {
        return text;
    }
}

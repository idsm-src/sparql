package cz.iocb.chemweb.server.sparql.parser.error;


public enum ErrorType
{
    parsingException("Exception during parsing: %s."),

    syntaxError("'%s'"),

    wrongNumberOfValues("The number of variables doesn't match the number of values."),

    unknownPrefix("Could not resolve the prefix: '%s'"),

    invalidProcedureCallObject("The procedure '%s' requires abbreviated blank node syntax for its parameters."),

    invalidMultiProcedureCallSubject("The procedure '%s' requires an abbreviated blank node syntax as its result."),

    invalidMultiProcedureCallPredicateCombinaion(
            "Cannot combine procedure call with multiple return values with other predicates."),

    invalidProcedureCallSubject(
            "The procedure '%s' requires a node or an abbreviated blank node syntax as its result."),

    invalidProcedureCallPropertyPathCombinaion("Cannot combine procedure call with property paths."),

    invalidProcedureParameterValue("Procedure parameter value name has to be an IRI."),

    invalidProcedureParameterValueNumber("Procedure parameter value can have only one value."),

    invalidProcedureResultValue("Procedure return value name has to be an IRI."),

    invalidProcedureResultValueNumber("Procedure return value can have only one value."),

    invalidBaseIri("Base IRI must be absolute."),

    malformedIri("IRI is malformed.");



    /**
     * Error message template.
     */
    private final String text;

    /**
     * @param text Error message template in a form of formated String.
     */
    ErrorType(String text)
    {
        this.text = text;
    }

    /**
     * Get error message template.
     *
     * @return Error message template
     */
    public String getText()
    {
        return this.text;
    }
}

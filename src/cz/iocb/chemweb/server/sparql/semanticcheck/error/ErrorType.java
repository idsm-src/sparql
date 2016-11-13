package cz.iocb.chemweb.server.sparql.semanticcheck.error;

/**
 * Type of an error occurring in a SPARQL query.
 *
 */
public enum ErrorType
{
    /**
     * Incorrect - unknown predicate according to ontology. Formatted string takes args: 1. String - incorrect
     * predicate.
     */
    incorrectPredicate("Incorrect Predicate: Predicate '%s' could not be found in the ontology."),
    /**
     * Path is incorrect - based on ontology, it is impossible to connect the predicates ranges with domains. Formatted
     * string takes String args: 1. full incorrect path 2. first part of path 3. expected type on first part range 4.
     * second part of path 5. expected type on second part domain
     */
    incorrectPathConnection("Incorrect Path Connection: Path '%s' is incorrect. Path '%s' ends with type '%s' " + "but path '%s' starts with '%s'"),
    /**
     * Repeated path is invalid - based on ontology, it is impossible to connect the predicates ranges with domains.
     * Formatted string takes String args: 1. full incorrect path 3. expected type on first part range 5. expected type
     * on second part domain
     */
    incorrectPathRepetition("Incorrect Path Repetition: Repeated path '%s' is invalid. Path ends with type '%s' " + "but starts with incompatible type '%s'"),
    /**
     * Repeated Path is incorrect - based on ontology, it is impossible to omit path element with zero possible
     * repetitions. Formatted string takes String args: 1. full incorrect path 3. repeated path
     */
    incorrectPathOmit("Incorrect Path Omit: Subpath cannot be omitted in path '%s'. Wrong subpath: '%s'."),

    /**
     * Variable has a different expected type in local context than in global context. Formatted string takes String
     * args: 1. variable 2. expected type 3. inferred type
     */
    inferenceErrorVar("Inference Error: For a variable '%s', type '%s' is expected, " + "but type '%s' was inferred from other part of the query."),

    inferenceErrorNamedBlankNode("Inference Error: For a blank node '%s', type '%s' is expected, "
            + "but type '%s' was inferred from other part of the query."),

    inferenceErrorBlankNode("Inference Error: For a blank node, type '%s' is expected, "
            + "but type '%s' was inferred from other part of the query."),

    /**
     * Literal type is different then expected. Formatted string takes String args: 1. literal 2. type found 3. inferred
     * type
     */
    literalError("Literal Error: Literal '%s' is of type '%s', but should be '%s'."),

    langLiteralError("Literal Error: Literal '%s' is of type '%s', but should be lang-tagged.");
    // /**
    // * IRI type is different then expected or is not in the database.
    // * Formatted string takes String args:
    // * 1. IRI
    // * 3. inferred type
    // */
    // IRIError
    // ("IRI '%s' is incorrect. It is either missing in the database or "
    // + "type is incompatible with '%s'");
    //
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

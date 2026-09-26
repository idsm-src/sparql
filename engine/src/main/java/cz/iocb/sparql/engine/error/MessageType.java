package cz.iocb.sparql.engine.error;

import static cz.iocb.sparql.engine.error.MessageCategory.ERROR;
import static cz.iocb.sparql.engine.error.MessageCategory.WARNING;



/**
 * Kinds of messages reported during parsing and translation, each with its severity and a {@link String#format}
 * template of its text.
 */
public enum MessageType
{
    /**
     * Syntax error reported by the ANTLR parser; the argument is the parser message.
     */
    syntaxError(ERROR, "'%s'"),

    /**
     * The request is a SPARQL Update, which the engine does not support.
     */
    unsupportedUpdateCommand(ERROR, "SPARQL Update operations are not supported."),

    /**
     * A VALUES row has a different number of values than there are variables.
     */
    wrongNumberOfValues(ERROR, "The number of variables doesn't match the number of values."),

    /**
     * A prefixed name uses a prefix that is neither declared nor configured.
     */
    unknownPrefix(ERROR, "Could not resolve the prefix: '%s'"),

    /**
     * The object of a procedure call triple is not a blank node property list of parameters.
     */
    invalidProcedureCallObject(ERROR, "The procedure '%s' requires blank node syntax for its parameters."),

    /**
     * The subject of a multi-result procedure call is not a blank node property list of results.
     */
    invalidMultiProcedureCallSubject(ERROR, "The procedure '%s' requires an blank node syntax as its result."),

    /**
     * The predicate of a procedure call is a property path.
     */
    invalidProcedureCallPropertyPathCombinaion(ERROR, "Cannot combine procedure call with property paths."),

    /**
     * A parameter of a procedure call is named by something other than an IRI.
     */
    invalidProcedureParameterValue(ERROR, "Procedure parameter value name has to be an IRI."),

    /**
     * A result of a procedure call is named by something other than an IRI.
     */
    invalidProcedureResultValue(ERROR, "Procedure return value name has to be an IRI."),

    /**
     * The same blank node label occurs in two basic graph patterns.
     */
    reuseOfBlankNode(ERROR, "Blank node '%s' is already used in another basic graph pattern."),

    /**
     * The parameter blank node of a procedure call occurs elsewhere as well.
     */
    reuseOfParameterNode(ERROR, "Parameter node of the procedure '%s' is already used."),

    /**
     * The result blank node of a procedure call occurs elsewhere as well.
     */
    reuseOfResultNode(ERROR, "Result node of the procedure '%s' is already used."),

    /**
     * A variable serving as the parameter node of a procedure call occurs elsewhere as well.
     */
    invalidParameterVariableOccurence(ERROR, "Variable '%s' is already used as a procedure parameter node."),

    /**
     * A variable serving as the result node of a procedure call occurs elsewhere as well.
     */
    invalidResultVariableOccurence(ERROR, "Variable '%s' is already used as a procedure result node."),

    /**
     * A blank node serving as the parameter node of a procedure call occurs elsewhere as well.
     */
    invalidParameterBlankNodeOccurence(ERROR, "Blank node '_:%s' is already used as a procedure parameter node."),

    /**
     * A blank node serving as the result node of a procedure call occurs elsewhere as well.
     */
    invalidResultBlankNodeOccurence(ERROR, "Blank node '_:%s' is already used as a procedure result node."),

    /**
     * The BASE declaration is a relative IRI.
     */
    invalidBaseIri(ERROR, "Base IRI must be absolute."),

    /**
     * An IRI reference cannot be parsed as an IRI.
     */
    malformedIri(ERROR, "IRI is malformed."),

    /**
     * A function IRI is neither a datatype nor a configured extension function.
     */
    unimplementedFunction(ERROR, "Function '%s' is not implemented."),

    /**
     * A function is called with a wrong number of arguments.
     */
    wrongCountOfParameters(ERROR, "Function '%s' requires %d parameter(s)"),

    /**
     * Internal error: an EXISTS expression could not be translated.
     */
    invalidExistsExpressionTranslation(ERROR, "Invalid translation of ExistsExpression."),

    /**
     * Internal error: an extension function call could not be translated.
     */
    unsupportedFunctionCall(ERROR, "Call of FunctionCallExpression is not supported."),

    /**
     * A literal cannot be converted to the value space of its datatype.
     */
    malformedLiteral(ERROR, "Cannot convert literal value of the given type."),

    /**
     * A projection or HAVING expression of a grouped select uses a variable that is neither grouped nor aggregated.
     */
    invalidVariableOutsideAggregate(
            ERROR,
            "Variable '%s' is used outside aggregate, but it is not mentioned in the GROUP BY clause."),

    /**
     * {@code SELECT *} is combined with GROUP BY.
     */
    invalidProjection(ERROR, "Use of * is only permitted when the query does not use grouping."),

    /**
     * A variable introduced by BIND already occurs in a preceding pattern of the group.
     */
    variableUsedBeforeBind(
            ERROR,
            "Variable '%s' introduced by BIND clauses cannot be used in any pattern preceding this clause."),

    /**
     * A variable introduced by a GROUP BY expression already occurs in the query.
     */
    variableUsedBeforeGroupBy(
            ERROR,
            "Variable '%s' introduced by GROUP BY clauses cannot be used in any pattern preceding this clause."),

    /**
     * A variable introduced by a projection expression already occurs in the select.
     */
    variableUsedBeforeProjection(
            ERROR,
            "Variable '%s' introduced by projection have to be unused in select clause before."),

    /**
     * A result blank node pattern of a procedure call names no result.
     */
    noResultParameter(
            ERROR,
            "At least one result parameter has to be specified if a result blank node pattern is used."),

    /**
     * A result predicate is used twice in a result blank node pattern.
     */
    repeatOfResultPredicate(ERROR, "Predicate '%s' cannot be used twice in a result blank node pattern."),

    /**
     * A variable receives two results of one procedure call.
     */
    repeatOfResultVariable(ERROR, "Variable '%s' cannot be used twice in a result blank node pattern."),

    /**
     * A result predicate is not a result of the called procedure.
     */
    invalidResultPredicate(ERROR, "Procedure '%s' has no result parameter called '%s'."),

    /**
     * A parameter predicate is used twice in one procedure call.
     */
    repeatOfParameterPredicate(ERROR, "Procedure parameter '%s' cannot be used twice."),

    /**
     * A parameter predicate is not a parameter of the called procedure.
     */
    invalidParameterPredicate(ERROR, "Procedure parameter '%s' is not suitable for the procedure '%s'."),

    /**
     * A mandatory parameter is missing in a procedure call.
     */
    missingParameterPredicate(
            ERROR,
            "Procedure parameter '%s' was not specified (and has no default value configured) for the procedure call of '%s'."),

    /**
     * A variable is projected twice.
     */
    repeatOfProjectionVariable(ERROR, "Projection variable '%s' cannot be used twice."),

    /**
     * A procedure parameter is given by a variable that cannot be bound at that point.
     */
    unboundedParameterValue(ERROR, "Procedure parameter '%s' uses unbounded value."),

    /**
     * A procedure is called inside a SERVICE pattern of a foreign endpoint.
     */
    procedureCallInsideService(ERROR, "Procedure call cannot be used inside service."),

    /**
     * A procedure is called inside a GRAPH pattern.
     */
    procedureCallInsideGraph(ERROR, "Procedure '%s' cannot be used inside graph."),

    /**
     * An aggregate is used outside the projections, HAVING or ORDER BY of a select.
     */
    invalidContextOfAggregate(ERROR, "Aggregates are allowed only in result sets."),

    /**
     * A variable does not occur anywhere in the WHERE clause.
     */
    unusedVariable(ERROR, "Variable '%s' is not used in the WHERE clause."),

    /**
     * A MINUS pattern shares no variable with the preceding patterns, so it never removes anything.
     */
    unnecessaryMinus(ERROR, "The MINUS clause has no effect."),

    /**
     * A variable is listed twice in a VALUES clause.
     */
    repeatOfValuesVariable(ERROR, "Variable '%s' cannot be used twice in a VALUES clause."),

    /**
     * A SERVICE call would be issued for more context solutions than allowed.
     */
    serviceContextLimitExceeded(ERROR, "The limit of service calls has been exceeded."),

    /**
     * A SERVICE call returned more solutions than allowed.
     */
    serviceResultLimitExceeded(ERROR, "The limit of service results has been exceeded."),

    /**
     * A remote SERVICE endpoint failed to evaluate the pattern.
     */
    badServiceEndpoint(ERROR, "The SERVICE pattern cannot be evaluated by endpoint '%s'."),

    /**
     * A string literal contains an unpaired UTF-16 surrogate.
     */
    partialSurrogatePair(ERROR, "Literal contains a codepoint that is half of a surrogate pair"),

    /**
     * The base direction of a language-tagged literal is neither {@code ltr} nor {@code rtl}.
     */
    invalidBaseDirection(ERROR, "Base direction '%s' is not valid, it must be either 'ltr' or 'rtl'."),

    /**
     * A reifier or an annotation block follows the object of a triple whose predicate is a property path other than an
     * IRI or a variable.
     */
    invalidAnnotationPropertyPath(ERROR, "Reifier or annotation syntax cannot be used with a property path."),

    /**
     * A literal uses a datatype unknown to the configuration.
     */
    unsupportedDatatype(WARNING, "datatype '%s' is not supported"),

    /**
     * A literal spells out a datatype that has a shorthand form (numeric, boolean).
     */
    invalidDatatype(WARNING, "datatype '%s' should not be specified explicitly"),

    /**
     * A literal has a lexical form invalid for its datatype.
     */
    invalidLexicalForm(WARNING, "lexical form '%s' is not valid"),

    /**
     * A language tag does not conform to BCP 47.
     */
    invalidLanguageTag(WARNING, "language tag '%s' is not valid"),

    /**
     * The label of a VERSION declaration is not one of the SPARQL version labels.
     */
    unknownVersionLabel(WARNING, "version label '%s' is not recognized");


    /**
     * Format template of the message text.
     */
    private final String text;

    /**
     * Severity of the message.
     */
    private final MessageCategory category;


    /**
     * Creates the message type with its severity and text template.
     *
     * @param category the severity
     * @param text format template of the message text
     */
    MessageType(MessageCategory category, String text)
    {
        this.category = category;
        this.text = text;
    }


    /**
     * Severity of the message.
     *
     * @return severity of the message
     */
    public MessageCategory getCategory()
    {
        return category;
    }


    /**
     * Format template of the message text.
     *
     * @return format template of the message text
     */
    public String getText()
    {
        return text;
    }
}

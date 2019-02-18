package cz.iocb.chemweb.server.sparql.error;

import static cz.iocb.chemweb.server.sparql.error.MessageCategory.ERROR;
import static cz.iocb.chemweb.server.sparql.error.MessageCategory.WARNING;



public enum MessageType
{
    syntaxError(ERROR, "'%s'"),

    wrongNumberOfValues(ERROR, "The number of variables doesn't match the number of values."),

    unknownPrefix(ERROR, "Could not resolve the prefix: '%s'"),

    invalidProcedureCallObject(ERROR, "The procedure '%s' requires abbreviated blank node syntax for its parameters."),

    invalidMultiProcedureCallSubject(
            ERROR,
            "The procedure '%s' requires an abbreviated blank node syntax as its result."),

    invalidMultiProcedureCallPredicateCombinaion(
            ERROR,
            "Cannot combine procedure call with multiple return values with other predicates."),

    invalidProcedureCallSubject(
            ERROR,
            "The procedure '%s' requires a node or an abbreviated blank node syntax as its result."),

    invalidProcedureCallPropertyPathCombinaion(ERROR, "Cannot combine procedure call with property paths."),

    invalidProcedureParameterValue(ERROR, "Procedure parameter value name has to be an IRI."),

    invalidProcedureParameterValueNumber(ERROR, "Procedure parameter value can have only one value."),

    invalidProcedureResultValue(ERROR, "Procedure return value name has to be an IRI."),

    invalidProcedureResultValueNumber(ERROR, "Procedure return value can have only one value."),

    invalidBaseIri(ERROR, "Base IRI must be absolute."),

    malformedIri(ERROR, "IRI is malformed."),





    unimplementedFunction(ERROR, "Function '%s' is not implemented."),

    wrongCountOfParameters(ERROR, "Function '%s' requires %d parameter(s)"),

    invalidExistsExpressionTranslation(ERROR, "Invalid translation of ExistsExpression."),

    unsupportedFunctionCall(ERROR, "Call of FunctionCallExpression is not supported."),

    malformedLiteral(ERROR, "Cannot convert literal value of the given type."),

    invalidVariableOutsideAggregate(
            ERROR,
            "Variable '%s' is used outside aggregate, but it is not mentioned in the GROUP BY clause."),

    invalidVariableInAggregate(
            ERROR,
            "Variable '%s' mentioned in the GROUP BY clause cannot be used in the aggregate."),

    invalidProjection(ERROR, "Use of SELECT * is only permitted when the query does not have a GROUP BY clause."),

    variableUsedBeforeBind(
            ERROR,
            "Variable '%s' introduced by BIND or GROUP BY clauses cannot be used in any pattern preceding this clause."),

    noResultParameter(
            ERROR,
            "At least one result parameter has to be specified if a result blank node pattern is used."),

    repeatOfResultPredicate(ERROR, "Predicate '%s' cannot be used twice in a result blank node pattern."),

    repeatOfResultVariable(ERROR, "Variable '%s' cannot be used twice in a result blank node pattern."),

    invalidResultPredicate(ERROR, "Procedure '%s' has no result parameter called '%s'."),

    repeatOfParameterPredicate(ERROR, "Procedure parameter '%s' cannot be used twice."),

    invalidParameterPredicate(ERROR, "Procedure parameter '%s' is not suitable for the procedure '%s'."),

    missingParameterPredicate(
            ERROR,
            "Procedure parameter '%s' was not specified (and has no default value configured) for the procedure call of '%s'."),

    repeatOfProjectionVariable(ERROR, "Projection variable '%s' cannot be used twice."),

    unboundedVariableParameterValue(
            ERROR,
            "Procedure parameter '%s' uses unbounded variable '%s', which is not permited."),

    unboundedBlankNodeParameterValue(
            ERROR,
            "Procedure parameter '%s' uses unbounded blank node, which is not permited."),

    procedureCallInsideService(ERROR, "Procedure call cannot be used inside service."),

    procedureCallInsideGraph(ERROR, "Procedure '%s' cannot be used inside graph."),

    invalidContextOfAggregate(ERROR, "Aggregates are allowed only in result sets."),

    unusedVariable(ERROR, "Variable '%s' is not used in the WHERE clause."),

    unnecessaryMinus(ERROR, "The MINUS clause has no effect."),

    repeatOfValuesVariable(ERROR, "Variable '%s' cannot be used twice in a VALUES clause."),

    nestedAggregateFunction(ERROR, "Aggregate functions cannot be nested."),

    serviceContextLimitExceeded(ERROR, "The limit of service calls has been exceeded."),

    serviceResultLimitExceeded(ERROR, "The limit of service results has been exceeded."),

    badServiceEndpoint(ERROR, "The SERVICE pattern cannot be evaluated by endpoint '%s'."),

    unsupportedDatatype(WARNING, "datatype '%s' is not supported"),

    invalidDatatype(WARNING, "datatype '%s' should not be specified explicitly"),

    invalidLexicalForm(WARNING, "lexical form '%s' is not valid"),

    invalidLanguageTag(WARNING, "language tag '%s' is not valid");


    private final String text;
    private final MessageCategory category;


    MessageType(MessageCategory category, String text)
    {
        this.category = category;
        this.text = text;
    }


    public MessageCategory getCategory()
    {
        return category;
    }


    public String getText()
    {
        return text;
    }
}

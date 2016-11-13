package cz.iocb.chemweb.server.sparql.translator.error;



public enum ErrorType
{
    unimplementedFunction("Function '%s' is not implemented."),

    invalidExistsExpressionTranslation("Invalid translation of ExistsExpression."),

    unsupportedFunctionCall("Call of FunctionCallExpression is not supported."),

    malformedLiteral("Cannot convert literal value of the given type."),

    invalidVariableOutsideAggregate(
            "Variable '%s' is used outside aggregate, but it is not mentioned in the GROUP BY clause."),

    invalidVariableInAggregate("Variable '%s' mentioned in the GROUP BY clause cannot be used in the aggregate."),

    invalidProjection("Use of SELECT * is only permitted when the query does not have a GROUP BY clause."),

    variableUsedBeforeBind(
            "Variable '%s' introduced by BIND or GROUP BY clauses cannot be used in any pattern preceding this clause."),

    noResultParameter("At least one result parameter has to be specified if a result blank node pattern is used."),

    repeatOfResultPredicate("Predicate '%s' cannot be used twice in a result blank node pattern."),

    repeatOfResultVariable("Variable '%s' cannot be used twice in a result blank node pattern."),

    invalidResultPredicate("Procedure '%s' has no result parameter called '%s'."),

    repeatOfParameterPredicate("Procedure parameter '%s' cannot be used twice."),

    invalidParameterPredicate("Procedure parameter '%s' is not suitable for the procedure '%s'."),

    missingParameterPredicate(
            "Procedure parameter '%s' was not specified (and has no default value configured) for the procedure call of '%s'."),

    repeatOfProjectionVariable("Projection variable '%s' cannot be used twice."),

    unboundedVariableParameterValue("Procedure parameter '%s' uses unbounded variable '%s', which is not permited."),

    unboundedBlankNodeParameterValue("Procedure parameter '%s' uses unbounded blank node, which is not permited."),

    procedureCallInsideService("Procedure call cannot be used inside service."),

    procedureCallInsideGraph("Procedure '%s' cannot be used inside graph '%s'."),

    invalidContextOfAggregate("Aggregates are allowed only in result sets."),

    unusedVariable("Variable '%s' is not used in the WHERE clause."),

    unnecessaryMinus("The MINUS clause has no effect.");

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

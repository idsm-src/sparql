package cz.iocb.chemweb.server.sparql.translator.visitor.expressions;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BracketedExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BuiltInCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.ExistsExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.FunctionCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.InExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.expression.UnaryExpression;
import cz.iocb.chemweb.server.sparql.translator.Pair;
import cz.iocb.chemweb.server.sparql.translator.error.ErrorType;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;



/**
 * Auxiliary translator class for handling the expressions translations based on the right context in the query. The
 * translation is done in the form of visitor design pattern (extending general ElementVisitor class).
 *
 */
public class ExpressionTranslator extends ElementVisitor<String>
{
    /**
     * Constructs translator object (with no translate flags).
     *
     * @param exceptions Output list of TranslateExceptions.
     */
    public ExpressionTranslator(List<TranslateException> exceptions, List<TranslateException> warnings)
    {
        this(exceptions, warnings, null);
    }


    /**
     * Constructs translator object (with specified translate flags).
     *
     * @param exceptions Output list of TranslateExceptions.
     * @param translateModes Specified translate flags.
     */
    public ExpressionTranslator(List<TranslateException> exceptions, List<TranslateException> warnings,
            EnumSet<ExpressionTranslateFlag> translateModes)
    {
        if(translateModes == null)
            translateModes = EnumSet.noneOf(ExpressionTranslateFlag.class);

        this.exceptions = exceptions;
        this.warnings = warnings;
        this.translateFlags = translateModes;
    }


    /**
     * Sets variables used in the current context (block) to be used in the translator object.
     *
     * @param variables Used variables in the query.
     * @return The current object itself.
     */
    public ExpressionTranslator setVariables(HashMap<String, String> variables)
    {
        this.variables = variables;
        return this;
    }


    /**
     * Translates the expression.
     *
     * @param expression General expression pattern.
     * @return String of a translated expression.
     */
    public String translate(Expression expression)
    {
        return this.visitElement(expression);
    }


    /**
     * Translates the binary expression.
     *
     * @param binaryExpression Binary expression.
     * @return String of a translated binary expression.
     */
    @Override
    public String visit(BinaryExpression binaryExpression)
    {
        StringBuilder strBuf = new StringBuilder();

        String leftOperand = visitElement(binaryExpression.getLeft());
        String operator = binaryExpression.getOperator().getText();
        String rightOperand = visitElement(binaryExpression.getRight());

        if(operator.equals("/"))
        {
            rightOperand = "NULLIF(" + rightOperand + ", 0)";
        }

        /*
        if(translateFlags.contains(ExpressionTranslateFlag.SPECIAL_RELATION_OPERATORS))
        {
            Pair<String, Boolean> specialRelOperator = mapSpecialRelOperator(operator);
        
            strBuf.append(specialRelOperator.getKey());
        
            String leftOp = leftOperand;
            String rightOp = rightOperand;
        
            if(specialRelOperator.getValue()) // switch operand order
            {
                leftOp = rightOperand;
                rightOp = leftOperand;
            }
            strBuf.append("(").append(leftOp).append(", ").append(rightOp).append(")");
        }
        else
        {
            strBuf.append(leftOperand);
            strBuf.append(" ");
            strBuf.append(mapSparqlOperatorToSql(operator));
            strBuf.append(" ");
            strBuf.append(rightOperand);
        }
        */

        //FIXME: check
        if(translateFlags.contains(ExpressionTranslateFlag.SPECIAL_RELATION_OPERATORS))
        {
            Pair<String, Boolean> specialRelOperator = mapSpecialRelOperator(operator);

            if(!specialRelOperator.getKey().equals(""))
            {
                strBuf.append(specialRelOperator.getKey());

                String leftOp = leftOperand;
                String rightOp = rightOperand;

                if(specialRelOperator.getValue()) // switch operand order
                {
                    leftOp = rightOperand;
                    rightOp = leftOperand;
                }
                strBuf.append("(").append(leftOp).append(", ").append(rightOp).append(")");

                return strBuf.toString();
            }
        }

        strBuf.append(leftOperand);
        strBuf.append(" ");
        strBuf.append(mapSparqlOperatorToSql(operator));
        strBuf.append(" ");
        strBuf.append(rightOperand);


        return strBuf.toString();
    }


    /**
     * Maps special prefix operators for the general SPARQL operators (while
     * ExpressionTranslateFlag.SPECIAL_RELATION_OPERATORS is active)
     *
     * @param operator SPARQL operator.
     * @return Pair of a correponding operator and a boolean value of whether the operators for the function should be
     *         reversed.
     */
    private Pair<String, Boolean> mapSpecialRelOperator(String operator)
    {
        String op = "";
        boolean reverseOperands = false;

        switch(operator)
        {
            case "&&":
                op = "__AND";
                break;
            case "||":
                op = "__OR";
                break;
            case "!":
                op = "__NOT";
                break;
            case "=":
                op = "EQU";
                break;
            case "!=":
                op = "NEQ";
                break;
            case "<":
                op = "LT";
                break;
            case "<=":
                op = "LTE";
                break;
            case ">":
                op = "LT";
                reverseOperands = true;
                break;
            case ">=":
                op = "LTE";
                reverseOperands = true;
                break;
        }

        return new Pair<>(op, reverseOperands);
    }


    /**
     * Translates the IN expression.
     *
     * @param inExpression IN expression.
     * @return String of a translated IN expression.
     */
    @Override
    public String visit(InExpression inExpression)
    {
        StringBuilder strBuf = new StringBuilder();

        if(translateFlags.contains(ExpressionTranslateFlag.SPECIAL_RELATION_OPERATORS))
        {
            if(inExpression.isNegated())
                strBuf.append("__NOT(");

            strBuf.append("ONE_OF_THESE(");

            strBuf.append(translate(inExpression.getLeft()));
            for(Expression expr : inExpression.getRight())
            {
                strBuf.append(", ");
                strBuf.append(translate(expr));
            }

            strBuf.append(")");

            if(inExpression.isNegated())
                strBuf.append(")");
        }
        else
        {
            if(inExpression.isNegated())
                strBuf.append("NOT ");

            strBuf.append(translate(inExpression.getLeft()));

            strBuf.append(" IN (");

            boolean first = true;
            for(Expression expr : inExpression.getRight())
            {
                if(!first)
                    strBuf.append(", ");
                else
                    first = false;

                strBuf.append(translate(expr));
            }

            strBuf.append(")");

            if(inExpression.isNegated())
                strBuf.append(")");
        }

        return strBuf.toString();
    }


    /**
     * Translates the unary expression.
     *
     * @param unaryExpression Unary expression.
     * @return String of a translated unary expression.
     */
    @Override
    public String visit(UnaryExpression unaryExpression)
    {
        StringBuilder strBuf = new StringBuilder();

        strBuf.append(mapSparqlOperatorToSql(unaryExpression.getOperator().getText()));
        strBuf.append(visitElement(unaryExpression.getOperand()));

        return strBuf.toString();
    }


    /**
     * Translates the bracketed expression.
     *
     * @param bracketedExpression Bracketed expression.
     * @return String of a translated bracketed expression.
     */
    @Override
    public String visit(BracketedExpression bracketedExpression)
    {
        return "( " + visitElement(bracketedExpression.getChild()) + " )";
    }


    /**
     * Translates the built-in-call expression.
     *
     * @param builtInCallExpression Built-in-call expression.
     * @return String of a translated built-in-call expression.
     */
    @Override
    public String visit(BuiltInCallExpression builtInCallExpression)
    {
        String sparqlFunctionName = builtInCallExpression.getFunctionName().toUpperCase(Locale.US);
        boolean isDistinct = builtInCallExpression.isDistinct();
        List<Expression> funcArgs = builtInCallExpression.getArguments();

        switch(sparqlFunctionName)
        {
            case "COUNT":
            case "SUM":
            case "MIN":
            case "MAX":
            case "AVG":
            case "COALESCE":
            case "ISNUMERIC":
                return sparqlFunctionName + translateGeneralFunctionArguments(builtInCallExpression);

            case "SAMPLE":
            case "REGEX":
            case "GROUP_CONCAT":
            case "LANG":
            case "DATATYPE":
            case "ISURI":
            case "ISIRI":
            case "ISLITERAL":
            case "STRLANG":
            case "STRLEN":
            case "SUBSTR":
            case "UUID":
            case "STRUUID":
            case "LCASE":
            case "UCASE":
            case "STRSTARTS":
            case "STRENDS":
            case "CONTAINS":
            case "STRBEFORE":
            case "STRAFTER":
            case "CONCAT":
            case "LANGMATCHES":
            case "REPLACE":
            case "RAND":
            case "ABS":
            case "ROUND":
            case "CEIL":
            case "FLOOR":
            case "NOW":
                return mapGeneralFunctionName(sparqlFunctionName, isDistinct)
                        + translateGeneralFunctionArguments(builtInCallExpression);

            case "ISBLANK":
                return translateIsBlankFunction(builtInCallExpression);

            case "IF":
                return translateIfFunction(funcArgs);

            case "BOUND":
                return translateBoundFunction(funcArgs);

            case "SAMETERM":
                return translateSametermFunction(funcArgs);

            case "STRDT":
                return translateStrdtFunction(funcArgs);

            case "STR":
                return "__RDF_STRSQLVAL(" + translateGeneralFunctionArguments(builtInCallExpression) + ", 0)";

            case "URI":
            case "IRI":
                return "__bft ( __bft(" + translateGeneralFunctionArguments(builtInCallExpression) + " , 1), 1)";

            default:
                exceptions.add(new TranslateException(ErrorType.unimplementedFunction, builtInCallExpression.getRange(),
                        sparqlFunctionName));
                return "ERROR_TRANSLATION";
        }
    }


    /**
     * Finds a direct SQL equivalent for a SPARQL function in the query.
     *
     * @param sparqlFunctionName SPARQL function name.
     * @param isDistinct Is distinct modifier applied?
     * @return String of an SQL equivalent function.
     */
    private String mapGeneralFunctionName(String sparqlFunctionName, boolean isDistinct)
    {
        switch(sparqlFunctionName)
        {
            case "SAMPLE":
                return "DB.DBA.SAMPLE";
            case "REGEX":
                return "RDF_REGEX_IMPL";
            case "GROUP_CONCAT":
                return isDistinct ? "DB.DBA.GROUP_CONCAT_DISTINCT" : "DB.DBA.GROUP_CONCAT";
            case "LANG":
                return "DB.DBA.RDF_LANGUAGE_OF_OBJ";
            case "DATATYPE":
                return "DB.DBA.RDF_DATATYPE_OF_OBJ";
            case "ISURI": // fall-through
            case "ISIRI":
                return "DB.DBA.RDF_IS_URI_REF";
            case "ISLITERAL":
                return "DB.DBA.RDF_IS_LITERAL";
            case "STRLANG":
                return "DB.DBA.RDF_STRLANG_IMPL";
            case "STRLEN":
                return "RDF_STRLEN_IMPL";
            case "SUBSTR":
                return "RDF_SUBSTR_IMPL";
            case "UUID":
                return "DB.DBA.RDF_UUID_IMPL";
            case "STRUUID":
                return "RDF_STRUUID_IMPL";
            case "LCASE":
                return "RDF_LCASE_IMPL";
            case "UCASE":
                return "RDF_UCASE_IMPL";
            case "STRSTARTS":
                return "RDF_STRSTARTS_IMPL";
            case "STRENDS":
                return "RDF_STRENDS_IMPL";
            case "CONTAINS":
                return "RDF_CONTAINS_IMPL";
            case "STRBEFORE":
                return "RDF_STRBEFORE_IMPL";
            case "STRAFTER":
                return "RDF_STRAFTER_IMPL";
            case "CONCAT":
                return "RDF_CONCAT_IMPL";
            case "LANGMATCHES":
                return "DB.DBA.RDF_LANGMATCHES";
            case "REPLACE":
                return "DB.DBA.RDF_REPLACE_IMPL";
            case "RAND":
                return "RDF_RAND_IMPL";
            case "ABS":
                return "RDF_ABS_IMPL";
            case "ROUND":
                return "RDF_ROUND_IMPL";
            case "CEIL":
                return "RDF_CEIL_IMPL";
            case "FLOOR":
                return "RDF_FLOOR_IMPL";
            case "NOW":
                return "RDF_NOW_IMPL";
        }

        return "";
    }


    /**
     * Translates argument list of a built-in-call expression.
     *
     * @param builtInCallExpression Built-in-call expression.
     * @return String of an translated argument list.
     */
    private String translateGeneralFunctionArguments(BuiltInCallExpression builtInCallExpression)
    {
        String sparqlFunctionName = builtInCallExpression.getFunctionName().toUpperCase(Locale.US);
        List<Expression> funcArgs = builtInCallExpression.getArguments();
        boolean isDistinct = builtInCallExpression.isDistinct();

        StringBuilder strBuf = new StringBuilder();

        strBuf.append("(");

        boolean zeroArgs = false;
        boolean distinctAlreadyConsidered = false;

        switch(sparqlFunctionName)
        {
            case "UUID":
            case "STRUUID":
            case "RAND":
            case "NOW":
                zeroArgs = true;
                break;
            case "GROUP_CONCAT":
                distinctAlreadyConsidered = true;
                break;
        }

        if(funcArgs.isEmpty() && !zeroArgs)
            strBuf.append("*");
        else
        {
            if(isDistinct && !distinctAlreadyConsidered)
                strBuf.append("DISTINCT ");
        }

        boolean first = true;
        for(Expression funcArg : funcArgs)
        {
            if(!first)
                strBuf.append(",");
            else
                first = false;

            EnumSet<ExpressionTranslateFlag> modes = this.translateFlags;
            modes.add(ExpressionTranslateFlag.SUPPRESS_LITERAL_CONVERSION);
            strBuf.append(new ExpressionTranslator(exceptions, warnings, modes).setVariables(this.variables)
                    .translate(funcArg));
        }

        strBuf.append(")");

        return strBuf.toString();
    }


    /**
     * Translates SPARQL isBLANK function.
     *
     * @param builtInCallExpression Built-in-call expression.
     * @return String of translated isBLANK function.
     */
    private String translateIsBlankFunction(BuiltInCallExpression builtInCallExpression)
    {
        if(builtInCallExpression.getArguments().size() != 1)
            return "NULL"; // ParseException should be already thrown

        //FIXME: use better aproach
        //return "IS_BNODE_IRI_ID(__i2idn (" + translateGeneralFunctionArguments(builtInCallExpression) + "))";
        return "(IS_BNODE_IRI_ID(__i2idn (" + translateGeneralFunctionArguments(builtInCallExpression)
                + ")) AND NOT DB.DBA.RDF_IS_LITERAL(" + translateGeneralFunctionArguments(builtInCallExpression) + "))";
    }


    /**
     * Translates SPARQL IF function.
     *
     * @param funcArgs Argument list.
     * @return String of translated IF function.
     */
    private String translateIfFunction(List<Expression> funcArgs)
    {
        if(funcArgs.size() < 3)
            return "NULL"; // ParseException should be already thrown

        EnumSet<ExpressionTranslateFlag> modes = this.translateFlags;
        modes.add(ExpressionTranslateFlag.SPECIAL_RELATION_OPERATORS);
        String constraint = new ExpressionTranslator(exceptions, warnings, modes).setVariables(this.variables)
                .translate(funcArgs.get(0));

        String trueCaseLiteral = translate(funcArgs.get(1));
        String falseCaseLiteral = translate(funcArgs.get(2));

        StringBuilder strBuf = new StringBuilder();

        strBuf.append("CASE(").append(constraint).append(") ");
        strBuf.append("WHEN 0 THEN (").append(falseCaseLiteral).append(") ");
        strBuf.append("ELSE (").append(trueCaseLiteral).append(") END");

        return strBuf.toString();
    }


    /**
     * Translates SPARQL BOUND function.
     *
     * @param funcArgs Argument list.
     * @return String of translated BOUND function.
     */
    private String translateBoundFunction(List<Expression> funcArgs)
    {
        if(funcArgs.size() != 1)
            return "0"; // ParseException should be already thrown

        Expression expr = funcArgs.get(0);
        if(expr instanceof Variable)
        {
            String argName = ((Variable) expr).getName();
            if(this.variables != null && this.variables.get(argName) != null)
            {
                return "(" + this.variables.get(argName) + " IS NOT NULL)";
            }
        }

        return "0";
    }


    /**
     * Translates SPARQL SAMETERM function.
     *
     * @param funcArgs Argument list.
     * @return String of translated SAMETERM function.
     */
    private String translateSametermFunction(List<Expression> funcArgs)
    {
        if(funcArgs.size() != 2)
            return "0"; // ParseException should be already thrown

        StringBuilder strBuf = new StringBuilder();

        boolean first = true;
        for(Expression funcArg : funcArgs)
        {
            if(!first)
                strBuf.append(" = ");
            else
                first = false;

            EnumSet<ExpressionTranslateFlag> modes = this.translateFlags;
            strBuf.append(new ExpressionTranslator(exceptions, warnings, modes).setVariables(this.variables)
                    .translate(funcArg));
        }

        return strBuf.toString();
    }


    /**
     * Translates SPARQL STRDT function.
     *
     * @param funcArgs Argument list.
     * @return String of translated STRDT function.
     */
    private String translateStrdtFunction(List<Expression> funcArgs)
    {
        if(funcArgs.size() != 2)
            return "NULL"; // ParseException should be already thrown

        Expression arg1 = funcArgs.get(0);

        EnumSet<ExpressionTranslateFlag> modes1 = this.translateFlags;
        modes1.add(ExpressionTranslateFlag.SUPPRESS_LITERAL_CONVERSION);
        String arg1Str = new ExpressionTranslator(exceptions, warnings, modes1).setVariables(this.variables)
                .translate(arg1);

        Expression arg2 = funcArgs.get(1);

        EnumSet<ExpressionTranslateFlag> modes2 = this.translateFlags;
        String arg2Str = new ExpressionTranslator(exceptions, warnings, modes2).setVariables(this.variables)
                .translate(arg2);

        StringBuilder strBuf = new StringBuilder();

        strBuf.append("DB.DBA.RDF_STRDT_IMPL(");
        strBuf.append(arg1Str).append(", ").append(arg2Str).append(")");

        return strBuf.toString();
    }


    /**
     * Translates the EXISTS expression (currently translated separately in the main translator class - shouldn't be
     * called).
     *
     * @param existsExpression EXISTS expression.
     * @return String of a translated EXISTS expression.
     */
    @Override
    public String visit(ExistsExpression existsExpression)
    {
        // probably some invalid use of exist expression; these should be
        // handled specially in the main translator
        exceptions
                .add(new TranslateException(ErrorType.invalidExistsExpressionTranslation, existsExpression.getRange()));
        return "ERROR_TRANSLATION";
    }


    /**
     * Translates the function-call expression (currently not supported).
     *
     * @param functionCallExpression Function-call expression.
     * @return String of a translated function-call expression.
     */
    @Override
    public String visit(FunctionCallExpression functionCallExpression)
    {
        // not supported
        exceptions.add(new TranslateException(ErrorType.unsupportedFunctionCall, functionCallExpression.getRange()));
        return "ERROR_TRANSLATION";
    }


    /**
     * Translates the general IRI.
     *
     * @param iri Input IRI.
     * @return String of a translated IRI.
     */
    @Override
    public String visit(IRI iri)
    {
        String translPrefix = "__bft ( __bft('";
        String translSuffix = "' , 1), 1)";

        if(translateFlags.contains(ExpressionTranslateFlag.SUPPRESS_LITERAL_CONVERSION))
        {
            return "'" + iri.getUri().toString() + "'";
        }
        else
        {
            return translPrefix + iri.getUri().toString() + translSuffix;
        }
    }


    /**
     * Translates the general literal value.
     *
     * @param literal Input literal value.
     * @return String of a translated literal value.
     */
    @Override
    public String visit(Literal literal)
    {
        String retVal = "";

        boolean literalConversionSuppressed = translateFlags
                .contains(ExpressionTranslateFlag.SUPPRESS_LITERAL_CONVERSION);
        boolean longCast = translateFlags.contains(ExpressionTranslateFlag.LONG_CAST);

        String literalVal = literal.getStringValue();
        String literalLangTag = literal.getLanguageTag();
        String literalType;

        IRI typeIRI = literal.getTypeIri();
        if(typeIRI != null)
            literalType = typeIRI.getUri().toString();
        else
            literalType = "NOT_TYPED_STRING";

        switch(LiteralTranslator.checkTypeMatch(literalType, literalVal, literalLangTag, literalConversionSuppressed))
        {
            case TYPE_OK:
                retVal = LiteralTranslator.makeLiteralStr(literalType, literalVal, false, literalLangTag,
                        literalConversionSuppressed, longCast);
                break;
            case CAST_NEEDED:
                retVal = LiteralTranslator.makeLiteralStr(literalType, literalVal, true, literalLangTag,
                        literalConversionSuppressed, longCast);
                break;
            case TYPE_ERROR:
                exceptions.add(new TranslateException(ErrorType.malformedLiteral, literal.getRange()));
                retVal = "ERROR_TRANSLATION";
                break;
            default:
                break;
        }

        return retVal;
    }


    /**
     * Translates the general variable.
     *
     * @param variable Input literal value.
     * @return String of a translated variable.
     */
    @Override
    public String visit(Variable variable)
    {
        String varName = variable.getName();

        if(this.variables == null)
            return "\"" + varName + "\"";
        else if(this.variables.get(varName) == null)
            return "NULL";
        else
            return this.variables.get(varName);
    }

    /**
     * Map SPARQL operator to its SQL equivalent.
     *
     * @param sparqlOperator SPARQL operator.
     * @return String of an equivalent SQL operator.
     */
    private String mapSparqlOperatorToSql(String sparqlOperator)
    {
        switch(sparqlOperator)
        {
            case "&&":
                return "AND";
            case "||":
                return "OR";
            case "!":
                return "NOT ";
            case "!=":
                return "<>";
        }

        return sparqlOperator;
    }

    protected EnumSet<ExpressionTranslateFlag> translateFlags = null;
    protected HashMap<String, String> variables = null;
    private final List<TranslateException> exceptions;
    private final List<TranslateException> warnings;
}

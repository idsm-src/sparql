package cz.iocb.chemweb.server.sparql.translator.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.translator.expression.TranslateRequest.BOOLEAN;
import static cz.iocb.chemweb.server.sparql.translator.expression.TranslateRequest.DATE;
import static cz.iocb.chemweb.server.sparql.translator.expression.TranslateRequest.DATETIME;
import static cz.iocb.chemweb.server.sparql.translator.expression.TranslateRequest.EBV;
import static cz.iocb.chemweb.server.sparql.translator.expression.TranslateRequest.IRI;
import static cz.iocb.chemweb.server.sparql.translator.expression.TranslateRequest.NUMERIC;
import static cz.iocb.chemweb.server.sparql.translator.expression.TranslateRequest.RDFTERM;
import static cz.iocb.chemweb.server.sparql.translator.expression.TranslateRequest.STRING;
import java.util.List;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
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
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;



public class ExpressionTranslateVisitor extends ParameterizedTranslateVisitor<TranslatedExpression, TranslateRequest>
{
    private final List<ResourceClass> classes;
    private final VariableAccessor variableAccessor;


    public ExpressionTranslateVisitor(TranslateVisitor parentVisitor, VariableAccessor variableAccessor)
    {
        this.classes = parentVisitor.getResourceClasses();
        this.variableAccessor = variableAccessor;
    }


    @Override
    public TranslatedExpression visit(BinaryExpression binaryExpression, TranslateRequest request)
    {
        BinaryExpression.Operator operator = binaryExpression.getOperator();

        switch(operator)
        {
            case And:
            case Or:
            {
                TranslatedExpression left = visitElement(binaryExpression.getLeft(), EBV);
                TranslatedExpression right = visitElement(binaryExpression.getRight(), EBV);

                if(left == null && right == null)
                    return null;

                left = createEBV(left);
                right = createEBV(right);

                if(operator == BinaryExpression.Operator.And && (left.isFalse() || right.isFalse()))
                    return new TranslatedExpression(xsdBoolean, false, "false");

                if(operator == BinaryExpression.Operator.Or && (left.isTrue() || right.isTrue()))
                    return new TranslatedExpression(xsdBoolean, false, "true");

                String code = "(" + left.getCode() + operator.getCode() + right.getCode() + ")";
                return new TranslatedExpression(xsdBoolean, left.canBeNull() || right.canBeNull(), code);
            }

            case Add:
            case Subtract:
            case Multiply:
            case Divide:
            {
                //FIXME: PostgreSQL does not conform to IEEE 754 division by zero semantics

                TranslatedExpression left = visitElement(binaryExpression.getLeft(), NUMERIC);
                TranslatedExpression right = visitElement(binaryExpression.getRight(), NUMERIC);

                if(left == null || right == null)
                    return null;

                if(!isNumericClass(left.getResourceClass()) || !isNumericClass(right.getResourceClass()))
                    return null; //TODO: add warning


                ResourceClass resClass = getWiderNumericClass(left.getResourceClass(), right.getResourceClass());

                if(operator != BinaryExpression.Operator.Divide)
                {
                    String code = "(" + left.getCode() + operator.getCode() + right.getCode() + ")";
                    return new TranslatedExpression(resClass, left.canBeNull() || right.canBeNull(), code);
                }
                else if(resClass != xsdInteger)
                {
                    String code = "(" + left.getCode() + operator.getCode() + "nullif(" + right.getCode() + ", 0))";
                    return new TranslatedExpression(resClass, true, code);
                }
                else
                {
                    String code = "(" + left.getCode() + operator.getCode() + "(nullif(" + right.getCode() + ", 0))::"
                            + xsdDecimal.getSqlType(0) + ")";
                    return new TranslatedExpression(xsdDecimal, true, code);
                }
            }

            case Equals:
            case GreaterThan:
            case GreaterThanOrEqual:
            case LessThan:
            case LessThanOrEqual:
            {
                Variable leftVar = getStandaloneVariable(binaryExpression.getLeft());
                Variable rightVar = getStandaloneVariable(binaryExpression.getRight());

                if(leftVar != null && rightVar != null)
                {
                    UsedVariable left = variableAccessor.getUsedVariable(leftVar);
                    UsedVariable right = variableAccessor.getUsedVariable(rightVar);

                    if(left == null || right == null)
                        return null;


                    List<ResourceClass> leftClasses = left.getClasses();
                    List<ResourceClass> rightClasses = right.getClasses();


                    StringBuilder builder = new StringBuilder();
                    int comparison = 0;


                    /* numeric comparison */
                    List<ResourceClass> leftNumericClasses = leftClasses.stream().filter(s -> isNumericClass(s))
                            .collect(Collectors.toList());
                    List<ResourceClass> rightNumericClasses = rightClasses.stream().filter(s -> isNumericClass(s))
                            .collect(Collectors.toList());


                    if(leftNumericClasses.size() > 0 && rightNumericClasses.size() > 0)
                    {
                        comparison++;

                        generateNumericAccess(builder, leftVar, leftNumericClasses);
                        builder.append(operator.getCode());
                        generateNumericAccess(builder, rightVar, rightNumericClasses);
                    }


                    /* other literal comparison */
                    for(LiteralClass literalClass : new LiteralClass[] { xsdString, xsdBoolean, xsdDateTime, xsdDate })
                    {
                        if(leftClasses.contains(literalClass) && rightClasses.contains(literalClass))
                        {
                            appendComma(builder, comparison > 0);
                            comparison++;

                            assert literalClass.getPartsCount() == 1;
                            builder.append(variableAccessor.variableAccess(leftVar, literalClass, 0));
                            builder.append(operator.getCode());
                            builder.append(variableAccessor.variableAccess(rightVar, literalClass, 0));
                        }
                    }


                    boolean canBeNull;

                    /* additional comparison */
                    if(operator != BinaryExpression.Operator.Equals)
                    {
                        if(comparison == 0)
                            return null;

                        canBeNull = left.canBeNull() || right.canBeNull()
                                || !areClassesCompatible(leftClasses, rightClasses);
                    }
                    else
                    {
                        List<ResourceClass> leftOtherClasses = leftClasses.stream()
                                .filter(s -> !isSimpleLiteralClass(s)).collect(Collectors.toList());
                        List<ResourceClass> rightOtherClasses = rightClasses.stream()
                                .filter(s -> !isSimpleLiteralClass(s)).collect(Collectors.toList());

                        for(ResourceClass resourceClass : leftOtherClasses)
                        {
                            if(rightOtherClasses.contains(resourceClass))
                            {
                                appendComma(builder, comparison > 0);
                                comparison++;


                                for(int i = 0; i < resourceClass.getPartsCount(); i++)
                                {
                                    appendAnd(builder, i > 0);

                                    builder.append(variableAccessor.variableAccess(leftVar, resourceClass, i));
                                    builder.append(operator.getCode());
                                    builder.append(variableAccessor.variableAccess(rightVar, resourceClass, i));
                                }
                            }
                        }


                        canBeNull = left.canBeNull() || right.canBeNull()
                                || hasLiteralClass(leftClasses) && hasLiteralClass(rightClasses);


                        if(hasIriClass(left.getClasses()) || hasIriClass(right.getClasses()))
                        {
                            appendComma(builder, comparison > 0);
                            comparison++;


                            if(!hasLiteralClass(leftClasses) || !hasLiteralClass(rightClasses))
                            {
                                if(!left.canBeNull() && !right.canBeNull())
                                {
                                    builder.append("false");
                                }
                                else
                                {
                                    builder.append("NULLIF(");

                                    if(left.canBeNull())
                                        generateIsNullCheck(builder, leftVar, leftClasses);

                                    if(left.canBeNull() && right.canBeNull())
                                        builder.append(" OR ");

                                    if(right.canBeNull())
                                        generateIsNullCheck(builder, rightVar, rightClasses);

                                    builder.append(", true)");
                                }
                            }
                            else
                            {
                                List<ResourceClass> leftIriClasses = selectIriClasses(leftClasses);
                                List<ResourceClass> rightIriClasses = selectIriClasses(rightClasses);

                                builder.append("NULLIF(");

                                if(leftIriClasses.size() > 0)
                                {
                                    if(right.canBeNull())
                                        builder.append("(");

                                    generateIsNullCheck(builder, leftVar, leftIriClasses);

                                    if(right.canBeNull())
                                    {
                                        builder.append(" OR ");
                                        generateIsNullCheck(builder, rightVar, rightClasses);
                                        builder.append(")");
                                    }
                                }

                                if(leftIriClasses.size() > 0 && rightIriClasses.size() > 0)
                                    builder.append(" AND ");

                                if(rightIriClasses.size() > 0)
                                {
                                    if(left.canBeNull())
                                        builder.append("(");

                                    generateIsNullCheck(builder, rightVar, rightIriClasses);

                                    if(left.canBeNull())
                                    {
                                        builder.append(" OR ");
                                        generateIsNullCheck(builder, leftVar, leftClasses);
                                        builder.append(")");
                                    }
                                }

                                builder.append(", true)");
                            }
                        }
                    }


                    String code = (comparison > 1 ? "COALESCE(" : "(") + builder.toString() + ")";
                    return new TranslatedExpression(xsdBoolean, canBeNull, code);
                }
                else if(leftVar != null || rightVar != null)
                {
                    Variable variable = leftVar != null ? leftVar : rightVar;
                    Expression expression = leftVar != null ? binaryExpression.getRight() : binaryExpression.getLeft();

                    IRI iri = getStandaloneIri(expression);

                    if(iri != null)
                    {
                        /* variable vs iri */

                        if(operator != BinaryExpression.Operator.Equals)
                            return null;

                        UsedVariable usedVariable = variableAccessor.getUsedVariable(variable);
                        IriClass iriClass = getIriClass(iri.getUri().toString());

                        if(usedVariable == null)
                            return null;

                        StringBuilder builder = new StringBuilder();
                        boolean compatible = usedVariable.getClasses().contains(iriClass);

                        if(compatible)
                        {
                            builder.append("(");

                            for(int i = 0; i < iriClass.getPartsCount(); i++)
                            {
                                appendAnd(builder, i > 0);

                                builder.append(variableAccessor.variableAccess(variable, iriClass, i));
                                builder.append(operator.getCode());
                                builder.append(iriClass.getSqlValue(iri, i));
                            }

                            builder.append(")");
                        }
                        else if(usedVariable.canBeNull())
                        {
                            builder.append("NULLIF(");
                            generateIsNullCheck(builder, variable, usedVariable.getClasses());
                            builder.append(", true)");
                        }
                        else
                        {
                            builder.append("false");
                        }


                        return new TranslatedExpression(xsdBoolean, usedVariable.canBeNull(), builder.toString());
                    }
                    else
                    {
                        /* variable vs (general) expression */

                        UsedVariable usedVariable = variableAccessor.getUsedVariable(variable);
                        TranslatedExpression translatedExpression = visitElement(expression);

                        if(usedVariable == null)
                            return null; //TODO: add warning

                        if(translatedExpression == null)
                            return null;


                        TranslateRequest variableRequest = classToRequest(translatedExpression.getResourceClass());
                        TranslatedExpression variableExpression = visitElement(variable, variableRequest);

                        boolean areFullyCompatible = areClassesCompatible(usedVariable.getClasses(),
                                translatedExpression.getResourceClass());


                        String code = null;

                        if(variableExpression != null)
                        {
                            if(leftVar != null)
                                code = "(" + variableExpression.getCode() + operator.getCode()
                                        + translatedExpression.getCode() + ")";
                            else
                                code = "(" + translatedExpression.getCode() + operator.getCode()
                                        + variableExpression.getCode() + ")";
                        }


                        if(operator != BinaryExpression.Operator.Equals || areFullyCompatible)
                        {
                            if(variableExpression == null)
                                return null;
                            else
                                return new TranslatedExpression(xsdBoolean, usedVariable.canBeNull()
                                        || translatedExpression.canBeNull() || !areFullyCompatible, code);
                        }
                        else
                        {
                            List<ResourceClass> selectedClasses = usedVariable.getClasses();
                            boolean selectedCanBeNull = usedVariable.canBeNull();

                            if(isLiteralClass(translatedExpression.getResourceClass()))
                            {
                                selectedClasses = selectIriClasses(usedVariable.getClasses());
                                selectedCanBeNull |= usedVariable.getClasses().size() > selectedClasses.size();
                            }


                            if(selectedClasses.size() == 0)
                            {
                                if(variableExpression == null)
                                    return null;
                                else
                                    return new TranslatedExpression(xsdBoolean, true, code);
                            }
                            else if(!selectedCanBeNull && !translatedExpression.canBeNull())
                            {
                                if(variableExpression == null)
                                    return new TranslatedExpression(xsdBoolean, false, "false");
                                else
                                    return new TranslatedExpression(xsdBoolean, false, "COALESCE(" + code + ", false)");
                            }
                            else
                            {
                                StringBuilder builder = new StringBuilder();

                                if(variableExpression != null)
                                {
                                    builder.append("CASE WHEN (");
                                    builder.append(variableExpression.getCode());
                                    builder.append(") IS NOT NULL THEN ");
                                    builder.append(code);
                                    builder.append(" ELSE ");
                                }

                                builder.append("NULLIF(");

                                if(selectedCanBeNull)
                                    generateIsNullCheck(builder, variable, selectedClasses);

                                if(selectedCanBeNull && translatedExpression.canBeNull())
                                    builder.append(" OR ");

                                if(translatedExpression.canBeNull())
                                    generateIsNullCheck(builder, translatedExpression);

                                builder.append(", true)");

                                if(variableExpression != null)
                                    builder.append(" END");

                                return new TranslatedExpression(xsdBoolean, true, builder.toString());
                            }
                        }
                    }
                }
                else
                {
                    /* expression vs expression */

                    TranslatedExpression left = visitElement(binaryExpression.getLeft(), RDFTERM);
                    TranslatedExpression right = visitElement(binaryExpression.getRight(), RDFTERM);

                    if(left == null || right == null)
                        return null;


                    if(areClassesCompatible(left.getResourceClass(), right.getResourceClass()))
                    {
                        String code = "(" + left.getCode() + operator.getCode() + right.getCode() + ")";
                        return new TranslatedExpression(xsdBoolean, left.canBeNull() || right.canBeNull(), code);
                    }
                    else if(operator != BinaryExpression.Operator.Equals)
                    {
                        return null;
                    }
                    else
                    {
                        if(isLiteralClass(left.getResourceClass()) && isLiteralClass(right.getResourceClass()))
                        {
                            return null;
                        }
                        else if(!left.canBeNull() && !right.canBeNull())
                        {
                            return new TranslatedExpression(xsdBoolean, false, "false");
                        }
                        else
                        {
                            StringBuilder builder = new StringBuilder();

                            builder.append("NULLIF(");

                            if(left.canBeNull())
                                generateIsNullCheck(builder, left);

                            if(left.canBeNull() && right.canBeNull())
                                builder.append(" OR ");

                            if(right.canBeNull())
                                generateIsNullCheck(builder, right);

                            builder.append(", true)");

                            return new TranslatedExpression(xsdBoolean, true, builder.toString());
                        }
                    }
                }
            }

            case NotEquals:
            {
                Expression expression = new BinaryExpression(BinaryExpression.Operator.Equals,
                        binaryExpression.getLeft(), binaryExpression.getRight());

                TranslatedExpression translated = visitElement(expression);

                if(translated == null)
                    return null;

                return new TranslatedExpression(xsdBoolean, translated.canBeNull(),
                        "NOT (" + translated.getCode() + ")");
            }
        }

        assert false;
        return null;
    }


    @Override
    public TranslatedExpression visit(InExpression inExpression, TranslateRequest parameter)
    {
        boolean hasElement = false;
        boolean canBeNull = false;

        StringBuilder builder = new StringBuilder();
        builder.append("(");

        if(inExpression.isNegated())
        {
            BinaryExpression.Operator operator = BinaryExpression.Operator.NotEquals;

            for(Expression right : inExpression.getRight())
            {
                TranslatedExpression compare = visit(new BinaryExpression(operator, inExpression.getLeft(), right));

                if(compare == null)
                {
                    appendAnd(builder, hasElement);
                    hasElement = true;

                    builder.append(" null ");
                    canBeNull |= true;
                }
                else if(!compare.isTrue())
                {
                    appendAnd(builder, hasElement);
                    hasElement = true;

                    builder.append(compare.getCode());
                    canBeNull |= compare.canBeNull();
                }
            }
        }
        else
        {
            BinaryExpression.Operator operator = BinaryExpression.Operator.Equals;

            for(Expression right : inExpression.getRight())
            {
                TranslatedExpression compare = visit(new BinaryExpression(operator, inExpression.getLeft(), right));

                if(compare == null)
                {
                    appendOr(builder, hasElement);
                    hasElement = true;

                    builder.append(" null ");
                    canBeNull |= true;
                }
                else if(!compare.isFalse())
                {
                    appendOr(builder, hasElement);
                    hasElement = true;

                    builder.append(compare.getCode());
                    canBeNull |= compare.canBeNull();
                }
            }
        }

        builder.append(")");


        if(!hasElement)
            return null;

        return new TranslatedExpression(xsdBoolean, canBeNull, builder.toString());
    }


    @Override
    public TranslatedExpression visit(UnaryExpression unaryExpression, TranslateRequest request)
    {
        UnaryExpression.Operator operator = unaryExpression.getOperator();

        switch(operator)
        {
            case Not:
            {
                TranslatedExpression operand = visitElement(unaryExpression.getOperand(), EBV);

                if(operand == null)
                    return null;

                operand = createEBV(operand);

                return new TranslatedExpression(xsdBoolean, operand.canBeNull(), "NOT " + operand.getCode());
            }

            case Minus:
            case Plus:
            {
                TranslatedExpression operand = visitElement(unaryExpression.getOperand(), NUMERIC);

                if(operand == null)
                    return null;

                if(!isNumericClass(operand.getResourceClass()))
                    return null; //TODO: add warning

                return new TranslatedExpression(operand.getResourceClass(), operand.canBeNull(),
                        operator.getCode() + operand.getCode());
            }
        }

        assert false;
        return null;
    }


    @Override
    public TranslatedExpression visit(BracketedExpression bracketedExpression, TranslateRequest request)
    {
        TranslatedExpression child = visitElement(bracketedExpression.getChild(), request);

        if(child == null)
            return null;

        return new TranslatedExpression(child.getResourceClass(), child.canBeNull(), "(" + child.getCode() + ")");
    }


    @Override
    public TranslatedExpression visit(BuiltInCallExpression builtInCallExpression, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(ExistsExpression existsExpression, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(FunctionCallExpression functionCallExpression, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(IRI iri, TranslateRequest request)
    {
        return new TranslatedExpression(IriClass.iriClass, false, iri.getUri().toString());
    }


    @Override
    public TranslatedExpression visit(Literal literal, TranslateRequest request)
    {
        //TODO: fix Literal.getTypeIri() to not return null
        IRI iri = literal.getTypeIri();
        String type = iri != null ? iri.getUri().toString() : "http://www.w3.org/2001/XMLSchema#string";

        Object value = literal.getValue();

        if(type.equals("http://www.w3.org/2001/XMLSchema#string"))
            value = "'" + ((String) value).replaceAll("'", "\\'") + "'";

        for(LiteralClass literalClass : LiteralClass.getClasses())
            if(literalClass.getTypeIri().equals(type))
                return new TranslatedExpression(literalClass, false, value.toString());

        //TODO: report literal with an unsupported type as an error
        return null;
    }


    @Override
    public TranslatedExpression visit(Variable variable, TranslateRequest request)
    {
        UsedVariable usedVariable = variableAccessor.getUsedVariable(variable);

        if(usedVariable == null)
            return null;

        List<ResourceClass> resClasses = usedVariable.getClasses();


        switch(request)
        {
            case EBV:
            {
                List<ResourceClass> ebvClasses = resClasses.stream().filter(s -> isEbvClass(s))
                        .collect(Collectors.toList());

                boolean canBeNull = usedVariable.canBeNull() || usedVariable.getClasses().size() > ebvClasses.size();


                if(ebvClasses.isEmpty())
                {
                    return null;
                }
                else if(ebvClasses.size() == 1)
                {
                    if(ebvClasses.get(0) == xsdBoolean)
                        return new TranslatedExpression(xsdBoolean, canBeNull,
                                variableAccessor.variableAccess(variable, xsdBoolean, 0));
                    else
                        return new TranslatedExpression(xsdBoolean, canBeNull,
                                "ebv(" + variableAccessor.variableAccess(variable, xsdBoolean, 0) + ")");
                }
                else
                {
                    StringBuilder builder = new StringBuilder();
                    builder.append("COALESCE(");

                    for(int i = 0; i < ebvClasses.size(); i++)
                    {
                        appendComma(builder, i > 0);

                        if(ebvClasses.get(i) == xsdBoolean)
                            builder.append(variableAccessor.variableAccess(variable, ebvClasses.get(i), 0));
                        else
                            builder.append(
                                    "ebv(" + variableAccessor.variableAccess(variable, ebvClasses.get(i), 0) + ")");
                    }

                    builder.append(")");
                    return new TranslatedExpression(xsdBoolean, canBeNull, builder.toString());
                }
            }

            case BOOLEAN:
                if(!resClasses.contains(xsdBoolean))
                    return null;
                else
                    return new TranslatedExpression(xsdBoolean,
                            usedVariable.canBeNull() || usedVariable.getClasses().size() > 1,
                            variableAccessor.variableAccess(variable, xsdBoolean, 0));

            case STRING:
                if(!resClasses.contains(xsdString))
                    return null;
                else
                    return new TranslatedExpression(xsdString,
                            usedVariable.canBeNull() || usedVariable.getClasses().size() > 1,
                            variableAccessor.variableAccess(variable, xsdString, 0));

            case DATETIME:
                if(!resClasses.contains(xsdDateTime))
                    return null;
                else
                    return new TranslatedExpression(xsdDateTime,
                            usedVariable.canBeNull() || usedVariable.getClasses().size() > 1,
                            variableAccessor.variableAccess(variable, xsdDateTime, 0));

            case DATE:
                if(!resClasses.contains(xsdDate))
                    return null;
                else
                    return new TranslatedExpression(xsdDate,
                            usedVariable.canBeNull() || usedVariable.getClasses().size() > 1,
                            variableAccessor.variableAccess(variable, xsdDate, 0));

            case INTEGER:
                if(!resClasses.contains(xsdInteger))
                    return null;
                else
                    return new TranslatedExpression(xsdInteger,
                            usedVariable.canBeNull() || usedVariable.getClasses().size() > 1,
                            variableAccessor.variableAccess(variable, xsdInteger, 0));

            case NUMERIC:
            {
                List<ResourceClass> numClasses = resClasses.stream().filter(s -> isNumericClass(s))
                        .collect(Collectors.toList());

                if(numClasses.size() == 0)
                    return null;

                ResourceClass type = xsdInteger;

                for(ResourceClass numClass : numClasses)
                    type = getWiderNumericClass(type, numClass);

                boolean canBeNull = usedVariable.canBeNull() || usedVariable.getClasses().size() > numClasses.size();

                StringBuilder builder = new StringBuilder();
                generateNumericAccess(builder, variable, numClasses);

                return new TranslatedExpression(type, canBeNull, builder.toString());
            }

            case IRI:
            {
                List<ResourceClass> iriClasses = resClasses.stream().filter(s -> s instanceof IriClass)
                        .collect(Collectors.toList());

                boolean canBeNull = usedVariable.canBeNull() || usedVariable.getClasses().size() > iriClasses.size();


                if(iriClasses.size() == 0)
                    return null;

                if(iriClasses.size() == 1)
                {
                    return new TranslatedExpression(IriClass.iriClass, canBeNull,
                            iriClasses.get(0).getSparqlValue(variable.getName()));
                }
                else
                {
                    StringBuilder builder = new StringBuilder();
                    builder.append("COALESCE(");

                    for(int i = 0; i < iriClasses.size(); i++)
                    {
                        appendComma(builder, i > 0);
                        builder.append(iriClasses.get(i).getSparqlValue(variable.getName()));
                    }

                    builder.append(")");
                    return new TranslatedExpression(IriClass.iriClass, canBeNull, builder.toString());
                }
            }

            default:
                assert false;
                return null;
        }
    }


    public static TranslatedExpression createEBV(TranslatedExpression operand)
    {
        if(operand == null)
            return new TranslatedExpression(xsdBoolean, true, "null");

        //TODO: The EBV of any literal whose type is xsd:boolean or numeric is false if the lexical form is not valid
        // for that datatype (e.g. "abc"^^xsd:integer).

        if(operand.getResourceClass() == xsdBoolean)
            return operand;

        if(operand.getResourceClass() == xsdString || isNumericClass(operand.getResourceClass()))
            return new TranslatedExpression(xsdBoolean, operand.canBeNull(), "ebv(" + operand.getCode() + ")");

        return new TranslatedExpression(xsdBoolean, true, "null");
    }


    private void generateNumericAccess(StringBuilder builder, Variable variable, List<ResourceClass> numericClasses)
    {
        if(numericClasses.size() == 1)
        {
            builder.append(variableAccessor.variableAccess(variable, numericClasses.get(0), 0));
        }
        else
        {
            builder.append("COALESCE(");

            for(int i = 0; i < numericClasses.size(); i++)
            {
                appendComma(builder, i > 0);
                builder.append(variableAccessor.variableAccess(variable, numericClasses.get(i), 0));
            }

            builder.append(")");
        }
    }


    private void generateIsNullCheck(StringBuilder builder, Variable variable, List<ResourceClass> resourceClasses)
    {
        builder.append("(");
        boolean useAnd = false;

        for(ResourceClass resourceClass : resourceClasses)
        {
            for(int i = 0; i < resourceClass.getPartsCount(); i++)
            {
                appendAnd(builder, useAnd);
                useAnd = true;

                builder.append(variableAccessor.variableAccess(variable, resourceClass, i));
                builder.append(" IS NULL ");
            }
        }

        builder.append(")");
    }


    private static void generateIsNullCheck(StringBuilder builder, TranslatedExpression expression)
    {
        builder.append("((");
        builder.append(expression.getCode());
        builder.append(") IS NULL)");
    }


    private static IRI getStandaloneIri(Expression expression)
    {
        while(true)
        {
            if(expression instanceof IRI)
                return (IRI) expression;

            if(expression instanceof BracketedExpression)
                expression = ((BracketedExpression) expression).getChild();
            else
                return null;
        }
    }


    private static Variable getStandaloneVariable(Expression expression)
    {
        while(true)
        {
            if(expression instanceof Variable)
                return (Variable) expression;

            if(expression instanceof BracketedExpression)
                expression = ((BracketedExpression) expression).getChild();
            else
                return null;
        }
    }


    private static boolean isLiteralClass(ResourceClass resourceClass)
    {
        return resourceClass instanceof LiteralClass;
    }


    private static boolean isSimpleLiteralClass(ResourceClass resourceClass)
    {
        if(!(resourceClass instanceof LiteralClass))
            return false;

        if(resourceClass.getPartsCount() > 1)
            return false;

        return true;
    }


    private static boolean isNumericClass(ResourceClass resourceClass)
    {
        if(resourceClass == xsdInteger)
            return true;

        if(resourceClass == xsdDecimal)
            return true;

        if(resourceClass == xsdFloat)
            return true;

        if(resourceClass == xsdDouble)
            return true;

        return false;
    }


    private static boolean isEbvClass(ResourceClass resourceClass)
    {
        if(resourceClass == xsdInteger)
            return true;

        if(resourceClass == xsdDecimal)
            return true;

        if(resourceClass == xsdFloat)
            return true;

        if(resourceClass == xsdDouble)
            return true;

        if(resourceClass == xsdBoolean)
            return true;

        if(resourceClass == xsdString)
            return true;

        return false;
    }


    private static ResourceClass getWiderNumericClass(ResourceClass leftResourceClass, ResourceClass rightResourceClass)
    {
        if(leftResourceClass == xsdDouble || rightResourceClass == xsdDouble)
            return xsdDouble;

        if(leftResourceClass == xsdFloat || rightResourceClass == xsdFloat)
            return xsdFloat;

        if(leftResourceClass == xsdDecimal || rightResourceClass == xsdDecimal)
            return xsdDecimal;

        if(leftResourceClass == xsdInteger || rightResourceClass == xsdInteger)
            return xsdInteger;

        assert false;
        return null;
    }


    private static boolean hasLiteralClass(List<ResourceClass> resourceClasses)
    {
        for(ResourceClass resourceClass : resourceClasses)
            if(resourceClass instanceof LiteralClass)
                return true;

        return false;
    }


    private static boolean hasIriClass(List<ResourceClass> resourceClasses)
    {
        for(ResourceClass resourceClass : resourceClasses)
            if(resourceClass instanceof IriClass)
                return true;

        return false;
    }


    private static List<ResourceClass> selectIriClasses(List<ResourceClass> classes)
    {
        return classes.stream().filter(s -> s instanceof IriClass).collect(Collectors.toList());
    }


    private IriClass getIriClass(String iri)
    {
        for(ResourceClass resClass : classes)
            if(resClass instanceof IriClass)
                if(((IriClass) resClass).match(iri))
                    return (IriClass) resClass;

        return IriClass.uncategorizedClass;
    }


    private static boolean areClassesCompatible(List<ResourceClass> classesA, List<ResourceClass> classesB)
    {
        for(ResourceClass resClass : classesB)
            if(areClassesCompatible(classesA, resClass) == false)
                return false;

        return true;
    }


    private static boolean areClassesCompatible(List<ResourceClass> resourceClasses, ResourceClass resourceClass)
    {
        for(ResourceClass resClass : resourceClasses)
            if(areClassesCompatible(resClass, resourceClass) == false)
                return false;

        return true;
    }


    private static boolean areClassesCompatible(ResourceClass resourceClassA, ResourceClass resourceClassB)
    {
        if(resourceClassA == resourceClassB)
            return true;

        if(isNumericClass(resourceClassA) && isNumericClass(resourceClassB))
            return true;

        if(resourceClassA instanceof IriClass && resourceClassB instanceof IriClass)
            return true;

        return false;
    }


    private static TranslateRequest classToRequest(ResourceClass resourceClass)
    {
        if(resourceClass == xsdBoolean)
            return BOOLEAN;

        if(resourceClass == xsdString)
            return STRING;

        if(isNumericClass(resourceClass))
            return NUMERIC;

        if(resourceClass == xsdDateTime)
            return DATETIME;

        if(resourceClass == xsdDate)
            return DATE;

        if(resourceClass == IriClass.iriClass)
            return IRI;

        assert false;
        return null;
    }


    protected static void appendComma(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(", ");
    }


    protected static void appendAnd(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(" AND ");
    }


    protected static void appendOr(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(" OR ");
    }
}

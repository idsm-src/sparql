package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.stream.Collectors.toSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.DateConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringConstantTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.UserIntBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Pair;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public final class SqlBinaryComparison extends SqlBinary
{
    private static final int SECS_PER_DAY = 24 * 60 * 60;

    private final Operator operator;

    private final boolean isAlwaysDifferentIfNotNull;
    private final List<Pair<ResourceClass, ResourceClass>> comparable;
    private final List<Pair<ResourceClass, ResourceClass>> different;


    protected SqlBinaryComparison(Operator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            boolean canBeNull, boolean isAlwaysDifferentIfNotNull, List<Pair<ResourceClass, ResourceClass>> comparable,
            List<Pair<ResourceClass, ResourceClass>> different)
    {
        super(left, right, asSet(xsdBoolean), canBeNull);
        this.operator = operator;
        this.isAlwaysDifferentIfNotNull = isAlwaysDifferentIfNotNull;
        this.comparable = comparable;
        this.different = different;
    }


    public static SqlExpressionIntercode create(Operator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        List<Pair<ResourceClass, ResourceClass>> comparable = new LinkedList<Pair<ResourceClass, ResourceClass>>();
        List<Pair<ResourceClass, ResourceClass>> different = new LinkedList<Pair<ResourceClass, ResourceClass>>();

        boolean isAlwaysNull = true;
        boolean isAlwaysDifferentIfNotNull = !left.canBeNull() && !right.canBeNull();
        boolean canBeNull = left.canBeNull() || right.canBeNull();


        if(left instanceof SqlIri a && right instanceof SqlIri b)
        {
            if(operator == Operator.Equals)
                return a.getIri().equals(b.getIri()) ? trueValue : falseValue;
            else if(operator == Operator.NotEquals)
                return a.getIri().equals(b.getIri()) ? falseValue : trueValue;
        }

        if(left instanceof SqlLiteral a && right instanceof SqlLiteral b)
        {
            if(operator == Operator.Equals)
                return a.getLiteral().equals(b.getLiteral()) ? trueValue : falseValue;
            else if(operator == Operator.NotEquals)
                return a.getLiteral().equals(b.getLiteral()) ? falseValue : trueValue;
        }


        for(ResourceClass leftClass : left.getResourceClasses())
        {
            for(ResourceClass rightClass : right.getResourceClasses())
            {
                if((operator == Operator.Equals || operator == Operator.NotEquals)
                        && leftClass instanceof DateConstantZoneClass l && rightClass instanceof DateConstantZoneClass r
                        && getTimezoneDiff(l, r) % SECS_PER_DAY != 0)
                {
                    // is always different
                    isAlwaysNull = false;
                    different.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass == xsdBoolean && rightClass == xsdBoolean
                        || isString(leftClass) && isString(rightClass) || isNumeric(leftClass) && isNumeric(rightClass)
                        || isDateTime(leftClass) && isDateTime(rightClass) || isDate(leftClass) && isDate(rightClass))
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(operator != Operator.Equals && operator != Operator.NotEquals)
                {
                    // is always null
                    canBeNull = true;
                }
                else if(leftClass == xsdDayTimeDuration && rightClass == xsdDayTimeDuration)
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(isLangString(leftClass) && isLangString(rightClass))
                {
                    if(leftClass == rightClass || leftClass == rdfLangString || rightClass == rdfLangString)
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else if(leftClass instanceof UserLiteralClass && leftClass == rightClass)
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass == unsupportedLiteral && rightClass == unsupportedLiteral)
                {
                    // is null, if types are different
                    canBeNull = true;
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass instanceof LiteralClass && rightClass instanceof LiteralClass)
                {
                    // is always null
                    canBeNull = true;
                }
                else if(leftClass instanceof StrBlankNodeClass && rightClass instanceof StrBlankNodeClass)
                {
                    if(leftClass == rightClass || leftClass == strBlankNode || rightClass == strBlankNode)
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else if(leftClass instanceof IntBlankNodeClass && rightClass instanceof IntBlankNodeClass)
                {
                    if(leftClass == rightClass || leftClass == intBlankNode || rightClass == intBlankNode)
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else if(leftClass instanceof IriClass && rightClass instanceof IriClass)
                {
                    if(leftClass == rightClass || leftClass == iri || rightClass == iri)
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else
                {
                    // is always different
                    isAlwaysNull = false;
                    different.add(new Pair<>(leftClass, rightClass));
                }
            }
        }


        if(isAlwaysNull)
            return SqlNull.get();

        if(operator == Operator.Equals && !canBeNull && isAlwaysDifferentIfNotNull)
            return falseValue;

        if(operator == Operator.NotEquals && !canBeNull && isAlwaysDifferentIfNotNull)
            return trueValue;

        return new SqlBinaryComparison(operator, left, right, canBeNull, isAlwaysDifferentIfNotNull, comparable,
                different);
    }


    @Override
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        Set<ResourceClass> leftSet = new HashSet<ResourceClass>();
        Set<ResourceClass> rightSet = new HashSet<ResourceClass>();

        for(Pair<ResourceClass, ResourceClass> p : comparable)
        {
            leftSet.add(p.getKey());
            rightSet.add(p.getValue());
        }

        for(Pair<ResourceClass, ResourceClass> p : different)
        {
            leftSet.add(p.getKey());
            rightSet.add(p.getValue());
        }

        return new Restrictions(left.getRequirements(leftSet), right.getRequirements(rightSet));
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        SqlExpressionIntercode optLeft = left.optimize(request, variables, evalServices);
        SqlExpressionIntercode optRight = right.optimize(request, variables, evalServices);

        if(optLeft == left && optRight == right)
            return this;

        return create(operator, optLeft, optRight);
    }


    @Override
    public String translate(Request request)
    {
        if(left instanceof SqlNodeValue && right instanceof SqlNodeValue)
            return translateAsNodeComparison(request);


        StringBuilder builder = new StringBuilder();

        if(different.size() == left.getResourceClasses().size() * right.getResourceClasses().size())
        {
            assert comparable.size() == 0;
            assert operator == Operator.Equals || operator == Operator.NotEquals;

            builder.append("NULLIF(");

            if(left.canBeNull())
                builder.append(translateAsNullCheck(request, left, operator == Operator.NotEquals));

            if(left.canBeNull() && right.canBeNull())
                builder.append(operator == Operator.Equals ? " OR " : " AND ");

            if(right.canBeNull())
                builder.append(translateAsNullCheck(request, right, operator == Operator.NotEquals));

            builder.append(operator == Operator.Equals ? ", true)" : ", false)");
        }
        //TODO: také podporovat, když je to integer vs integer+decimal
        else if(different.size() == 0 && SqlExpressionIntercode.getExpressionResourceClass(
                comparable.stream().map(r -> determineComparisonClass(r.getKey(), r.getValue()))
                        .collect(toSet())) != null/*!(left.isBoxed() || right.isBoxed())*/)
        {
            Set<ResourceClass> cmpClasses = comparable.stream()
                    .map(r -> determineComparisonClass(r.getKey(), r.getValue())).collect(toSet());

            ResourceClass cmpClass = SqlExpressionIntercode.getExpressionResourceClass(cmpClasses);

            if(cmpClass instanceof UserLiteralClass userClass)
            {
                //TODO: add special treatment

                builder.append("(");
                builder.append(translateAsUnboxedOperand(request, left, cmpClass));
                builder.append(" ");
                builder.append(userClass.getOperatorCode(operator));
                builder.append(" ");
                builder.append(translateAsUnboxedOperand(request, right, cmpClass));
                builder.append(")");
            }
            else if(cmpClass == xsdDateTime || cmpClass == xsdDate || isFloatPoint(cmpClass))
            {
                //TODO: add special cases to compare xsdDate as simple date

                builder.append("(");
                builder.append(translateAsUnboxedOperand(request, left, cmpClass));
                builder.append(" operator(sparql.");
                builder.append(operator.getText());
                builder.append(") ");
                builder.append(translateAsUnboxedOperand(request, right, cmpClass));
                builder.append(")");
            }
            else
            {
                builder.append("(");
                builder.append(translateAsUnboxedOperand(request, left, cmpClass));
                builder.append(" ");
                builder.append(operator.getText());
                builder.append(" ");
                builder.append(translateAsUnboxedOperand(request, right, cmpClass));
                builder.append(")");
            }
        }
        else
        {
            Set<ResourceClass> leftSet = new HashSet<ResourceClass>();
            leftSet.addAll(comparable.stream().map(r -> r.getKey()).collect(toSet()));
            leftSet.addAll(different.stream().map(r -> r.getKey()).collect(toSet()));

            Set<ResourceClass> rightSet = new HashSet<ResourceClass>();
            rightSet.addAll(comparable.stream().map(r -> r.getValue()).collect(toSet()));
            rightSet.addAll(different.stream().map(r -> r.getValue()).collect(toSet()));

            builder.append("(");
            builder.append(translateAsBoxedOperand(request, left, leftSet));
            builder.append(" operator(sparql.");
            builder.append(operator.getText());
            builder.append(") ");
            builder.append(translateAsBoxedOperand(request, right, rightSet));
            builder.append(")");
        }

        return builder.toString();
    }


    public String translateAsNodeComparison(Request request)
    {
        SqlNodeValue leftNode = (SqlNodeValue) left;
        SqlNodeValue rightNode = (SqlNodeValue) right;

        StringBuilder builder = new StringBuilder();
        boolean hasAlternative = false;

        if(comparable.size() + different.size() > 1)
            builder.append("coalesce(");


        for(Pair<ResourceClass, ResourceClass> pair : comparable)
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            ResourceClass leftClass = pair.getKey();
            ResourceClass rightClass = pair.getValue();

            List<Column> leftCols = leftNode.asResource(request, leftClass);
            List<Column> rightCols = rightNode.asResource(request, rightClass);


            if(leftClass instanceof BlankNodeClass)
            {
                if(leftClass instanceof UserIntBlankNodeClass && rightClass instanceof UserIntBlankNodeClass)
                {
                    builder.append(leftCols.get(0) + " " + operator.getText() + " " + rightCols.get(0));
                }
                else if(leftClass instanceof UserStrBlankNodeClass && rightClass instanceof UserStrBlankNodeClass)
                {
                    builder.append(leftCols.get(0) + " " + operator.getText() + " " + rightCols.get(0));
                }
                else
                {
                    List<Column> lcols = leftNode.asResource(request, getExpressionBaseClass(leftClass));
                    List<Column> rcols = rightNode.asResource(request, getExpressionBaseClass(rightClass));

                    builder.append("(");
                    builder.append(lcols.get(0) + " " + operator.getText() + " " + rcols.get(0));
                    builder.append(operator == Operator.Equals ? " AND " : " OR ");
                    builder.append(lcols.get(1) + " " + operator.getText() + " " + rcols.get(1));
                    builder.append(")");
                }
            }
            else if(leftClass == xsdBoolean || isString(leftClass) || leftClass == xsdDayTimeDuration
                    || isNumeric(leftClass))
            {
                ResourceClass leftGenClass = getExpressionBaseClass(leftClass);
                ResourceClass rightGenClass = getExpressionBaseClass(rightClass);

                List<Column> leftColumns = leftNode.asResource(request, leftGenClass);
                String left = leftGenClass.toExpression(leftColumns).toString();

                List<Column> rightColumns = rightNode.asResource(request, rightGenClass);
                String right = rightGenClass.toExpression(rightColumns).toString();

                ResourceClass cmpClass = determineComparisonClass(leftGenClass, rightGenClass);

                if(leftGenClass != cmpClass)
                    left = "sparql.cast_as_" + cmpClass.getName() + "_from_" + leftGenClass.getName() + "(" + left
                            + ")";

                if(rightGenClass != cmpClass)
                    right = "sparql.cast_as_" + cmpClass.getName() + "_from_" + rightGenClass.getName() + "(" + right
                            + ")";

                if(!isFloat(cmpClass) && !isDouble(cmpClass))
                {
                    builder.append("(" + left + " " + operator.getText() + " " + right + ")");
                }
                else
                {
                    builder.append("(");

                    if(leftClass == xsdFloat || leftClass == xsdDouble)
                        builder.append(leftCols.get(0) + " != 'NaN'::" + leftClass.getSqlTypes().get(0) + " AND ");

                    if(rightClass == xsdFloat || rightClass == xsdDouble)
                        builder.append(rightCols.get(0) + " != 'NaN'::" + rightClass.getSqlTypes().get(0) + " AND ");

                    builder.append(left + " " + operator.getText() + " " + right + ")");
                }
            }
            else if(isDateTime(leftClass))
            {
                String left = leftCols.get(0).toString();
                String right = rightCols.get(0).toString();

                //NOTE: it is assumed that default timezone is UTC
                builder.append("(" + left + " " + operator.getText() + " " + right + ")");
            }
            else if(isDate(leftClass))
            {
                if(leftClass instanceof DateConstantZoneClass l && rightClass instanceof DateConstantZoneClass r
                        && getTimezoneDiff(l, r) < SECS_PER_DAY)
                {
                    Operator effectiveOperator = operator;

                    if(operator == Operator.LessThanOrEqual)
                        effectiveOperator = Operator.LessThan;
                    else if(operator == Operator.GreaterThanOrEqual)
                        effectiveOperator = Operator.GreaterThan;

                    String left = leftCols.get(0).toString();
                    String right = rightCols.get(0).toString();

                    builder.append("(" + left + " " + effectiveOperator.getText() + " " + right + ")");
                }
                else
                {
                    List<Column> lcols = leftNode.asResource(request, xsdDate);
                    List<Column> rcols = rightNode.asResource(request, xsdDate);

                    String left = null;
                    String right = null;

                    //NOTE: it is assumed that default timezone is UTC

                    if(leftClass instanceof DateConstantZoneClass constantZoneClass
                            && (constantZoneClass.getZone() == 0 || constantZoneClass.getZone() == Integer.MIN_VALUE))
                        left = lcols.get(0) + "::timestamp";
                    else if(leftClass instanceof DateConstantZoneClass constantZoneClass)
                        left = "(" + lcols.get(0) + " + make_interval(secs => " + constantZoneClass.getZone() + "))";
                    else
                        left = "(" + lcols.get(0) + " + make_interval(secs => CASE " + lcols.get(1)
                                + " WHEN -2147483648 THEN 0 ELSE " + lcols.get(1) + " END))";

                    //NOTE: it is assumed that default timezone is UTC

                    if(rightClass instanceof DateConstantZoneClass constantZoneClass
                            && (constantZoneClass.getZone() == 0 || constantZoneClass.getZone() == Integer.MIN_VALUE))
                        right = rcols.get(0) + "::timestamp";
                    else if(rightClass instanceof DateConstantZoneClass constantZoneClass)
                        right = "(" + rcols.get(0) + " + make_interval(secs => " + constantZoneClass.getZone() + "))";
                    else
                        right = "(" + rcols.get(0) + " + make_interval(secs => CASE " + rcols.get(1)
                                + " WHEN -2147483648 THEN 0 ELSE " + rcols.get(1) + " END))";

                    builder.append("(" + left + " " + operator.getText() + " " + right + ")");
                }
            }
            else if(isLangString(leftClass))
            {
                if(leftClass instanceof LangStringConstantTagClass && rightClass instanceof LangStringConstantTagClass)
                {
                    builder.append(leftCols.get(0) + " " + operator.getText() + " " + rightCols.get(0));
                }
                else
                {
                    List<Column> lcols = leftNode.asResource(request, getExpressionBaseClass(leftClass));
                    List<Column> rcols = rightNode.asResource(request, getExpressionBaseClass(rightClass));

                    builder.append("(");
                    builder.append(lcols.get(0) + " " + operator.getText() + " " + rcols.get(0));
                    builder.append(operator == Operator.Equals ? " AND " : " OR ");
                    builder.append(lcols.get(1) + " " + operator.getText() + " " + rcols.get(1));
                    builder.append(")");
                }
            }
            else if(leftClass instanceof IriClass)
            {
                if(leftClass == rightClass)
                {
                    builder.append("(");

                    for(int i = 0; i < leftClass.getColumnCount(); i++)
                    {
                        if(operator == Operator.Equals)
                        {
                            appendAnd(builder, i > 0);
                            builder.append(leftCols.get(i));
                            builder.append(" = ");
                            builder.append(rightCols.get(i));
                        }
                        else if(operator == Operator.NotEquals)
                        {
                            appendOr(builder, i > 0);
                            builder.append(leftCols.get(i));
                            builder.append(" != ");
                            builder.append(rightCols.get(i));
                        }
                        else
                        {
                            assert false;
                        }
                    }

                    builder.append(")");
                }
                else
                {
                    Column left = leftClass.toExpression(leftCols);
                    Column right = rightClass.toExpression(rightCols);

                    builder.append("(" + left + ", " + right + ")");
                }
            }
            else if(leftClass instanceof UserLiteralClass userClass)
            {
                //TODO: add special treatment

                builder.append("(");
                builder.append(leftCols.get(0));
                builder.append(" ");
                builder.append(userClass.getOperatorCode(operator));
                builder.append(" ");
                builder.append(rightCols.get(0));
                builder.append(")");
            }
            else if(leftClass == unsupportedLiteral)
            {
                builder.append("nullif(");
                builder.append(leftCols.get(0));
                builder.append(" ");
                builder.append(operator.getText());
                builder.append(" ");
                builder.append(rightCols.get(0));
                builder.append(operator == Operator.Equals ? " AND " : " OR ");
                builder.append(leftCols.get(1));
                builder.append(" ");
                builder.append(operator.getText());
                builder.append(" ");
                builder.append(rightCols.get(1));
                builder.append(operator == Operator.Equals ? ", false)" : ", true)");
            }
            else
            {
                assert false;
            }
        }


        for(Pair<ResourceClass, ResourceClass> pair : different)
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            ResourceClass leftClass = pair.getKey();
            ResourceClass rightClass = pair.getValue();

            List<Column> leftCols = leftNode.asResource(request, leftClass);
            List<Column> rightCols = rightNode.asResource(request, rightClass);

            assert operator == Operator.Equals || operator == Operator.NotEquals;

            boolean hasVariants = false;

            builder.append("NULLIF(");

            if(left.canBeNull() || leftNode.getResourceClasses().size() > 1)
            {
                for(int i = 0; i < leftClass.getColumnCount(); i++)
                {
                    if(operator == Operator.Equals)
                        appendOr(builder, hasVariants);
                    else
                        appendAnd(builder, hasVariants);

                    hasVariants = true;
                    builder.append(leftCols.get(i));
                    builder.append(operator == Operator.Equals ? " IS NULL" : " IS NOT NULL");
                }
            }

            if(right.canBeNull() || rightNode.getResourceClasses().size() > 1)
            {
                for(int i = 0; i < rightClass.getColumnCount(); i++)
                {
                    if(operator == Operator.Equals)
                        appendOr(builder, hasVariants);
                    else
                        appendAnd(builder, hasVariants);

                    hasVariants = true;
                    builder.append(rightCols.get(i));
                    builder.append(operator == Operator.Equals ? " IS NULL" : " IS NOT NULL");
                }
            }

            builder.append(operator == Operator.Equals ? ", true)" : ", false)");
        }


        if(comparable.size() + different.size() > 1)
            builder.append(")");

        return builder.toString();
    }


    private static ResourceClass determineComparisonClass(ResourceClass leftClass, ResourceClass rightClass)
    {
        if(leftClass == rightClass)
            return leftClass;

        if(isString(leftClass) && isString(rightClass))
            return xsdString;

        if(isDateTime(leftClass) && isDateTime(rightClass))
            return xsdDateTime;

        if(isDate(leftClass) && isDate(rightClass))
            return xsdDate;

        if(isLangString(leftClass) || isLangString(rightClass))
            return rdfLangString;

        if(leftClass instanceof IriClass && rightClass instanceof IriClass)
            return iri;

        if(leftClass instanceof IntBlankNodeClass && rightClass instanceof IntBlankNodeClass)
            return intBlankNode;

        if(leftClass instanceof StrBlankNodeClass && rightClass instanceof StrBlankNodeClass)
            return strBlankNode;

        /*
        if(leftClass == rightClass)
            return leftClass;
        */

        if(isNumeric(leftClass) && isNumeric(rightClass))
        {
            if(isDouble(leftClass) || isDouble(rightClass))
                return xsdDouble;
            else if(isFloat(leftClass) || isFloat(rightClass))
                return xsdFloat;
            else if(isDecimal(leftClass) || isDecimal(rightClass))
                return xsdDecimal;
            else if(isInteger(leftClass) || isInteger(rightClass))
                return xsdInteger;
            else if(isLong(leftClass) || isLong(rightClass))
                return xsdLong;
            else if(isInt(leftClass) || isInt(rightClass))
                return xsdInt;
            else if(isShort(leftClass) || isShort(rightClass))
                return xsdShort;
        }

        throw new IllegalArgumentException();
    }


    private static int getTimezoneDiff(DateConstantZoneClass left, DateConstantZoneClass right)
    {
        int leftZone = left.getZone();
        int rightZone = right.getZone();

        //NOTE: it is assumed that default timezone is UTC
        if(leftZone == Integer.MIN_VALUE)
            leftZone = 0;

        if(rightZone == Integer.MIN_VALUE)
            rightZone = 0;

        return Math.abs(leftZone - rightZone);
    }


    public boolean isAlwaysFalseOrNull()
    {
        return operator == Operator.Equals && isAlwaysDifferentIfNotNull;
    }


    public boolean isAlwaysTrueOrNull()
    {
        return operator == Operator.NotEquals && isAlwaysDifferentIfNotNull;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        int myPriortity = 6;

        if(myPriortity > priority)
            builder.append("(");

        left.generateExplanation(builder, indent, myPriortity);
        builder.append(" ");
        builder.append(operator.getText());
        builder.append(" ");
        right.generateExplanation(builder, indent, myPriortity);

        if(myPriortity > priority)
            builder.append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlBinaryComparison imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(operator, imcode.operator))
            return false;

        if(!left.equals(imcode.left) || !right.equals(imcode.right))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(operator, left, right);
    }
}

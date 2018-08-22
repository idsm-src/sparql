package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangStringExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteralExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTimeExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue.falseValue;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue.trueValue;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.BlankNodeBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.chemweb.server.sparql.translator.Pair;



public class SqlCommonBinaryComparison extends SqlBinaryComparison
{
    private final List<Pair<ExpressionResourceClass, ExpressionResourceClass>> comparable;
    private final List<Pair<ExpressionResourceClass, ExpressionResourceClass>> different;


    public SqlCommonBinaryComparison(Operator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            boolean canBeNull, boolean isAlwaysDifferentIfNotNull,
            List<Pair<ExpressionResourceClass, ExpressionResourceClass>> comparable,
            List<Pair<ExpressionResourceClass, ExpressionResourceClass>> different)
    {
        super(operator, left, right, canBeNull, isAlwaysDifferentIfNotNull);
        this.comparable = comparable;
        this.different = different;
    }


    public static SqlExpressionIntercode create(Operator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        List<Pair<ExpressionResourceClass, ExpressionResourceClass>> comparable = new LinkedList<Pair<ExpressionResourceClass, ExpressionResourceClass>>();
        List<Pair<ExpressionResourceClass, ExpressionResourceClass>> different = new LinkedList<Pair<ExpressionResourceClass, ExpressionResourceClass>>();

        boolean isAlwaysNull = true;
        boolean isAlwaysDifferentIfNotNull = !left.canBeNull() && !right.canBeNull();
        boolean canBeNull = left.canBeNull() || right.canBeNull();

        for(ExpressionResourceClass leftClass : left.getResourceClasses())
        {
            for(ExpressionResourceClass rightClass : right.getResourceClasses())
            {
                if(leftClass == rightClass && (leftClass == xsdBoolean || leftClass == xsdString
                        || leftClass == xsdDateTimeExpr || leftClass == xsdDateExpr))
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(isNumeric(leftClass) && isNumeric(rightClass))
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
                else if(leftClass instanceof BlankNodeBaseClass && rightClass instanceof BlankNodeBaseClass)
                {
                    if(leftClass == rightClass)
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
                else if(leftClass == rightClass
                        && (leftClass == iri || leftClass == xsdDayTimeDuration || leftClass == rdfLangStringExpr))
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass == unsupportedLiteralExpr && rightClass == unsupportedLiteralExpr)
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

        return new SqlCommonBinaryComparison(operator, left, right, canBeNull, isAlwaysDifferentIfNotNull, comparable,
                different);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        if(comparable.size() == 0
                && different.size() == getLeft().getResourceClasses().size() * getRight().getResourceClasses().size())
        {
            assert operator == Operator.Equals || operator == Operator.NotEquals;

            builder.append("NULLIF(");

            if(getLeft().canBeNull())
                buildNullCheck(builder, getLeft(), operator == Operator.NotEquals);

            if(getLeft().canBeNull() && getRight().canBeNull())
                builder.append(operator == Operator.Equals ? " OR " : " AND ");

            if(getRight().canBeNull())
                buildNullCheck(builder, getRight(), operator == Operator.NotEquals);

            builder.append(operator == Operator.Equals ? ", true)" : ", false)");

            return builder.toString();
        }
        else if(different.size() == 0)
        {
            Set<ExpressionResourceClass> cmpClasses = comparable.stream()
                    .map(r -> determineComparisonClass(r.getKey(), r.getValue())).collect(Collectors.toSet());

            if(cmpClasses.size() == 1)
            {
                ExpressionResourceClass cmpClass = cmpClasses.iterator().next();

                if(cmpClass != rdfLangStringExpr && cmpClass != unsupportedLiteralExpr)
                {
                    builder.append("sparql.");
                    builder.append(operator.getName());
                    builder.append("_");
                    builder.append(cmpClass.getName());
                    builder.append("(");
                    buildUnboxedOperand(builder, getLeft(), cmpClass);
                    builder.append(", ");
                    buildUnboxedOperand(builder, getRight(), cmpClass);
                    builder.append(")");

                    return builder.toString();
                }
            }
        }


        Set<ExpressionResourceClass> leftSet = new HashSet<ExpressionResourceClass>();
        leftSet.addAll(comparable.stream().map(r -> r.getKey()).collect(Collectors.toSet()));
        leftSet.addAll(different.stream().map(r -> r.getKey()).collect(Collectors.toSet()));

        Set<ExpressionResourceClass> rightSet = new HashSet<ExpressionResourceClass>();
        rightSet.addAll(comparable.stream().map(r -> r.getValue()).collect(Collectors.toSet()));
        rightSet.addAll(different.stream().map(r -> r.getValue()).collect(Collectors.toSet()));

        builder.append("sparql.");
        builder.append(operator.getName());
        builder.append("_rdfbox");
        builder.append("(");
        buildBoxedOperand(builder, getLeft(), leftSet);
        builder.append(", ");
        buildBoxedOperand(builder, getRight(), rightSet);
        builder.append(")");

        return builder.toString();
    }


    private void buildNullCheck(StringBuilder builder, SqlExpressionIntercode operand, boolean not)
    {
        if(operand instanceof SqlVariable)
        {
            SqlVariable variable = (SqlVariable) operand;
            String name = variable.getName();

            boolean hasOuterVariants = false;

            if(variable.getPatternResourceClasses().size() > 1)
                builder.append("(");

            for(PatternResourceClass resourceClass : variable.getPatternResourceClasses())
            {
                if(not)
                    appendOr(builder, hasOuterVariants);
                else
                    appendAnd(builder, hasOuterVariants);

                hasOuterVariants = true;
                boolean hasInnerVariants = false;

                if(resourceClass.getPartsCount() > 1)
                    builder.append("(");

                for(int i = 0; i < resourceClass.getPartsCount(); i++)
                {
                    String access = variable.getVariableAccessor().variableAccess(name, resourceClass, i);

                    if(not)
                        appendAnd(builder, hasInnerVariants);
                    else
                        appendOr(builder, hasInnerVariants);

                    hasInnerVariants = true;
                    builder.append(access);
                    builder.append(not ? " IS NOT NULL" : " IS NULL");
                }

                if(resourceClass.getPartsCount() > 1)
                    builder.append(")");
            }

            if(variable.getPatternResourceClasses().size() > 1)
                builder.append(")");
        }
        else
        {
            builder.append(operand.translate());
            builder.append(not ? " IS NOT NULL" : " IS NULL");
        }
    }


    private void buildUnboxedOperand(StringBuilder builder, SqlExpressionIntercode operand,
            ExpressionResourceClass resourceClass)
    {
        if(operand instanceof SqlVariable)
        {
            SqlVariable variable = (SqlVariable) operand;
            String name = variable.getName();

            List<PatternResourceClass> compatibleClasses = new LinkedList<PatternResourceClass>();

            for(PatternResourceClass patternClass : variable.getPatternResourceClasses())
            {
                ExpressionResourceClass expressionClass = patternClass.getExpressionResourceClass();

                if(expressionClass == resourceClass || isNumeric(expressionClass) && isNumeric(resourceClass))
                    compatibleClasses.add(patternClass);
            }


            boolean hasAlternative = false;

            if(compatibleClasses.size() > 1)
                builder.append("COALESCE(");

            for(PatternResourceClass patternClass : compatibleClasses)
            {
                String code = patternClass.getSqlExpressionValue(name, variable.getVariableAccessor(), false);

                appendComma(builder, hasAlternative);
                hasAlternative = true;

                if(patternClass.getExpressionResourceClass() != resourceClass)
                {
                    builder.append("sparql.cast_as_");
                    builder.append(resourceClass.getName());
                    builder.append("_from_");
                    builder.append(patternClass.getName());
                    builder.append("(");
                    builder.append(code);
                    builder.append(")");
                }
                else
                {
                    builder.append(code);
                }
            }

            if(compatibleClasses.size() > 1)
                builder.append(")");
        }
        else if(operand.getResourceClasses().size() == 1 && operand.getResourceClasses().contains(resourceClass))
        {
            builder.append(operand.translate());
        }
        else
        {
            builder.append("sparql.cast_as_");
            builder.append(resourceClass.getName());
            builder.append("_from_");
            builder.append(operand.getResourceName());
            builder.append("(");
            builder.append(operand.translate());
            builder.append(")");
        }
    }


    private void buildBoxedOperand(StringBuilder builder, SqlExpressionIntercode operand,
            Set<ExpressionResourceClass> resourceClasses)
    {
        if(operand instanceof SqlVariable)
        {
            SqlVariable variable = (SqlVariable) operand;
            String name = variable.getName();

            List<PatternResourceClass> compatibleClasses = variable.getPatternResourceClasses().stream()
                    .filter(r -> resourceClasses.contains(r.getExpressionResourceClass())).collect(Collectors.toList());

            boolean hasAlternative = false;

            if(compatibleClasses.size() > 1)
                builder.append("COALESCE(");

            for(PatternResourceClass patternClass : compatibleClasses)
            {
                appendComma(builder, hasAlternative);
                hasAlternative = true;

                builder.append(patternClass.getSqlExpressionValue(name, variable.getVariableAccessor(), true));
            }

            if(compatibleClasses.size() > 1)
                builder.append(")");
        }
        else if(operand.isBoxed())
        {
            builder.append(operand.translate());
        }
        else
        {
            builder.append("sparql.cast_as_rdfbox");
            builder.append("_from_");
            builder.append(operand.getResourceName());
            builder.append("(");
            builder.append(operand.translate());
            builder.append(")");
        }
    }


    private static ExpressionResourceClass determineComparisonClass(ExpressionResourceClass leftClass,
            ExpressionResourceClass rightClass)
    {
        if(leftClass == rightClass)
            return leftClass;

        if(isNumeric(leftClass) && isNumeric(rightClass))
        {
            if(leftClass == xsdDouble || rightClass == xsdDouble)
                return xsdDouble;
            else if(leftClass == xsdFloat || rightClass == xsdFloat)
                return xsdFloat;
            else if(leftClass == xsdDecimal || rightClass == xsdDecimal)
                return xsdDecimal;
            else if(leftClass == xsdInteger || rightClass == xsdInteger)
                return xsdInteger;
            else if(leftClass == xsdLong || rightClass == xsdLong)
                return xsdLong;
            else if(leftClass == xsdInt || rightClass == xsdInt)
                return xsdInt;
            else if(leftClass == xsdShort || rightClass == xsdShort)
                return xsdShort;
        }

        throw new IllegalArgumentException();
    }
}

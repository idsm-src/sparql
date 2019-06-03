package cz.iocb.chemweb.server.sparql.translator.imcode;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode.isDate;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode.isDateTime;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode.isIri;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode.isNumeric;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode.isNumericCompatibleWith;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.db.schema.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.mapping.classes.BlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition.Direction;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlSelect extends SqlIntercode
{
    private final SqlIntercode child;
    private final LinkedHashMap<String, Direction> orderByVariables;
    private final HashSet<String> distinctVariables;
    private BigInteger limit = null;
    private BigInteger offset = null;


    public SqlSelect(UsedVariables variables, SqlIntercode child, HashSet<String> distinctVariables,
            LinkedHashMap<String, Direction> orderByVariables)
    {
        super(variables, child.isDeterministic());
        this.distinctVariables = distinctVariables;
        this.orderByVariables = orderByVariables;
        this.child = child;
    }


    public final void setLimit(BigInteger limit)
    {
        this.limit = limit;
    }


    public final void setOffset(BigInteger offset)
    {
        this.offset = offset;
    }


    @Override
    public SqlIntercode optimize(DatabaseSchema schema, HashSet<String> restrictions, boolean reduced)
    {
        restrictions = new HashSet<String>(restrictions);
        restrictions.retainAll(variables.getNames());

        if(orderByVariables.isEmpty() && distinctVariables.isEmpty() && limit == null
                && (offset == null || offset.equals(BigInteger.ZERO)))
            return child.optimize(schema, restrictions, reduced);


        HashSet<String> childRestrictions = new HashSet<String>(restrictions);
        childRestrictions.addAll(orderByVariables.keySet());
        childRestrictions.addAll(distinctVariables);

        SqlIntercode optimized = child.optimize(schema, childRestrictions, !distinctVariables.isEmpty() || reduced);

        SqlSelect result = new SqlSelect(optimized.getVariables().restrict(restrictions), optimized, distinctVariables,
                orderByVariables);

        result.setLimit(limit);
        result.setOffset(offset);

        return result;
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        boolean useRow = orderByVariables.keySet().stream().anyMatch(v -> !distinctVariables.contains(v));

        if(!distinctVariables.isEmpty() && !orderByVariables.isEmpty() && useRow)
        {
            builder.append("SELECT ");
            builder.append(translateSelectVariables(variables));
            builder.append(" FROM (");
        }


        builder.append("SELECT ");

        builder.append(translateSelectVariables(variables));

        if(!distinctVariables.isEmpty() && !orderByVariables.isEmpty() && useRow)
        {
            builder.append(", row_number() OVER (");
            builder.append(translateOrderBy());
            builder.append(") AS \"#rn\"");
        }

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        if(distinctVariables.isEmpty() && !orderByVariables.isEmpty())
            builder.append(translateOrderBy());


        if(!distinctVariables.isEmpty())
        {
            if(!orderByVariables.isEmpty() && useRow)
                builder.append(") AS tab");

            builder.append(" GROUP BY ");
            builder.append(translateSelectVariables(child.getVariables().restrict(distinctVariables)));

            if(!orderByVariables.isEmpty() && !useRow)
                builder.append(translateOrderBy());

            if(!orderByVariables.isEmpty() && useRow)
                builder.append(" ORDER BY min(\"#rn\")");
        }


        if(limit != null)
            builder.append(" LIMIT ").append(limit.toString());

        if(offset != null)
            builder.append(" OFFSET ").append(offset.toString());

        return builder.toString();
    }


    private String translateSelectVariables(UsedVariables variables)
    {
        StringBuilder builder = new StringBuilder();

        boolean hasSelect = false;

        for(UsedVariable variable : variables.getValues())
        {
            for(ResourceClass resClass : variable.getClasses())
            {
                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(resClass.getSqlColumn(variable.getName(), i));
                }
            }
        }

        if(!hasSelect)
            builder.append("1");

        return builder.toString();
    }


    private String translateOrderBy()
    {
        StringBuilder builder = new StringBuilder();

        builder.append(" ORDER BY ");
        boolean hasOrderCondition = false;

        for(Entry<String, Direction> order : orderByVariables.entrySet())
        {
            String varName = order.getKey();
            UsedVariable variable = child.getVariables().get(varName);
            Set<ResourceClass> classes = variable.getClasses();


            // order unbounded
            if(variable.canBeNull())
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                boolean hasOuterVariants = false;

                if(classes.size() > 1)
                    builder.append("(");

                for(ResourceClass resourceClass : classes)
                {
                    appendOr(builder, hasOuterVariants);

                    hasOuterVariants = true;
                    boolean hasInnerVariants = false;

                    if(resourceClass.getPatternPartsCount() > 1)
                        builder.append("(");

                    for(int i = 0; i < resourceClass.getPatternPartsCount(); i++)
                    {
                        appendAnd(builder, hasInnerVariants);
                        hasInnerVariants = true;

                        builder.append(resourceClass.getSqlColumn(varName, i));
                        builder.append(" IS NOT NULL");
                    }

                    if(resourceClass.getPatternPartsCount() > 1)
                        builder.append(")");
                }

                if(classes.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order blank nodes
            for(ResourceClass resClass : variable.getClasses())
            {
                if(resClass instanceof BlankNodeClass)
                {
                    appendComma(builder, hasOrderCondition);
                    hasOrderCondition = true;

                    builder.append(resClass.getSqlColumn(varName, 0));
                    builder.append(" IS NULL");

                    if(order.getValue() == Direction.Descending)
                        builder.append(" DESC");
                }
            }


            // order IRIs
            Set<ResourceClass> iris = classes.stream().filter(r -> isIri(r)).collect(Collectors.toSet());

            if(iris.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(iris.size() > 1)
                    builder.append("COALESCE(");

                boolean hasVariants = false;

                for(ResourceClass res : iris)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    //TODO: use better approach
                    builder.append(res.getGeneralisedPatternCode(null, varName, 0, false));
                }

                if(iris.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order numerics
            Set<ResourceClass> numerics = classes.stream().filter(r -> isNumeric(r)).collect(Collectors.toSet());

            if(numerics.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                Set<ResourceClass> decimals = classes.stream().filter(r -> isNumericCompatibleWith(r, xsdDecimal))
                        .collect(Collectors.toSet());

                if(numerics.size() > 1)
                    builder.append("COALESCE(");

                boolean hasVariants = false;

                for(ResourceClass numeric : numerics)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    if(decimals.size() > 0 && decimals.size() != numerics.size())
                        builder.append("sparql.cast_as_rdfbox_from_").append(numeric.getName()).append("(");

                    builder.append(numeric.getSqlColumn(varName, 0));

                    if(decimals.size() > 0 && decimals.size() != numerics.size())
                        builder.append(")");
                }

                if(numerics.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:booleans
            if(variable.containsClass(xsdBoolean))
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(xsdBoolean.getSqlColumn(varName, 0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:strings
            if(variable.containsClass(xsdString))
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(xsdString.getSqlColumn(varName, 0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:dateTimes
            Set<ResourceClass> dateTimes = classes.stream().filter(r -> isDateTime(r)).collect(Collectors.toSet());

            if(dateTimes.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(dateTimes.size() > 1)
                    builder.append("COALESCE(");

                boolean hasVariants = false;

                for(ResourceClass dateTime : dateTimes)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(dateTime.getSqlColumn(varName, 0));
                }

                if(dateTimes.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:dates
            Set<ResourceClass> dates = classes.stream().filter(r -> isDate(r)).collect(Collectors.toSet());

            if(dates.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(dates.size() > 1)
                    builder.append("COALESCE(");

                boolean hasVariants = false;

                for(ResourceClass date : dates)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(date.getSqlColumn(varName, 0));
                }

                if(dates.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }
        }

        return builder.toString();
    }
}

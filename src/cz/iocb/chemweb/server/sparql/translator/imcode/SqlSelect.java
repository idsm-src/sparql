package cz.iocb.chemweb.server.sparql.translator.imcode;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode.isDate;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode.isDateTime;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode.isIri;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode.isNumeric;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode.isNumericCompatibleWith;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition.Direction;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlSelect extends SqlIntercode
{
    private final SqlIntercode child;
    private final Collection<String> selectedVariables;
    private final LinkedHashMap<String, Direction> orderByVariables;
    private boolean distinct = false;
    private BigInteger limit = null;
    private BigInteger offset = null;


    public SqlSelect(Collection<String> selectedVariables, UsedVariables variables, SqlIntercode child,
            LinkedHashMap<String, Direction> orderByVariables)
    {
        super(variables);
        this.selectedVariables = selectedVariables;
        this.orderByVariables = orderByVariables;
        this.child = child;
    }


    public final void setDistinct(boolean distinct)
    {
        this.distinct = distinct;
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
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        if(distinct)
            builder.append("SELECT DISTINCT * FROM (");


        builder.append("SELECT ");

        boolean hasSelect = false;

        for(String variableName : selectedVariables)
        {
            UsedVariable variable = variables.get(variableName);

            if(variable == null)
                continue;

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

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");


        if(!orderByVariables.isEmpty())
        {
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
                if(variable.containsClass(intBlankNode))
                {
                    appendComma(builder, hasOrderCondition);
                    hasOrderCondition = true;

                    builder.append(intBlankNode.getSqlColumn(varName, 0));
                    builder.append(" IS NULL");

                    if(order.getValue() == Direction.Descending)
                        builder.append(" DESC");
                }

                if(variable.containsClass(strBlankNode))
                {
                    appendComma(builder, hasOrderCondition);
                    hasOrderCondition = true;

                    builder.append(strBlankNode.getSqlColumn(varName, 0));
                    builder.append(" IS NULL");

                    if(order.getValue() == Direction.Descending)
                        builder.append(" DESC");
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
        }


        if(distinct)
            builder.append(") AS tab");


        if(limit != null)
        {
            builder.append(" LIMIT ");
            builder.append(limit.toString());
        }


        if(offset != null)
        {
            builder.append(" OFFSET ");
            builder.append(offset.toString());
        }

        return builder.toString();
    }
}

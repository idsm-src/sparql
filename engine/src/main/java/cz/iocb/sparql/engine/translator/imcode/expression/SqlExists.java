package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.translator.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.translator.imcode.SqlUnion;



public final class SqlExists extends SqlExpressionIntercode
{
    private final boolean negated;
    private final SqlIntercode pattern;
    private final UsedVariables variables;


    protected SqlExists(boolean negated, SqlIntercode pattern, Map<ResourceClass, List<Column>> mappings,
            UsedVariables variables)
    {
        super(mappings, false, pattern.isDeterministic());

        this.negated = negated;
        this.pattern = pattern;
        this.variables = variables;

        this.referencedVariables.addAll(pattern.getVariables().getNames());
    }


    public static SqlExpressionIntercode create(Request request, boolean negated, SqlIntercode pattern,
            UsedVariables variables)
    {
        return create(request, negated, pattern, variables, Restriction.ALL);
    }


    public static SqlExpressionIntercode create(Request request, boolean negated, SqlIntercode pattern,
            UsedVariables variables, Restriction restriction)
    {
        if(pattern.equals(SqlNoSolution.get()))
            return negated ? trueValue : falseValue;

        if(pattern.equals(SqlEmptySolution.get()))
            return negated ? falseValue : trueValue;

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(pattern.getVariables(), variables);

        if(pairs.stream().anyMatch(p -> !p.isJoinable()))
            return negated ? trueValue : falseValue;


        if(!restriction.contains(xsdBoolean))
            return new SqlExists(negated, pattern, singletonMap(xsdBoolean, null), variables);


        List<Column> result = translate(request, negated, pattern, variables);

        return new SqlExists(negated, pattern, singletonMap(xsdBoolean, result), variables);
    }


    @Override
    public Restrictions getRequirements()
    {
        return SqlIntercode.getJoinRestrictions(variables, pattern.getVariables(), new Restrictions());
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, Restriction restriction,
            boolean evalServices)
    {
        SqlIntercode optPattern = pattern;

        Restrictions restrictions = SqlIntercode.getJoinRestrictions(optPattern.getVariables(), variables,
                new Restrictions());

        while(true)
        {
            optPattern = optPattern.optimize(request, restrictions, true, evalServices);

            if(optPattern instanceof SqlUnion union)
            {
                List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

                for(SqlIntercode child : union.getChilds())
                {
                    ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(child.getVariables(), variables);

                    if(pairs.stream().allMatch(p -> p.isJoinable()))
                        unionList.add(child);
                }

                optPattern = SqlUnion.union(request, unionList).optimize(request, restrictions, true, evalServices);
            }

            Restrictions optRestrictions = SqlIntercode.getJoinRestrictions(optPattern.getVariables(), variables,
                    new Restrictions());

            if(optRestrictions.equals(restrictions))
                break;

            restrictions = optRestrictions;
        }


        if(optPattern == pattern && variables.equals(this.variables))
            return this;

        return create(request, negated, optPattern, variables, restriction);
    }


    public static List<Column> translate(Request request, boolean negated, SqlIntercode pattern,
            UsedVariables variables)
    {
        //NOTE: rename pattern columns to prevent collisions

        Map<Column, Column> map = new HashMap<Column, Column>();
        pattern.getVariables().getNonConstantColumns().forEach(c -> map.put(c, new TableColumn("@cnd" + map.size())));

        UsedVariables cndvariables = new UsedVariables();

        for(UsedVariable var : pattern.getVariables().getValues())
        {
            UsedVariable cndvar = new UsedVariable(var.getName(), var.canBeNull());

            for(Entry<ResourceClass, List<Column>> entry : var.getMappings().entrySet())
                cndvar.addMapping(entry.getKey(),
                        entry.getValue().stream().map(c -> map.containsKey(c) ? map.get(c) : c).toList());

            cndvariables.add(cndvar);
        }


        StringBuilder builder = new StringBuilder();

        if(negated)
            builder.append("NOT ");

        builder.append("EXISTS ( SELECT 1 FROM (SELECT ");

        Set<Column> columns = pattern.getVariables().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> c + " AS " + map.get(c)).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");

        builder.append(pattern.translate(request));

        builder.append(") AS tab) AS tabcnd");

        String condition = SqlIntercode.generateJoinCondition(cndvariables, variables, null, null);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        builder.append(")");

        return List.of(new ExpressionColumn(builder.toString(), false));
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        String existsIndent = indent + "  ";

        builder.append("(").append(existsIndent);

        if(negated)
            builder.append("not ");

        builder.append("exists");

        indentChild(builder, existsIndent, true);
        pattern.generateExplanation(builder, getIndent(existsIndent, true));

        builder.append(existsIndent).append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlExists imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(negated, imcode.negated))
            return false;

        if(!Objects.equals(pattern, imcode.pattern))
            return false;

        if(!Objects.equals(variables, imcode.variables))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(negated, pattern);
    }
}

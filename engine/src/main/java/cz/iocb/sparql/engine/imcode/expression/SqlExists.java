package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
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
import cz.iocb.sparql.engine.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.imcode.SqlUnion;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlExists extends SqlExpressionIntercode
{
    private final boolean negated;
    private final SqlIntercode pattern;
    private final VariableBindings bindings;


    protected SqlExists(boolean negated, SqlIntercode pattern, Map<ResourceClass, List<Column>> mappings,
            VariableBindings bindings)
    {
        super(mappings, false, pattern.isDeterministic());

        this.negated = negated;
        this.pattern = pattern;
        this.bindings = bindings;

        this.referencedVariables.addAll(pattern.getVariableBindings().getVariables());
    }


    public static SqlExpressionIntercode create(Request request, boolean negated, SqlIntercode pattern,
            VariableBindings bindings)
    {
        return create(request, negated, pattern, bindings, Restriction.ALL);
    }


    public static SqlExpressionIntercode create(Request request, boolean negated, SqlIntercode pattern,
            VariableBindings bindings, Restriction restriction)
    {
        if(pattern.equals(SqlNoSolution.get()))
            return negated ? trueValue : falseValue;

        if(pattern.equals(SqlEmptySolution.get()))
            return negated ? falseValue : trueValue;

        List<VariableBindingPair> pairs = VariableBindingPair.getPairs(pattern.getVariableBindings(), bindings);

        if(pairs.stream().anyMatch(p -> !p.isJoinable()))
            return negated ? trueValue : falseValue;


        if(!restriction.contains(xsdBoolean))
            return new SqlExists(negated, pattern, singletonMap(xsdBoolean, null), bindings);


        List<Column> result = translate(request, negated, pattern, bindings);

        return new SqlExists(negated, pattern, singletonMap(xsdBoolean, result), bindings);
    }


    @Override
    public Restrictions getRequirements()
    {
        return SqlIntercode.getJoinRestrictions(bindings, pattern.getVariableBindings(), new Restrictions());
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        SqlIntercode optPattern = pattern;

        Restrictions restrictions = SqlIntercode.getJoinRestrictions(optPattern.getVariableBindings(), bindings,
                new Restrictions());

        while(true)
        {
            optPattern = optPattern.optimize(request, restrictions, true, evalServices);

            if(optPattern instanceof SqlUnion union)
            {
                List<SqlIntercode> unionList = new ArrayList<>();

                for(SqlIntercode child : union.getChilds())
                {
                    List<VariableBindingPair> pairs = VariableBindingPair.getPairs(child.getVariableBindings(),
                            bindings);

                    if(pairs.stream().allMatch(p -> p.isJoinable()))
                        unionList.add(child);
                }

                optPattern = SqlUnion.union(request, unionList).optimize(request, restrictions, true, evalServices);
            }

            Restrictions optRestrictions = SqlIntercode.getJoinRestrictions(optPattern.getVariableBindings(), bindings,
                    new Restrictions());

            if(optRestrictions.equals(restrictions))
                break;

            restrictions = optRestrictions;
        }


        if(optPattern == pattern && bindings.equals(this.bindings))
            return this;

        return create(request, negated, optPattern, bindings, restriction);
    }


    public static List<Column> translate(Request request, boolean negated, SqlIntercode pattern,
            VariableBindings bindings)
    {
        //NOTE: rename pattern columns to prevent collisions

        Map<Column, Column> map = new HashMap<>();
        pattern.getVariableBindings().getNonConstantColumns()
                .forEach(c -> map.put(c, new TableColumn("@cnd" + map.size())));

        VariableBindings cndBindings = new VariableBindings();

        for(VariableBinding binding : pattern.getVariableBindings().getValues())
        {
            VariableBinding cndBinding = new VariableBinding(binding.getVariable(), binding.canBeNull());

            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                cndBinding.addMapping(entry.getKey(),
                        entry.getValue().stream().map(c -> map.containsKey(c) ? map.get(c) : c).toList());

            cndBindings.add(cndBinding);
        }


        StringBuilder builder = new StringBuilder();

        if(negated)
            builder.append("NOT ");

        builder.append("EXISTS ( SELECT 1 FROM (SELECT ");

        Set<Column> columns = pattern.getVariableBindings().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> c + " AS " + map.get(c)).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");

        builder.append(pattern.translate(request));

        builder.append(") AS tab) AS tabcnd");

        String condition = SqlIntercode.generateJoinCondition(cndBindings, bindings, null, null);

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

        if(!Objects.equals(bindings, imcode.bindings))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(negated, pattern);
    }
}

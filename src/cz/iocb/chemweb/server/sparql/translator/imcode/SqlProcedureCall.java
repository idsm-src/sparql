package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlProcedureCall extends SqlIntercode
{
    private static final String resultName = "@res";

    private final ProcedureDefinition procedure;
    private final LinkedHashMap<ParameterDefinition, Node> parameters;
    private final LinkedHashMap<ResultDefinition, Node> results;
    private final SqlIntercode context;


    protected SqlProcedureCall(UsedVariables variables, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, Node> parameters, LinkedHashMap<ResultDefinition, Node> results,
            SqlIntercode context)
    {
        super(variables);
        this.procedure = procedure;
        this.parameters = parameters;
        this.results = results;
        this.context = context;
    }


    public static SqlIntercode create(ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, Node> parameters, LinkedHashMap<ResultDefinition, Node> results,
            SqlIntercode context)
    {
        UsedVariables callVariables = new UsedVariables();
        boolean hasSolution = true;
        boolean useContext = false;


        for(Entry<ParameterDefinition, Node> entry : parameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            Node node = entry.getValue();

            if(node instanceof VariableOrBlankNode)
            {
                useContext = true;

                String name = ((VariableOrBlankNode) node).getName();
                UsedVariable usedVariable = callVariables.get(name);

                if(usedVariable == null)
                {
                    callVariables.add(new UsedVariable(name, definition.getParameterClass(), false));
                }
                else if(!usedVariable.getClasses().contains(definition.getParameterClass()))
                {
                    hasSolution = false;
                    usedVariable.getClasses().add(definition.getParameterClass());
                }
            }
            else if(node == null || !definition.getParameterClass().match(node))
            {
                hasSolution = false;
            }
        }


        for(Entry<ResultDefinition, Node> entry : results.entrySet())
        {
            ResultDefinition definition = entry.getKey();
            Node node = entry.getValue();

            if(node instanceof VariableOrBlankNode)
            {
                String name = ((VariableOrBlankNode) node).getName();
                UsedVariable usedVariable = callVariables.get(name);

                if(usedVariable == null)
                {
                    callVariables.add(new UsedVariable(name, definition.getResultClass(), false));
                }
                else if(!usedVariable.getClasses().contains(definition.getResultClass()))
                {
                    hasSolution = false;
                    usedVariable.getClasses().add(definition.getResultClass());
                }
            }
            else if(node == null || !definition.getResultClass().match(node))
            {
                System.err.println("A");
                hasSolution = false;
            }
        }


        if(!hasSolution)
            return new SqlNoSolution();


        SqlIntercode originalContext = null;

        if(!useContext && !(context instanceof SqlEmptySolution))
        {
            originalContext = context;
            context = new SqlEmptySolution();
        }


        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(callVariables, context.getVariables());
        UsedVariables variables = new UsedVariables();

        for(UsedPairedVariable pair : pairs)
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();


            String var = pair.getName();
            boolean canBeLeftNull = leftVariable == null ? true : leftVariable.canBeNull();
            boolean canBeRightNull = rightVariable == null ? true : rightVariable.canBeNull();

            UsedVariable variable = new UsedVariable(var, canBeLeftNull && canBeRightNull);
            variables.add(variable);


            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() == null)
                {
                    if(leftVariable != null && !leftVariable.canBeNull())
                        continue;

                    variable.addClass(pairedClass.getRightClass());
                }
                else if(pairedClass.getRightClass() == null)
                {
                    if(rightVariable != null && !rightVariable.canBeNull())
                        continue;

                    variable.addClass(pairedClass.getLeftClass());
                }
                else
                {
                    assert pairedClass.getLeftClass() == pairedClass.getRightClass();
                    variable.addClass(pairedClass.getLeftClass());
                }
            }

            if(variable.getClasses().isEmpty())
                return new SqlNoSolution();
        }


        SqlProcedureCall call = new SqlProcedureCall(variables, procedure, parameters, results, context);

        if(originalContext == null)
            return call;

        return SqlJoin.join(null, call, originalContext);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;
        HashSet<String> usedInSelect = new HashSet<String>();

        for(Entry<ResultDefinition, Node> entry : results.entrySet())
        {
            if(entry.getValue() == null)
                continue;


            Node node = entry.getValue();

            if(!(node instanceof VariableOrBlankNode))
                continue;


            String name = ((VariableOrBlankNode) node).getName();

            if(context.getVariables().get(name) != null)
                continue;


            if(usedInSelect.contains(name))
                continue;

            usedInSelect.add(name);


            ResultDefinition definition = entry.getKey();

            PatternResourceClass resultClass = definition.getResultClass();
            String[] fields = definition.getSqlTypeFields();

            for(int i = 0; i < resultClass.getPartsCount(); i++)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append(getSqlResultColumn(fields, i));
                builder.append(" AS ");
                builder.append(resultClass.getSqlColumn(name, i));
            }
        }


        for(UsedVariable variable : context.getVariables().getValues())
        {
            for(PatternResourceClass resClass : variables.get(variable.getName()).getClasses())
            {
                for(int i = 0; i < resClass.getPartsCount(); i++)
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
        generateInnerSelect(builder);
        builder.append(" ) AS tab ");


        builder.append(" WHERE ");
        boolean hasWhere = false;


        HashMap<String, ResultDefinition> notUsedInWhere = new HashMap<String, ResultDefinition>();

        for(Entry<ResultDefinition, Node> entry : results.entrySet())
        {
            if(entry.getValue() == null)
                continue;


            Node node = entry.getValue();

            ResultDefinition definition = entry.getKey();
            String[] fields = definition.getSqlTypeFields();
            PatternResourceClass resultClass = definition.getResultClass();

            if(node instanceof VariableOrBlankNode)
            {
                String name = ((VariableOrBlankNode) node).getName();
                ResultDefinition previousDefinition = notUsedInWhere.get(name);

                if(context.getVariables().get(name) != null)
                {
                    for(int i = 0; i < resultClass.getPartsCount(); i++)
                    {
                        appendAnd(builder, hasWhere);
                        hasWhere = true;

                        builder.append(getSqlResultColumn(fields, i));
                        builder.append(" = ");
                        builder.append(resultClass.getSqlColumn(name, i));
                    }
                }
                else if(previousDefinition != null)
                {
                    for(int i = 0; i < resultClass.getPartsCount(); i++)
                    {
                        appendAnd(builder, hasWhere);
                        hasWhere = true;

                        builder.append(getSqlResultColumn(fields, i));
                        builder.append(" = ");
                        builder.append(getSqlResultColumn(previousDefinition.getSqlTypeFields(), i));
                    }
                }
                else
                {
                    notUsedInWhere.put(name, entry.getKey());
                }
            }
            else
            {
                for(int i = 0; i < resultClass.getPartsCount(); i++)
                {
                    appendAnd(builder, hasWhere);
                    hasWhere = true;

                    builder.append(getSqlResultColumn(fields, i));
                    builder.append(" = ");
                    builder.append(resultClass.getSqlValue(node, i));
                }
            }
        }


        if(!hasWhere)
            builder.append("true");


        return builder.toString();
    }


    private void generateInnerSelect(StringBuilder builder)
    {
        builder.append("SELECT ");


        builder.append('"');
        builder.append(procedure.getSqlProcedureName());
        builder.append('"');


        boolean hasParameter = false;
        builder.append("(");

        for(Entry<ParameterDefinition, Node> entry : parameters.entrySet())
        {
            PatternResourceClass resClass = entry.getKey().getParameterClass();

            appendComma(builder, hasParameter);
            hasParameter = true;

            for(int i = 0; i < resClass.getPartsCount(); i++)
            {
                appendComma(builder, i > 0);
                builder.append(resClass.getSqlValue(entry.getValue(), i));
            }
        }

        builder.append(")");

        builder.append(" AS \"");
        builder.append(resultName);
        builder.append('"');

        for(UsedVariable variable : context.getVariables().getValues())
        {
            for(PatternResourceClass resClass : variables.get(variable.getName()).getClasses())
            {
                for(int i = 0; i < resClass.getPartsCount(); i++)
                {
                    appendComma(builder, true);
                    builder.append(resClass.getSqlColumn(variable.getName(), i));
                }
            }
        }


        if(!(context instanceof SqlEmptySolution))
        {
            builder.append(" FROM (");
            builder.append(context.translate());
            builder.append(" ) AS tab");

            builder.append(" WHERE ");
            boolean hasWhere = false;

            for(Entry<ParameterDefinition, Node> entry : parameters.entrySet())
            {
                Node node = entry.getValue();

                if(!(node instanceof VariableOrBlankNode))
                    continue;

                UsedVariable variable = context.getVariables().get(((VariableOrBlankNode) node).getName());

                if(variable.canBeNull() == false && variable.getClasses().size() == 1)
                    continue;


                PatternResourceClass parameterClass = entry.getKey().getParameterClass();

                appendAnd(builder, hasWhere);
                hasWhere = true;

                for(int i = 0; i < parameterClass.getPartsCount(); i++)
                {
                    appendAnd(builder, hasWhere);
                    builder.append(parameterClass.getSqlColumn(variable.getName(), i));
                    builder.append(" IS NOT NULL");
                }
            }


            if(!hasWhere)
                builder.append("true");
        }
    }


    private static String getSqlResultColumn(String[] fields, int i)
    {
        if(fields == null)
            return "\"" + resultName + "\"";
        else
            return "(\"" + resultName + "\").\"" + fields[i] + "\"";
    }
}

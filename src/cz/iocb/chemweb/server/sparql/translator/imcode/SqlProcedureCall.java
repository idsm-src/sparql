package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.extension.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
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
        super(variables, context.isDeterministic()); //TODO: is procedure deterministic?
        this.procedure = procedure;
        this.parameters = parameters;
        this.results = results;
        this.context = context;
    }


    public static SqlIntercode create(ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, Node> parameters, LinkedHashMap<ResultDefinition, Node> results,
            SqlIntercode context, Request request, HashSet<String> restrictions)
    {
        UsedVariables callVariables = new UsedVariables();
        HashSet<String> parameterVariables = new HashSet<String>();
        boolean hasSolution = true;
        boolean useContext = false;


        for(Entry<ParameterDefinition, Node> entry : parameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            Node node = entry.getValue();

            if(node instanceof VariableOrBlankNode)
            {
                useContext = true;

                String name = ((VariableOrBlankNode) node).getSqlName();
                UsedVariable usedVariable = callVariables.get(name);
                parameterVariables.add(name);

                if(usedVariable == null)
                {
                    callVariables.add(new UsedVariable(name, definition.getParameterClass(), false));
                }
                else if(usedVariable.getClasses().contains(definition.getParameterClass().getGeneralClass()))
                {
                    usedVariable.getClasses().remove(definition.getParameterClass().getGeneralClass());
                    usedVariable.getClasses().add(definition.getParameterClass());
                }
                else if(!usedVariable.getClasses().contains(definition.getParameterClass()))
                {
                    hasSolution = false;
                    usedVariable.getClasses().add(definition.getParameterClass());
                }
            }
            else if(node == null || !definition.getParameterClass().match(node, request))
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
                String name = ((VariableOrBlankNode) node).getSqlName();
                UsedVariable usedVariable = callVariables.get(name);

                if(usedVariable == null)
                {
                    callVariables.add(new UsedVariable(name, definition.getResultClass(), false));
                }
                else if(usedVariable.getClasses().contains(definition.getResultClass().getGeneralClass()))
                {
                    usedVariable.getClasses().remove(definition.getResultClass().getGeneralClass());
                    usedVariable.getClasses().add(definition.getResultClass());
                }
                else if(!usedVariable.getClasses().contains(definition.getResultClass()))
                {
                    hasSolution = false;
                    usedVariable.getClasses().add(definition.getResultClass());
                }
            }
            else if(node == null || !definition.getResultClass().match(node, request))
            {
                hasSolution = false;
            }
        }


        if(!hasSolution)
            return SqlNoSolution.get();


        SqlIntercode originalContext = null;

        if(!useContext && context != SqlEmptySolution.get())
        {
            originalContext = context;
            context = SqlEmptySolution.get();
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
                    if(leftVariable != null)
                        continue;

                    variable.addClass(pairedClass.getRightClass());
                }
                else if(pairedClass.getRightClass() == null)
                {
                    if(parameterVariables.contains(leftVariable.getName())
                            || rightVariable != null && !rightVariable.canBeNull())
                        continue;

                    variable.addClass(pairedClass.getLeftClass());
                }
                else if(pairedClass.getLeftClass() == pairedClass.getRightClass().getGeneralClass()
                        && (!rightVariable.canBeNull() || parameterVariables.contains(leftVariable.getName())))
                {
                    variable.addClass(pairedClass.getRightClass());
                }
                else
                {
                    variable.addClass(pairedClass.getLeftClass());
                }
            }

            if(variable.getClasses().isEmpty())
                return SqlNoSolution.get();
        }


        SqlProcedureCall call = new SqlProcedureCall(variables, procedure, parameters, results, context);

        if(originalContext == null)
            return call;

        return SqlJoin.join(null, call, originalContext);
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        HashSet<String> contextRestrictions = new HashSet<String>(restrictions);

        for(Node paramater : parameters.values())
            if(paramater instanceof VariableOrBlankNode)
                contextRestrictions.add(((VariableOrBlankNode) paramater).getSqlName());

        //FIXME: is procedure deterministic?
        SqlIntercode optimized = context.optimize(request, contextRestrictions, reduced);

        return create(procedure, parameters, results, optimized, request, restrictions);
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
            Node node = entry.getValue();

            if(!(node instanceof VariableOrBlankNode))
                continue;


            String name = ((VariableOrBlankNode) node).getSqlName();

            if(variables.get(name) == null)
                continue;

            ResultDefinition definition = entry.getKey();
            ResourceClass resultClass = definition.getResultClass();

            if(context.getVariables().contains(name, resultClass))
                continue;

            if(!getVariables().contains(name, resultClass))
                continue;

            if(usedInSelect.contains(name))
                continue;

            usedInSelect.add(name);


            String[] fields = definition.getSqlTypeFields();

            for(int i = 0; i < resultClass.getPatternPartsCount(); i++)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append(getSqlResultColumn(fields, i));
                builder.append(" AS ");
                builder.append(resultClass.getSqlColumn(name, i));
            }
        }


        for(UsedVariable variable : variables.getValues())
        {
            if(variables.get(variable.getName()) == null)
                continue;

            for(ResourceClass resClass : variables.get(variable.getName()).getClasses())
            {
                if(context.getVariables().contains(variable.getName(), resClass))
                {
                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append(resClass.getSqlColumn(variable.getName(), i));
                    }
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
            Node node = entry.getValue();

            if(node == null)
                continue;


            ResultDefinition definition = entry.getKey();
            String[] fields = definition.getSqlTypeFields();
            ResourceClass resultClass = definition.getResultClass();

            if(node instanceof VariableOrBlankNode)
            {
                String name = ((VariableOrBlankNode) node).getSqlName();
                ResultDefinition previousDefinition = notUsedInWhere.get(name);

                UsedVariable variable = context.getVariables().get(name);

                if(variable != null)
                {
                    String varName = variable.getName();

                    if(variable.getClasses().contains(resultClass))
                    {
                        for(int i = 0; i < resultClass.getPatternPartsCount(); i++)
                        {
                            appendAnd(builder, hasWhere);
                            hasWhere = true;

                            builder.append(getSqlResultColumn(fields, i));
                            builder.append(" = ");
                            builder.append(resultClass.getSqlColumn(varName, i));
                        }
                    }
                    else if(variable.getClasses().contains(resultClass.getGeneralClass()))
                    {
                        for(int i = 0; i < resultClass.getPatternPartsCount(); i++)
                        {
                            appendAnd(builder, hasWhere);
                            hasWhere = true;

                            builder.append(getSqlResultColumn(fields, i));
                            builder.append(" = ");
                            builder.append(resultClass.getSpecialisedPatternCode(null, varName, i));
                        }
                    }
                    else
                    {
                        Set<ResourceClass> compatibleClasses = variable.getCompatible(resultClass);

                        for(int i = 0; i < resultClass.getPatternPartsCount(); i++)
                        {
                            appendAnd(builder, hasWhere);
                            hasWhere = true;

                            builder.append(getSqlResultColumn(fields, i));
                            builder.append(" = ");

                            if(compatibleClasses.size() > 1)
                                builder.append("COALESCE(");

                            boolean hasVariant = false;

                            for(ResourceClass compatibleClass : compatibleClasses)
                            {
                                appendComma(builder, hasVariant);
                                hasVariant = true;

                                builder.append(compatibleClass.getGeneralisedPatternCode(null, varName, i,
                                        compatibleClasses.size() > 1));
                            }

                            if(compatibleClasses.size() > 1)
                                builder.append(")");
                        }
                    }
                }
                else if(previousDefinition != null)
                {
                    if(previousDefinition.getResultClass() == definition.getResultClass())
                    {
                        for(int i = 0; i < resultClass.getPatternPartsCount(); i++)
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
                        //TODO: At this time, we do not use any procedure that has two result values having two
                        //      different types that are compatible.
                        assert false;
                    }
                }
                else
                {
                    notUsedInWhere.put(name, entry.getKey());
                }
            }
            else
            {
                for(int i = 0; i < resultClass.getPatternPartsCount(); i++)
                {
                    appendAnd(builder, hasWhere);
                    hasWhere = true;

                    builder.append(getSqlResultColumn(fields, i));
                    builder.append(" = ");
                    builder.append(resultClass.getPatternCode(node, i));
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

        builder.append(procedure.getSqlProcedure().getCode());

        boolean hasParameter = false;
        builder.append("(");

        for(Entry<ParameterDefinition, Node> entry : parameters.entrySet())
        {
            appendComma(builder, hasParameter);
            hasParameter = true;

            ResourceClass resClass = entry.getKey().getParameterClass();
            Node node = entry.getValue();

            if(node instanceof VariableOrBlankNode)
            {
                String varName = ((VariableOrBlankNode) node).getSqlName();
                UsedVariable variable = context.getVariables().get(varName);

                if(variable.getClasses().contains(resClass))
                {
                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendComma(builder, i > 0);
                        builder.append(resClass.getSqlColumn(varName, i));
                    }
                }
                else if(variable.getClasses().contains(resClass.getGeneralClass()))
                {
                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendComma(builder, i > 0);
                        builder.append(resClass.getSpecialisedPatternCode(null, varName, i));
                    }
                }
                else
                {
                    Set<ResourceClass> compatibleClasses = variable.getCompatible(resClass);

                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendComma(builder, i > 0);

                        if(compatibleClasses.size() > 1)
                            builder.append("COALESCE(");

                        boolean hasVariant = false;

                        for(ResourceClass compatibleClass : compatibleClasses)
                        {
                            appendComma(builder, hasVariant);
                            hasVariant = true;

                            builder.append(compatibleClass.getGeneralisedPatternCode(null, varName, i,
                                    compatibleClasses.size() > 1));
                        }

                        if(compatibleClasses.size() > 1)
                            builder.append(")");
                    }
                }
            }
            else
            {
                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, i > 0);
                    builder.append(resClass.getPatternCode(entry.getValue(), i));
                }
            }
        }

        builder.append(")");

        builder.append(" AS \"");
        builder.append(resultName);
        builder.append('"');

        for(UsedVariable variable : context.getVariables().getValues())
        {
            if(variables.get(variable.getName()) == null)
                continue;

            for(ResourceClass resClass : context.getVariables().get(variable.getName()).getClasses())
            {
                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, true);
                    builder.append(resClass.getSqlColumn(variable.getName(), i));
                }
            }
        }


        if(context != SqlEmptySolution.get())
        {
            builder.append(" FROM (");
            builder.append(context.translate());
            builder.append(" ) AS tab");
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

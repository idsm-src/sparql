package cz.iocb.chemweb.server.sparql.translator.sql.imcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariables;



public class SqlProcedureCall extends SqlIntercode
{
    public static class ClassifiedNode
    {
        private final Node node;
        private final ResourceClass nodeClass;

        public ClassifiedNode(Node node, ResourceClass nodeClass)
        {
            this.node = node;
            this.nodeClass = nodeClass;
        }

        public final Node getNode()
        {
            return node;
        }

        public final ResourceClass getNodeClass()
        {
            return nodeClass;
        }
    }


    private final ProcedureDefinition procedure;
    private final LinkedHashMap<ParameterDefinition, ClassifiedNode> parameters;
    private final LinkedHashMap<ResultDefinition, ClassifiedNode> results;
    private final SqlIntercode context;


    protected SqlProcedureCall(UsedVariables variables, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, ClassifiedNode> parameters,
            LinkedHashMap<ResultDefinition, ClassifiedNode> results, SqlIntercode context)
    {
        super(variables);
        this.procedure = procedure;
        this.parameters = parameters;
        this.results = results;
        this.context = context;
    }


    public static SqlIntercode create(ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, ClassifiedNode> parameters,
            LinkedHashMap<ResultDefinition, ClassifiedNode> results, SqlIntercode context)
    {
        UsedVariables callVariables = new UsedVariables();
        boolean hasSolution = true;
        boolean useContext = false;


        for(Entry<ParameterDefinition, ClassifiedNode> entry : parameters.entrySet())
        {
            if(entry.getValue() == null)
                continue;

            ParameterDefinition definition = entry.getKey();
            Node node = entry.getValue().getNode();
            ResourceClass nodeClass = entry.getValue().getNodeClass();

            if(nodeClass != null && nodeClass != definition.getParameterClass())
                hasSolution = false;

            if(nodeClass == null && !(node instanceof VariableOrBlankNode))
                hasSolution = false;


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
        }


        for(Entry<ResultDefinition, ClassifiedNode> entry : results.entrySet())
        {
            if(entry.getValue() == null)
                continue;

            ResultDefinition definition = entry.getKey();
            Node node = entry.getValue().getNode();
            ResourceClass nodeClass = entry.getValue().getNodeClass();

            if(nodeClass != null && nodeClass != definition.getResultClass())
                hasSolution = false;

            if(nodeClass == null && !(node instanceof VariableOrBlankNode))
                hasSolution = false;


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
        }


        if(!hasSolution)
        {
            UsedVariables variables = new UsedVariables();

            for(UsedVariable variable : callVariables.getValues())
                variables.add(new UsedVariable(variable.getName(), true));

            for(UsedVariable variable : context.getVariables().getValues())
                if(variables.get(variable.getName()) != null)
                    variables.add(new UsedVariable(variable.getName(), true));

            return new SqlNoSolution(variables);
        }


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

            if((leftVariable == null || leftVariable.getClasses().isEmpty())
                    && (rightVariable == null || rightVariable.getClasses().isEmpty()))
                continue;


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
            {
                UsedVariables noSolutionVariables = new UsedVariables();

                for(UsedVariable usedVar : variables.getValues())
                    noSolutionVariables.add(new UsedVariable(usedVar.getName(), true));

                return new SqlNoSolution(noSolutionVariables);
            }
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

        for(Entry<ResultDefinition, ClassifiedNode> entry : results.entrySet())
        {
            if(entry.getValue() == null)
                continue;


            Node node = entry.getValue().getNode();

            if(!(node instanceof VariableOrBlankNode))
                continue;


            String name = ((VariableOrBlankNode) node).getName();

            if(context.getVariables().get(name) != null)
                continue;


            if(usedInSelect.contains(name))
                continue;

            usedInSelect.add(name);


            ResultDefinition definition = entry.getKey();

            ResourceClass resultClass = definition.getResultClass();
            String field = definition.getSqlTypeField();

            for(int i = 0; i < resultClass.getPartsCount(); i++)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append(resultClass.getSqlColumn(field, i));
                builder.append(" AS ");
                builder.append(resultClass.getSqlColumn(name, i));
            }
        }


        for(UsedVariable variable : context.getVariables().getValues())
        {
            for(ResourceClass resClass : variables.get(variable.getName()).getClasses())
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

        for(Entry<ResultDefinition, ClassifiedNode> entry : results.entrySet())
        {
            if(entry.getValue() == null)
                continue;


            Node node = entry.getValue().getNode();

            ResultDefinition definition = entry.getKey();
            String field = definition.getSqlTypeField();
            ResourceClass resultClass = definition.getResultClass();

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

                        builder.append(resultClass.getSqlColumn(field, i));
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

                        builder.append(resultClass.getSqlColumn(field, i));
                        builder.append(" = ");
                        builder.append(resultClass.getSqlColumn(previousDefinition.getSqlTypeField(), i));
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

                    builder.append(resultClass.getSqlColumn(field, i));
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


        if(procedure.getSqlReturnType() != null)
            builder.append("(");

        builder.append('"');
        builder.append(procedure.getSqlProcedureName());
        builder.append('"');


        boolean hasParameter = false;
        builder.append("(");

        for(Entry<ParameterDefinition, ClassifiedNode> entry : parameters.entrySet())
        {
            ResourceClass resClass = entry.getKey().getParameterClass();

            appendComma(builder, hasParameter);
            hasParameter = true;

            for(int i = 0; i < resClass.getPartsCount(); i++)
            {
                appendComma(builder, i > 0);
                builder.append(resClass.getSqlValue(entry.getValue().getNode(), i));
            }
        }

        builder.append(")");


        if(procedure.getSqlReturnType() != null)
        {
            builder.append("::");
            builder.append(procedure.getSqlReturnType());
            builder.append(").*");
        }
        else
        {
            builder.append(" AS ");
            builder.append(
                    procedure.getResult(null).getResultClass().getSqlColumn(ResultDefinition.singleResultName, 0));
        }


        for(UsedVariable variable : context.getVariables().getValues())
        {
            for(ResourceClass resClass : variables.get(variable.getName()).getClasses())
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

            for(Entry<ParameterDefinition, ClassifiedNode> entry : parameters.entrySet())
            {
                Node node = entry.getValue().getNode();

                if(!(node instanceof VariableOrBlankNode))
                    continue;

                UsedVariable variable = context.getVariables().get(((VariableOrBlankNode) node).getName());

                if(variable.canBeNull() == false && variable.getClasses().size() == 1)
                    continue;


                ResourceClass parameterClass = entry.getKey().getParameterClass();

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
}

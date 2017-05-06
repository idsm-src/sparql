package cz.iocb.chemweb.server.sparql.translator.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.ToStringVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.Define;
import cz.iocb.chemweb.server.sparql.parser.model.GroupCondition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition;
import cz.iocb.chemweb.server.sparql.parser.model.Projection;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BuiltInCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Bind;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Filter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Graph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GraphPattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GroupGraph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Minus;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.MultiProcedureCall;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Optional;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Pattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCall;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCallBase;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCallBase.Parameter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Service;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Union;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values.ValuesList;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.NodeOptions;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;
import cz.iocb.chemweb.server.sparql.translator.Pair;
import cz.iocb.chemweb.server.sparql.translator.TranslateResult;
import cz.iocb.chemweb.server.sparql.translator.config.Config;
import cz.iocb.chemweb.server.sparql.translator.config.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.translator.config.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.translator.config.ResultDefinition;
import cz.iocb.chemweb.server.sparql.translator.error.ErrorType;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateExceptions;
import cz.iocb.chemweb.server.sparql.translator.visitor.GraphOrServiceRestriction.RestrictionType;



public class SparqlTranslateVisitor extends ElementVisitor<TranslatedSegment>
{
    private static final String tempStarFakePrefix = "_star_fake_";
    private static final String tempVarPrefix = "__tmpVar";
    private static final int indentMultiplier = 2;

    private boolean topLevelSelect = true;
    private int queryDepth = 0;

    private int uniqueStarFakeID = 1;
    private int uniqueVarID = 1;

    private Map<String, List<String>> propertyChains = null;

    private Config proceduresConfig = null;
    private Prologue prologue;
    private final Stack<GraphOrServiceRestriction> graphOrServiceRestrictions = new Stack<>();
    private final Stack<List<DataSet>> selectDatasets = new Stack<>();
    private HashSet<String> queryVariables;
    private final HashMap<String, String> blankNodesTmpVarHashes = new HashMap<>();
    private final List<TranslateException> exceptions = new LinkedList<TranslateException>();
    private final List<TranslateException> warnings = new LinkedList<TranslateException>();

    List<String> codes = new LinkedList<String>();



    public SparqlTranslateVisitor(Map<String, List<String>> propertyChains)
    {
        this.propertyChains = propertyChains;
    }


    /**
     * Constructs the translator object.
     *
     * @param sparqlQuery SPARQL query.
     * @return String of a translated query.
     * @throws cz.iocb.chemweb.server.sparql.translator.error.TranslateException
     */
    public List<String> translate(SelectQuery sparqlQuery) throws TranslateExceptions
    {
        return translate(sparqlQuery, new Config());
    }


    /**
     * Constructs the translator object (with the use of inner procedures).
     *
     * @param sparqlQuery SPARQL query.
     * @param proceduresConfig Inner procedures settings.
     * @return String of a translated query.
     * @throws cz.iocb.chemweb.server.sparql.translator.error.TranslateException
     */
    public List<String> translate(SelectQuery sparqlQuery, Config proceduresConfig) throws TranslateExceptions
    {
        this.proceduresConfig = proceduresConfig;

        String code = visitElement(sparqlQuery).str;

        codes.add(code);

        if(!exceptions.isEmpty())
            throw new TranslateExceptions(exceptions);

        return codes;
    }


    /**
     * Constructs the translator object (with the use of inner procedures).
     *
     * @param sparqlQuery SPARQL query.
     * @param proceduresConfig Inner procedures settings.
     * @param fullQueryDecomposition Specify whether the result query should be consisting of minimal SPARQL chunks
     *            (otherwise, only necessary decomposition is used).
     * @return String of a translated query.
     * @throws cz.iocb.chemweb.server.sparql.translator.error.TranslateException
     */
    public TranslateResult tryTranslate(SelectQuery sparqlQuery, Config proceduresConfig,
            boolean fullQueryDecomposition)
    {
        this.proceduresConfig = proceduresConfig;

        TranslatedSegment result = visitElement(sparqlQuery);
        return new TranslateResult(result != null ? result.str : null, exceptions, warnings);
    }

    //-------------------------------------------------------------------------


    /**
     * Translates the top-level SPARQL SELECT (with a prologue).
     *
     * @param selectQuery Pattern to be translated.
     * @return Translated top-level SELECT pattern.
     */
    @Override
    public TranslatedSegment visit(SelectQuery selectQuery)
    {
        // store some data later used in the translation
        prologue = selectQuery.getPrologue();
        queryVariables = selectQuery.getQueryVariables();

        // actual translation of the SELECT clause
        TranslatedSegment translatedSelectClause = visitElement(selectQuery.getSelect());

        // building the string
        StringBuilder strBuf = new StringBuilder();
        newLine(strBuf);
        strBuf.append("sparql define input:storage virtrdf:PubchemQuadStorage");

        for(Define def : prologue.getDefines())
            strBuf.append(" ").append(def.toString());

        newLine(strBuf);
        strBuf.append(translatedSelectClause.str);

        return new TranslatedSegment(strBuf.toString());
    }


    /**
     * Translates the SPARQL SELECT clause.
     *
     * @param select Pattern to be translated.
     * @return Translated SELECT pattern.
     */
    @Override
    public TranslatedSegment visit(Select select)
    {
        checkProjectionVariables(select);

        boolean isTopLevel = topLevelSelect;

        if(topLevelSelect)
            topLevelSelect = false;

        // register information about datasets used in this SELECT (sub)query
        selectDatasets.push(select.getDataSets());

        // translate the WHERE clause
        queryDepth++;
        TranslatedSegment translatedWhereClause = visitElement(select.getPattern());
        queryDepth--;

        TranslatedSegment translatedSelect = null;


        if(!select.getGroupByConditions().isEmpty())
        {
            translatedWhereClause = translateBindInGroupBy(select.getGroupByConditions(), translatedWhereClause);

            if(translatedWhereClause != null)
            {
                StringBuilder strBuf = new StringBuilder();

                strBuf.append("\n{");
                strBuf.append(indentBlock(translatedWhereClause.str, 1));
                strBuf.append("\n}");

                translatedWhereClause.str = strBuf.toString();
            }
        }

        translatedSelect = translateSelect(select, translatedWhereClause, isTopLevel);

        // clear information about datasets of this SELECT (sub)query
        selectDatasets.pop();

        return translatedSelect;
    }


    /**
     * Translates the current SPARQL block (group).
     *
     * @param groupGraph Details of the group.
     * @return Translated group pattern.
     */
    @Override
    public TranslatedSegment visit(GroupGraph groupGraph)
    {
        TranslatedSegment translatedGroupGraphPattern = translatePatternList(groupGraph.getPatterns());

        if(translatedGroupGraphPattern != null)
        {
            StringBuilder strBuf = new StringBuilder();

            strBuf.append("\n{");
            strBuf.append(indentBlock(translatedGroupGraphPattern.str, 1));
            strBuf.append("\n}");

            translatedGroupGraphPattern.str = strBuf.toString();
        }

        return translatedGroupGraphPattern;
    }


    /**
     * Translates the SPARQL GRAPH clause.
     *
     * @param graph Pattern to be translated.
     * @return Translated GRAPH pattern.
     */
    @Override
    public TranslatedSegment visit(Graph graph)
    {
        VarOrIri varOrIri = graph.getName();

        // register current graph restriction and translate inner block
        graphOrServiceRestrictions.push(new GraphOrServiceRestriction(RestrictionType.GRAPH_RESTRICTION, varOrIri));
        TranslatedSegment translatedPattern = visitElement(graph.getPattern());
        graphOrServiceRestrictions.pop();

        if(translatedPattern == null)
            return null;


        StringBuilder strBuf = new StringBuilder();

        newLine(strBuf);
        strBuf.append("GRAPH ").append(varOrIri.toString());
        strBuf.append(translatedPattern.str);

        return new TranslatedSegment(strBuf.toString(), translatedPattern.subqueryVars,
                translatedPattern.possibleUnboundVars);
    }


    /**
     * Translates the SPARQL SERVICE clause.
     *
     * @param service Pattern to be translated.
     * @return Translated SERVICE pattern.
     */
    @Override
    public TranslatedSegment visit(Service service)
    {
        VarOrIri varOrIri = service.getName();
        boolean isSilent = service.isSilent();

        // register current service restriction and translate inner block
        graphOrServiceRestrictions
                .push(new GraphOrServiceRestriction(RestrictionType.SERVICE_RESTRICTION, varOrIri).setSilent(isSilent));
        TranslatedSegment translatedPattern = visitElement(service.getPattern());
        graphOrServiceRestrictions.pop();


        StringBuilder strBuf = new StringBuilder();
        newLine(strBuf);

        if(isSilent)
            strBuf.append("SERVICE SILENT ");
        else
            strBuf.append("SERVICE ");

        strBuf.append(varOrIri.toString());
        strBuf.append(translatedPattern.str);

        return new TranslatedSegment(strBuf.toString(), translatedPattern.subqueryVars,
                translatedPattern.possibleUnboundVars);
    }


    /**
     * Translates the SPARQL UNION clause.
     *
     * @param union Pattern to be translated.
     * @return Translated UNION pattern.
     */
    @Override
    public TranslatedSegment visit(Union union)
    {
        List<TranslatedSegment> translatedPatterns = new ArrayList<>();
        LinkedHashSet<String> allUsedVars = new LinkedHashSet<>();
        LinkedHashSet<String> allPossibleUnboundVars = new LinkedHashSet<>();

        for(GraphPattern pattern : union.getPatterns())
        {
            TranslatedSegment translatedPattern = visitElement(pattern);

            if(translatedPattern == null)
                continue;

            translatedPatterns.add(translatedPattern);
            allUsedVars.addAll(translatedPattern.subqueryVars);
            allPossibleUnboundVars.addAll(translatedPattern.possibleUnboundVars);
        }


        if(translatedPatterns.isEmpty())
            return null;

        if(translatedPatterns.size() == 1)
            return translatedPatterns.get(0);


        StringBuilder strBuf = new StringBuilder();


        boolean firstPat = true;

        for(TranslatedSegment translatedPattern : translatedPatterns)
        {
            if(!firstPat)
            {
                strBuf.append("\n");
                strBuf.append("UNION");
            }
            firstPat = false;

            strBuf.append(translatedPattern.str);
        }

        return new TranslatedSegment(strBuf.toString(), allUsedVars, allPossibleUnboundVars);
    }


    /**
     * Translates the SPARQL VALUES clause.
     *
     * @param values Pattern to be translated.
     * @return Translated VALUES pattern.
     */
    @Override
    public TranslatedSegment visit(Values values)
    {
        LinkedHashSet<String> usedVars = new LinkedHashSet<>();

        for(Variable var : values.getVariables())
            usedVars.add(var.getName());


        LinkedHashSet<String> possibleUnboundVars = new LinkedHashSet<>();

        for(ValuesList valueList : values.getValuesLists())
            for(int i = 0; i < values.getVariables().size(); i++)
                if(valueList.getValues().get(i) == null)
                    possibleUnboundVars.add(values.getVariables().get(i).getName());

        return new TranslatedSegment("\n" + values.toString(), usedVars, possibleUnboundVars);
    }


    /**
     * Translates the SPARQL triple.
     *
     * @param triple Pattern to be translated.
     * @return Translated triple pattern.
     */
    @Override
    public TranslatedSegment visit(Triple triple)
    {
        triple = eliminateBlankNodesFromTriple(triple);

        LinkedHashSet<String> usedVars = extractVariblesFromTriple(triple);

        if(!graphOrServiceRestrictions.isEmpty())
        {
            VarOrIri varOrIri = graphOrServiceRestrictions.peek().getVarOrIri();

            if(varOrIri instanceof Variable)
                usedVars.add(((Variable) varOrIri).getName());
        }


        ToStringVisitor visitor = new ToStringVisitor()
        {
            @Override
            public Void visit(IRI iri)
            {
                String def = iri.toString(prologue, true, false);
                List<String> path = propertyChains != null ? propertyChains.get(def) : null;

                if(path == null)
                {
                    write(def);
                }
                else
                {
                    write("(");
                    separatedForEach(path, (p) -> write(p), () -> word("/"));
                    write(")");
                }

                return null;
            }
        };

        visitor.visitElement(triple.getPredicate());

        StringBuffer code = new StringBuffer();
        code.append("\n");

        code.append(triple.getSubject().toString());
        code.append(" ");
        code.append(visitor.getString());
        code.append(" ");
        code.append(triple.getObject().toString());

        NodeOptions options = triple.getObject().getNodeOptions();

        if(options != null && options.getTableOption() != null)
        {
            code.append(" option (table_option ").append(options.getTableOption()).append(')');
        }

        /**
         * Verb predicate = triple.getPredicate();
         *
         * if(predicate instanceof IRI) { String def = ((IRI) predicate).toString(prologue, true, false);
         *
         * if(def.equals("<http://semanticscience.org/resource/has-value>")) code.append( " option (table_option
         * \"loop\")"); }
         *
         **/

        code.append(" .");

        return new TranslatedSegment(code.toString(), usedVars);
    }


    @Override
    public TranslatedSegment visit(Minus minus)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }


    @Override
    public TranslatedSegment visit(Optional optional)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }

    @Override
    public TranslatedSegment visit(Bind bind)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }

    @Override
    public TranslatedSegment visit(Filter filter)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }

    //-------------------------------------------------------------------------


    /**
     * Builds the SELECT query as SPARQL when we already have translated WHERE clause.
     *
     * @param select Details of the SELECT clause.
     * @param translatedWhereClause Translated WHERE clause of the query.
     * @param isTopLevel Is this SPARQL SELECT the top-level one?.
     * @return Translated SELECT pattern as SPARQL.
     */
    private TranslatedSegment translateSelect(Select select, TranslatedSegment translatedWhereClause,
            boolean isTopLevel)
    {
        if(translatedWhereClause == null)
            translatedWhereClause = new TranslatedSegment("\n{}");


        StringBuilder strBuf = new StringBuilder();
        LinkedHashSet<String> usedVars = new LinkedHashSet<>();
        LinkedHashSet<String> possibleUnboundVars = new LinkedHashSet<>();


        if(!isTopLevel)
        {
            translatedWhereClause.str = indentBlock(translatedWhereClause.str, 1);

            newLine(strBuf);
            strBuf.append("{");
            queryDepth++;
            newLine(strBuf);
        }

        strBuf.append("SELECT ");

        if(select.isDistinct())
            strBuf.append("DISTINCT ");
        else if(select.isReduced())
            strBuf.append("REDUCED ");


        if(select.getProjections().isEmpty())
        {
            if(!translatedWhereClause.subqueryVars.isEmpty())
            {
                for(String var : translatedWhereClause.subqueryVars)
                {
                    // filter out auxiliary/artificial variables
                    if(!queryVariables.contains(var))
                        continue;

                    strBuf.append("?");
                    strBuf.append(var);
                    strBuf.append(" ");

                    usedVars.add(var);

                    if(translatedWhereClause.possibleUnboundVars.contains(var))
                        possibleUnboundVars.add(var);
                }
            }

            if(usedVars.isEmpty())
                strBuf.append("(1 as ?" + getNewFakeStarName() + ")");
        }
        else
        {
            for(Projection proj : select.getProjections())
            {
                String var = proj.getVariable().getName();

                usedVars.add(var);

                if(translatedWhereClause.possibleUnboundVars.contains(var) || proj.getExpression() != null)
                    possibleUnboundVars.add(var);

                if(!translatedWhereClause.subqueryVars.contains(var) && proj.getExpression() == null)
                {
                    warnings.add(new TranslateException(ErrorType.unusedVariable, proj.getVariable().getRange(), var));
                    possibleUnboundVars.add(var);
                }
            }

            strBuf.append(convertStringableListToString(select.getProjections(), " "));
        }

        if(!select.getDataSets().isEmpty())
        {
            newLine(strBuf);
            strBuf.append(convertStringableListToString(select.getDataSets(), "\n"));
        }

        newLine(strBuf);
        strBuf.append("WHERE");

        strBuf.append(indentBlock(translatedWhereClause.str, 1));


        if(!select.getGroupByConditions().isEmpty())
        {
            newLine(strBuf);
            strBuf.append("GROUP BY ");

            boolean first = true;

            for(GroupCondition groupByCond : select.getGroupByConditions())
            {
                if(!first)
                    strBuf.append(" ");
                else
                    first = false;

                if(groupByCond.getVariable() == null)
                    strBuf.append(groupByCond.getExpression().toString());
                else
                    strBuf.append(groupByCond.getVariable().toString());
            }
        }

        if(!select.getHavingConditions().isEmpty())
        {
            //NOTE: due to the virtuoso bug https://github.com/openlink/virtuoso-opensource/issues/432
            if(select.getGroupByConditions().isEmpty())
            {
                newLine(strBuf);
                strBuf.append("GROUP BY ");
                strBuf.append(getNewTmpVarName());
            }

            newLine(strBuf);
            strBuf.append("HAVING ");
            strBuf.append(convertStringableListToString(select.getHavingConditions(), " "));
        }

        if(!select.getOrderByConditions().isEmpty())
        {
            newLine(strBuf);
            strBuf.append("ORDER BY ");
            strBuf.append(convertStringableListToString(select.getOrderByConditions(), " "));
        }

        if(select.getLimit() != null)
        {
            newLine(strBuf);
            strBuf.append("LIMIT ");
            strBuf.append(select.getLimit().toString());
        }

        if(select.getOffset() != null)
        {
            newLine(strBuf);
            strBuf.append("OFFSET ");
            strBuf.append(select.getOffset().toString());
        }

        if(select.getValues() != null)
        {
            // NOTE: tailing VALUES are not probably proccessed correctly in virtuoso:
            // https://github.com/openlink/virtuoso-opensource/issues/430
            for(Variable var : select.getValues().getVariables())
                usedVars.add(var.getName());

            for(ValuesList valueList : select.getValues().getValuesLists())
                for(int i = 0; i < select.getValues().getVariables().size(); i++)
                    if(valueList.getValues().get(i) == null)
                        possibleUnboundVars.add(select.getValues().getVariables().get(i).getName());

            newLine(strBuf);
            strBuf.append(select.getValues().toString());
        }

        if(!isTopLevel)
        {
            queryDepth--;
            newLine(strBuf);
            strBuf.append("}");
        }

        return new TranslatedSegment(strBuf.toString(), usedVars, possibleUnboundVars);
    }


    /**
     * Translates inner procedure call. Only a group preceding the procedure call is used as a context for the input
     * parameters. If no variables are used as input parameters, then the context is not used. Also, the variable input
     * parameters cannot be unbounded variables. If the result node is IRI, the translation is done with a temporary
     * variable and then filtered.
     *
     * @param procedureCall Details of the procedure call.
     * @param procedureCallContext Context preceding the procedure call (while still in the current block),
     * @param wholeContextInSparql true if the whole preceding context is of type SPARQL.
     * @return Translated inner procedure call.
     */
    private TranslatedSegment translateProcedureCall(ProcedureCallBase procedureCallBase,
            TranslatedSegment procedureCallContext)
    {
        IRI procedureName = procedureCallBase.getProcedure();
        ProcedureDefinition currProcConfig = proceduresConfig.getProcedureByName(procedureName.toString());


        for(GraphOrServiceRestriction restriction : graphOrServiceRestrictions)
        {
            if(restriction.isRestrictionType(GraphOrServiceRestriction.RestrictionType.SERVICE_RESTRICTION))
            {
                exceptions.add(new TranslateException(ErrorType.procedureCallInsideService,
                        procedureCallBase.getProcedure().getRange()));

                break;
            }
        }

        if(!graphOrServiceRestrictions.isEmpty())
        {
            GraphOrServiceRestriction restriction = graphOrServiceRestrictions.peek();

            if(restriction.getVarOrIri() instanceof IRI)
            {
                IRI iri = (IRI) restriction.getVarOrIri();

                if(!iri.equals(procedureName))
                    exceptions.add(new TranslateException(ErrorType.procedureCallInsideGraph,
                            procedureCallBase.getProcedure().getRange(), procedureName.toString(prologue),
                            iri.toString(prologue)));
            }
        }


        /*
         * check paramaters
         */

        // process parameters from the config
        ParameterCalls parameterCalls = new ParameterCalls();

        for(ParameterDefinition param : currProcConfig.getParameters())
        {
            String paramName = param.getParamName();
            String defaultVal = param.getDefaultValue();
            String typeIRI = param.getTypeIRI();

            if(defaultVal.isEmpty())
                defaultVal = null;

            parameterCalls.getArr().add(new ParameterCall(paramName, null, typeIRI, defaultVal));
        }


        // process parameters from the actual procedure calling
        HashSet<IRI> usedIRIs = new HashSet<IRI>();

        for(ProcedureCall.Parameter par : procedureCallBase.getParameters())
        {
            String parName = par.getName().toString();
            Node paramValue = par.getValue();


            if(usedIRIs.contains(par.getName()))
                exceptions.add(new TranslateException(ErrorType.repeatOfParameterPredicate, par.getName().getRange(),
                        par.getName().toString(prologue)));
            usedIRIs.add(par.getName());


            ParameterCall parameterCall = parameterCalls.find(parName);
            if(parameterCall == null)
            {
                exceptions.add(new TranslateException(ErrorType.invalidParameterPredicate, par.getName().getRange(),
                        par.getName().toString(prologue), procedureCallBase.getProcedure().toString(prologue)));
            }

            if(paramValue instanceof Variable || paramValue instanceof BlankNode)
            {
                String varName;
                if(paramValue instanceof Variable)
                    varName = ((Variable) paramValue).getName();
                else
                    varName = getTmpVarNameForBlankNode(((BlankNode) paramValue).getName());

                if(parameterCall != null)
                    parameterCall.setVar(varName);
            }
            else if(paramValue instanceof Literal || paramValue instanceof IRI)
            {
                //FIXME: Only for non - stored mode
                //String value = translateLiteralNodeExpression((Expression) paramValue,
                //        EnumSet.of(ExpressionTranslateFlag.SUPPRESS_LITERAL_CONVERSION));
                String value = ((Expression) paramValue).toString();

                if(parameterCall != null)
                    parameterCall.setLiteralVal(value);
            }
        }


        for(int i = 0; i < parameterCalls.getArr().size(); ++i)
        {
            ParameterCall paramCall = parameterCalls.getArr().get(i);

            if(paramCall.getVar() == null && paramCall.getLiteralVal() == null)
            {
                exceptions.add(new TranslateException(ErrorType.missingParameterPredicate,
                        procedureCallBase.getProcedure().getRange(), new IRI(paramCall.getParName()).toString(prologue),
                        procedureCallBase.getProcedure().toString(prologue)));
            }
        }


        // check whether variables used in parameters are in the actual context (unbounded variables are not permited)
        LinkedHashSet<String> contextVars = procedureCallContext == null ? null : procedureCallContext.subqueryVars;

        for(ProcedureCall.Parameter par : procedureCallBase.getParameters())
        {
            if(par.getValue() instanceof Variable)
            {
                String parameterVar = ((Variable) par.getValue()).getName();

                if(contextVars == null || !contextVars.contains(parameterVar))
                {
                    exceptions.add(new TranslateException(ErrorType.unboundedVariableParameterValue,
                            par.getValue().getRange(), par.getName().toString(prologue), parameterVar));
                }
            }
            else if(par.getValue() instanceof BlankNode)
            {
                String parameterVar = blankNodesTmpVarHashes.get(((BlankNode) par.getValue()).getName());

                if(contextVars == null || !contextVars.contains(parameterVar))
                {
                    exceptions.add(new TranslateException(ErrorType.unboundedBlankNodeParameterValue,
                            par.getValue().getRange(), par.getName().toString(prologue)));
                }
            }
        }



        /*
         * check results
         */
        List<String> resultVarList = new ArrayList<>();
        List<Pair<String, Node>> varsToFilter = new ArrayList<>();
        LinkedHashSet<String> possibleUnboundVars = new LinkedHashSet<>();
        LinkedHashSet<String> usedVars = new LinkedHashSet<>();

        if(procedureCallBase instanceof ProcedureCall)
        {
            /*
             * single-result procedure call
             */
            ProcedureCall procedureCall = (ProcedureCall) procedureCallBase;
            Node resultNode = procedureCall.getResult();


            String resultVarName;

            if(resultNode instanceof Variable)
            {
                resultVarName = ((Variable) resultNode).getName();
                usedVars.add(resultVarName);
            }
            else if(resultNode instanceof BlankNode)
            {
                resultVarName = getTmpVarNameForBlankNode(((BlankNode) resultNode).getName());
                usedVars.add(resultVarName);
            }
            else
            {
                resultVarName = getNewTmpVarName();
                varsToFilter.add(new Pair<>(resultVarName, resultNode));
            }

            resultVarList.add(resultVarName);
        }
        else
        {
            /*
             * multi-result procedure call
             */
            MultiProcedureCall multiProcedureCall = (MultiProcedureCall) procedureCallBase;

            if(multiProcedureCall.getResults().isEmpty())
            {
                // FIXME: use result blank node range
                exceptions.add(new TranslateException(ErrorType.noResultParameter,
                        procedureCallBase.getProcedure().getRange()));
            }
            else
            {
                HashSet<IRI> usedResultIRIs = new HashSet<IRI>();
                HashSet<String> usedVariables = new HashSet<String>();

                for(Parameter par : multiProcedureCall.getResults())
                {
                    if(usedResultIRIs.contains(par.getName()))
                        exceptions.add(new TranslateException(ErrorType.repeatOfResultPredicate,
                                par.getName().getRange(), par.getName().toString(prologue)));

                    usedResultIRIs.add(par.getName());


                    if(par.getValue() instanceof Variable)
                    {
                        Variable var = (Variable) par.getValue();

                        // TODO: can be valid in a new version
                        if(usedVariables.contains(var.getName()))
                            exceptions.add(new TranslateException(ErrorType.repeatOfResultVariable, var.getRange(),
                                    var.getName()));

                        usedVariables.add(var.getName());
                    }
                }
            }


            HashMap<String, Node> actualResultCalls = new HashMap<>();

            for(Parameter par : multiProcedureCall.getResults())
                actualResultCalls.put(par.getName().toString(), par.getValue());


            for(ResultDefinition resDef : currProcConfig.getResults())
            {
                String resultParamName = resDef.getParamName();
                Node resultNode = actualResultCalls.get(resultParamName);
                actualResultCalls.remove(resultParamName);

                String resultVarName = null;

                if(resultNode != null)
                {
                    if(resultNode instanceof Variable)
                    {
                        resultVarName = ((Variable) resultNode).getName();
                        usedVars.add(resultVarName);

                        if(resDef.isCanBeNull())
                            possibleUnboundVars.add(resultVarName);
                    }
                    else if(resultNode instanceof BlankNode)
                    {
                        resultVarName = getTmpVarNameForBlankNode(((BlankNode) resultNode).getName());
                        usedVars.add(resultVarName);

                        if(resDef.isCanBeNull())
                            possibleUnboundVars.add(resultVarName);
                    }
                    else
                    {
                        resultVarName = getNewTmpVarName();
                        varsToFilter.add(new Pair<>(resultVarName, resultNode));
                    }
                }

                resultVarList.add(resultVarName);
            }


            if(!currProcConfig.getResults().isEmpty()) // report only if the procedure is configured as a multi-result one.
            {
                HashSet<String> names = new HashSet<>();

                for(ResultDefinition resDef : currProcConfig.getResults())
                    names.add(resDef.getParamName());

                for(Parameter par : ((MultiProcedureCall) procedureCallBase).getResults())
                    if(!names.contains(par.getName().toString()))
                        exceptions
                                .add(new TranslateException(ErrorType.invalidResultPredicate, par.getName().getRange(),
                                        procedureName.toString(prologue), par.getName().toString(prologue)));
            }
        }


        TranslatedSegment originalContext = procedureCallContext;


        /*
         * Translate 1st part
         */
        LinkedHashSet<String> parameterVars = new LinkedHashSet<String>();

        for(ParameterCall par : parameterCalls.getArr())
            if(par.getVar() != null)
                parameterVars.add(par.getVar());


        if(procedureCallContext == null)
            procedureCallContext = new TranslatedSegment(""); // fake context


        TranslatedSegment fakeCallTranslation = new TranslatedSegment(null, usedVars, possibleUnboundVars);


        StringBuilder strBuf = new StringBuilder();
        strBuf.append("sparql define input:storage virtrdf:PubchemQuadStorage ");

        for(Define def : prologue.getDefines())
        {
            strBuf.append(" ");
            strBuf.append(def.toString());
        }

        strBuf.append("\n");

        String countVar = getNewTmpVarName();

        strBuf.append("SELECT (sum(?").append(countVar).append(") as ?SUM)  WHERE\n{\n");
        strBuf.append(indentBlock(procedureCallContext.str, 1));

        strBuf.append("\n  BIND (");

        strBuf.append("sql:").append(currProcConfig.getMappedProcName()).append("_store(");

        strBuf.append(graphID);

        for(int i = 0; i < parameterCalls.getArr().size(); ++i)
        {
            ParameterCall par = parameterCalls.getArr().get(i);

            strBuf.append(", ");

            if(par.getVar() != null)
                strBuf.append('?').append(par.getVar());
            else
                strBuf.append(par.getLiteralVal());
        }


        strBuf.append(") AS ?").append(countVar).append(")\n}\n");

        codes.add(strBuf.toString());


        /*
         * Translate 2nd part
         */

        StringBuilder strBuf2 = new StringBuilder();
        String callVar = getNewTmpVarName();

        String graph = "http://bioinfo.iocb.cz/rdf/0.9/procedure-calls#" + currProcConfig.getMappedProcName();

        strBuf2.append("\n");
        strBuf2.append("GRAPH <").append(graph).append(">\n");
        strBuf2.append("{\n");


        strBuf2.append("  ?").append(callVar).append("\n");
        strBuf2.append("    <http://bioinfo.iocb.cz/rdf/0.9/procedure-calls#context> ").append(graphID);


        LinkedHashMap<String, String> filterMap = new LinkedHashMap<String, String>();

        for(ProcedureCall.Parameter par : procedureCallBase.getParameters())
        {
            if(par.getValue() instanceof Variable || par.getValue() instanceof BlankNode)
            {
                strBuf2.append(";\n    ").append(par.getName().toString());
                strBuf2.append(" ");

                String tmpVar = getNewTmpVarName();
                strBuf2.append('?').append(tmpVar);

                if(par.getValue() instanceof BlankNode)
                    filterMap.put(tmpVar, "?" + getTmpVarNameForBlankNode(((BlankNode) par.getValue()).getName()));
                else
                    filterMap.put(tmpVar, par.getValue().toString());
            }
        }


        strBuf2.append(" .\n");
        strBuf2.append("}\n");

        for(Entry<String, String> pair : filterMap.entrySet())
        {
            strBuf2.append("filter(");
            strBuf2.append("str(?");
            strBuf2.append(pair.getKey());
            strBuf2.append(") = str(");
            strBuf2.append(pair.getValue());

            strBuf2.append("))\n");
        }



        /* result graph */
        strBuf2.append("\n");
        strBuf2.append("GRAPH <").append(graph).append(">\n");
        strBuf2.append("{\n");


        strBuf2.append("  ?").append(getNewTmpVarName()).append("\n");
        strBuf2.append("    <http://bioinfo.iocb.cz/rdf/0.9/procedure-calls#call> ?").append(callVar);

        if(procedureCallBase instanceof ProcedureCall)
        {
            strBuf2.append(";\n");
            strBuf2.append("    <http://bioinfo.iocb.cz/rdf/0.9/procedure-calls#result> ");
            ProcedureCall procedureCall = (ProcedureCall) procedureCallBase;
            strBuf2.append(" ").append(procedureCall.getResult().toString());
            strBuf2.append(" .\n");
        }
        else
        {
            MultiProcedureCall multiProcedureCall = (MultiProcedureCall) procedureCallBase;

            for(Parameter par : multiProcedureCall.getResults())
            {
                if(par.getValue() != null)
                {
                    strBuf2.append(";\n    ");
                    strBuf2.append(par.getName().toString());
                    strBuf2.append(" ");
                    strBuf2.append(par.getValue().toString());
                }
            }

            strBuf2.append(" .\n");
        }

        strBuf2.append("}\n");

        TranslatedSegment procedureGraph = new TranslatedSegment(strBuf2.toString(), fakeCallTranslation.subqueryVars,
                fakeCallTranslation.possibleUnboundVars);

        TranslatedSegment translatedSegment = translateJoin(originalContext, procedureGraph);

        graphID++;

        return translatedSegment;
    }

    private static int graphID = 0;

    private TranslatedSegment translatePatternList(List<Pattern> patterns)
    {
        TranslatedSegment translatedGroupGraphPattern = null;
        TranslatedSegment translatedTripletsPatterns = null;

        LinkedList<Filter> filters = new LinkedList<>();


        for(Pattern pattern : patterns)
        {
            if(!(pattern instanceof Triple))
            {
                translatedGroupGraphPattern = translateJoin(translatedGroupGraphPattern, translatedTripletsPatterns);
                translatedTripletsPatterns = null;
            }


            if(pattern instanceof Triple)
            {
                TranslatedSegment translatedPattern = visitElement(pattern);
                translatedTripletsPatterns = translateJoin(translatedTripletsPatterns, translatedPattern);
            }
            else if(pattern instanceof Optional)
            {
                GraphPattern optionalPat = ((Optional) pattern).getPattern();

                TranslatedSegment translatedPattern = null;
                LinkedList<Filter> optionalFilters = new LinkedList<>();

                if(optionalPat != null)
                {
                    if(optionalPat instanceof GroupGraph)
                    {
                        LinkedList<Pattern> optionalPatterns = new LinkedList<Pattern>();

                        for(Pattern subpattern : ((GroupGraph) optionalPat).getPatterns())
                        {
                            if(subpattern instanceof Filter)
                                optionalFilters.add((Filter) subpattern);
                            else
                                optionalPatterns.add(subpattern);
                        }

                        translatedPattern = translatePatternList(optionalPatterns);
                    }
                    else
                    {
                        translatedPattern = optionalPat.accept(this);
                    }
                }

                translatedGroupGraphPattern = translateLeftJoin(translatedGroupGraphPattern, translatedPattern,
                        optionalFilters);
            }
            else if(pattern instanceof Minus)
            {
                translatedGroupGraphPattern = translateMinus((Minus) pattern, translatedGroupGraphPattern);
            }
            else if(pattern instanceof Bind)
            {
                translatedGroupGraphPattern = translateBind((Bind) pattern, translatedGroupGraphPattern);
            }
            else if(pattern instanceof Filter)
            {
                filters.add((Filter) pattern);
            }
            else if(pattern instanceof ProcedureCallBase)
            {
                translatedGroupGraphPattern = translateProcedureCall((ProcedureCallBase) pattern,
                        translatedGroupGraphPattern);
            }
            else
            {
                TranslatedSegment translatedPattern = visitElement(pattern);
                translatedGroupGraphPattern = translateJoin(translatedGroupGraphPattern, translatedPattern);
            }
        }

        translatedGroupGraphPattern = translateJoin(translatedGroupGraphPattern, translatedTripletsPatterns);

        return translateFilters(filters, translatedGroupGraphPattern);
    }


    /**
     * Processes the FILTER clauses.
     *
     * @param prevTranslatedPattern Previously translated segment of patterns.
     * @param filters List of FILTER patterns.
     * @return Translated segment with FILTER claused processed.
     */
    private TranslatedSegment translateFilters(List<Filter> filters, TranslatedSegment prevTranslatedPattern)
    {
        // if the filtered block is empty, there is no point to filter something
        if(filters.isEmpty() || prevTranslatedPattern == null)
            return prevTranslatedPattern;


        for(Filter filter : filters)
            checkExpressionForUnGroupedSolutions(filter.getConstraint());


        StringBuilder strBuf = new StringBuilder();
        strBuf.append(prevTranslatedPattern.str);

        for(Filter filter : filters)
        {
            strBuf.append("\n");
            strBuf.append(filter.toString());
        }

        TranslatedSegment result = new TranslatedSegment(strBuf.toString(), prevTranslatedPattern.subqueryVars,
                prevTranslatedPattern.possibleUnboundVars);

        return result;
    }


    /**
     * Processes the BIND clause.
     *
     * @param prevTranslatedPattern Previously translated segment of patterns.
     * @param bindPattern BIND pattern to be processed.
     * @return Translated segment with BIND clause processed.
     */
    private TranslatedSegment translateBind(Bind bindPat, TranslatedSegment prevTranslatedPattern)
    {
        String varName = bindPat.getVariable().getName();

        LinkedHashSet<String> usedVariables = new LinkedHashSet<>();
        LinkedHashSet<String> possibleUnboundVars = new LinkedHashSet<>();

        if(prevTranslatedPattern != null)
        {
            usedVariables.addAll(prevTranslatedPattern.subqueryVars);
            possibleUnboundVars.addAll(prevTranslatedPattern.possibleUnboundVars);
        }

        if(usedVariables.contains(varName))
            exceptions.add(new TranslateException(ErrorType.variableUsedBeforeBind, bindPat.getVariable().getRange(),
                    varName));

        checkExpressionForUnGroupedSolutions(bindPat.getExpression());


        usedVariables.add(varName);
        possibleUnboundVars.add(varName);



        StringBuilder strBuf = new StringBuilder();

        if(prevTranslatedPattern != null)
            strBuf.append(prevTranslatedPattern.str);

        strBuf.append("\n" + bindPat.toString());

        return new TranslatedSegment(strBuf.toString(), usedVariables, possibleUnboundVars);
    }

    /**
     * Processes the BIND clauses implicitly used in GROUP BY clause.
     *
     */
    private TranslatedSegment translateBindInGroupBy(List<GroupCondition> groupByList,
            TranslatedSegment translatedWhereClause)
    {
        for(GroupCondition groupBy : groupByList)
        {
            if(groupBy.getVariable() != null)
            {
                Bind bind = new Bind(groupBy.getExpression(), groupBy.getVariable());
                bind.setRange(groupBy.getRange());

                translatedWhereClause = translateBind(bind, translatedWhereClause);
            }

            if(groupBy.getExpression() != null)
                checkExpressionForUnGroupedSolutions(groupBy.getExpression());
        }

        return translatedWhereClause;
    }

    //-------------------------------------------------------------------------


    /**
     * Joins two translated patterns.
     *
     * @param prevTranslatedPattern The first pattern to be joined.
     * @param translatedPatterns The second pattern to be joined.
     * @return Joined pattern.
     */
    private TranslatedSegment translateJoin(TranslatedSegment prevTranslatedPattern,
            TranslatedSegment translatedPattern)
    {
        if(prevTranslatedPattern == null)
            return translatedPattern;

        if(translatedPattern == null)
            return prevTranslatedPattern;


        LinkedHashSet<String> allUsedVars = new LinkedHashSet<>();
        allUsedVars.addAll(prevTranslatedPattern.subqueryVars);
        allUsedVars.addAll(translatedPattern.subqueryVars);

        LinkedHashSet<String> allPossibleUnboundVars = new LinkedHashSet<>();
        allPossibleUnboundVars.addAll(prevTranslatedPattern.possibleUnboundVars);
        allPossibleUnboundVars.addAll(translatedPattern.possibleUnboundVars);


        StringBuilder strBuf = new StringBuilder();

        strBuf.append(prevTranslatedPattern.str);
        strBuf.append(translatedPattern.str);

        return new TranslatedSegment(strBuf.toString(), allUsedVars, allPossibleUnboundVars);
    }


    /**
     * Left-joins two translated patterns.
     *
     */
    private TranslatedSegment translateLeftJoin(TranslatedSegment prevTranslatedPattern,
            TranslatedSegment translatedPattern, List<Filter> optionalFilters)
    {
        if(translatedPattern == null && optionalFilters.isEmpty())
            return prevTranslatedPattern;


        for(Filter filter : optionalFilters)
            checkExpressionForUnGroupedSolutions(filter.getConstraint());


        LinkedHashSet<String> allUsedVars = new LinkedHashSet<>();
        LinkedHashSet<String> allPossibleUnboundVars = new LinkedHashSet<>();

        if(prevTranslatedPattern != null)
        {
            allUsedVars.addAll(prevTranslatedPattern.subqueryVars);
            allPossibleUnboundVars.addAll(prevTranslatedPattern.possibleUnboundVars);
        }

        if(translatedPattern != null)
        {
            allUsedVars.addAll(translatedPattern.subqueryVars);
            allPossibleUnboundVars.addAll(translatedPattern.possibleUnboundVars);

            if(prevTranslatedPattern != null)
            {
                for(String var : translatedPattern.subqueryVars)
                    if(!prevTranslatedPattern.subqueryVars.contains(var))
                        allPossibleUnboundVars.add(var);
            }
            else
            {
                allPossibleUnboundVars.addAll(translatedPattern.subqueryVars);
            }
        }



        StringBuilder strBuf = new StringBuilder();



        if(prevTranslatedPattern != null)
            strBuf.append(prevTranslatedPattern.str);

        strBuf.append("\nOPTIONAL\n{");

        if(translatedPattern != null)
            strBuf.append(indentBlock(translatedPattern.str, 1));

        for(Filter filter : optionalFilters)
        {
            strBuf.append("\n");
            strBuf.append(indentBlock(filter.toString(), 1));
        }

        strBuf.append("\n}");

        return new TranslatedSegment(strBuf.toString(), allUsedVars, allPossibleUnboundVars);
    }


    /**
     * Minus two translated patterns.
     */
    private TranslatedSegment translateMinus(Minus minus, TranslatedSegment prevTranslatedPattern)
    {
        TranslatedSegment translatedPattern = null;

        if(minus.getPattern() != null)
            translatedPattern = visitElement(minus.getPattern());


        HashSet<String> intersection = new HashSet<String>();

        if(prevTranslatedPattern != null)
            intersection.addAll(prevTranslatedPattern.subqueryVars);

        if(translatedPattern != null)
            intersection.retainAll(translatedPattern.subqueryVars);

        if(intersection.isEmpty())
            // FIXME: use keywork range
            warnings.add(new TranslateException(ErrorType.unnecessaryMinus, minus.getRange()));


        if(prevTranslatedPattern == null)
            return null;

        if(translatedPattern == null)
            return prevTranslatedPattern;


        StringBuilder strBuf = new StringBuilder();


        strBuf.append(prevTranslatedPattern.str);
        strBuf.append("\n");
        strBuf.append("MINUS");
        strBuf.append(translatedPattern.str);

        return new TranslatedSegment(strBuf.toString(), prevTranslatedPattern.subqueryVars,
                prevTranslatedPattern.possibleUnboundVars);
    }

    //-------------------------------------------------------------------------


    /**
     * Eliminates blank-nodes from the given triple - replace them with variables (they are handled equally according to
     * SPARQL specification).
     *
     * @param triple SPARQL triple pattern.
     * @return Modified triple without blank-nodes.
     */
    private Triple eliminateBlankNodesFromTriple(Triple triple)
    {
        if(!(triple.getSubject() instanceof BlankNode) && !(triple.getObject() instanceof BlankNode))
            return triple;


        Node subject = triple.getSubject();
        Node object = triple.getObject();

        if(subject instanceof BlankNode)
        {
            String tmpVarName = getTmpVarNameForBlankNode(((BlankNode) subject).getName());
            Variable var = new Variable(tmpVarName);
            var.setNodeOptions(subject.getNodeOptions());
            var.setRange(subject.getRange());
            subject = var;
        }

        if(object instanceof BlankNode)
        {
            String tmpVarName = getTmpVarNameForBlankNode(((BlankNode) object).getName());
            Variable var = new Variable(tmpVarName);
            var.setNodeOptions(object.getNodeOptions());
            var.setRange(object.getRange());
            object = var;
        }

        Triple newTriple = new Triple(subject, triple.getPredicate(), object);
        newTriple.setRange(triple.getRange());

        return newTriple;
    }


    /**
     * Collects variables from the triple.
     *
     * @param triple SPARQL triple.
     * @return List of variables that are present in the SPARQL triple.
     */
    private LinkedHashSet<String> extractVariblesFromTriple(Triple triple)
    {
        LinkedHashSet<String> varsList = new LinkedHashSet<>();

        Node subj = triple.getSubject();
        if(subj instanceof Variable)
        {
            varsList.add(((Variable) subj).getName());
        }

        Verb pred = triple.getPredicate();
        if(pred instanceof Variable)
        {
            varsList.add(((Variable) pred).getName());
        }

        Node obj = triple.getObject();
        if(obj instanceof Variable)
        {
            varsList.add(((Variable) obj).getName());
        }

        return varsList;
    }

    //-------------------------------------------------------------------------


    private void checkExpressionForGroupedSolutions(Expression expresion, HashSet<String> groupByVars)
    {
        new ElementVisitor<Void>()
        {
            @Override
            public Void visit(BuiltInCallExpression func)
            {
                if(!isAggregateFunction(func.getFunctionName()) || func.getArguments().isEmpty())
                    return null;

                Expression arg = func.getArguments().get(0);

                if(!(arg instanceof Variable))
                    return null;

                if(groupByVars.contains(((Variable) arg).getName()))
                    exceptions.add(new TranslateException(ErrorType.invalidVariableInAggregate, arg.getRange(),
                            arg.toString()));

                return null;
            }

            @Override
            public Void visit(Variable var)
            {
                if(!groupByVars.contains(var.getName()))
                    exceptions.add(new TranslateException(ErrorType.invalidVariableOutsideAggregate, var.getRange(),
                            var.toString()));

                return null;
            }

            @Override
            public Void visit(GroupGraph expr)
            {
                return null;
            }

            @Override
            public Void visit(Select expr)
            {
                return null;
            }
        }.visitElement(expresion);
    }

    private void checkExpressionForUnGroupedSolutions(Expression expresion)
    {
        new ElementVisitor<Void>()
        {
            @Override
            public Void visit(BuiltInCallExpression call)
            {
                if(isAggregateFunction(call.getFunctionName()))
                    exceptions.add(new TranslateException(ErrorType.invalidContextOfAggregate, call.getRange()));

                return null;
            }

            @Override
            public Void visit(GroupGraph expr)
            {
                return null;
            }

            @Override
            public Void visit(Select expr)
            {
                return null;
            }
        }.visitElement(expresion);
    }



    private boolean containsAggregateFunction(Expression expression)
    {
        if(expression == null)
            return false;

        Boolean ret = new ElementVisitor<Boolean>()
        {
            @Override
            public Boolean visit(BuiltInCallExpression call)
            {
                return isAggregateFunction(call.getFunctionName());
            }

            @Override
            public Boolean visit(GroupGraph expr)
            {
                return false;
            }

            @Override
            public Boolean visit(Select expr)
            {
                return false;
            }

            @Override
            protected Boolean aggregateResult(List<Boolean> results)
            {
                return results.stream().anyMatch(x -> (x != null && x));
            }

        }.visitElement(expression);

        return ret != null && ret;
    }


    private boolean isInAggregateMode(Select select)
    {
        // check whether aggregateMode is explicit
        if(!select.getGroupByConditions().isEmpty() || !select.getHavingConditions().isEmpty())
            return true;

        // check whether aggregateMode is implicit
        for(Projection projection : select.getProjections())
            if(projection.getExpression() != null && containsAggregateFunction(projection.getExpression()))
                return true;

        for(OrderCondition orderCond : select.getOrderByConditions())
            if(containsAggregateFunction(orderCond.getExpression()))
                return true;

        return false;
    }



    //-------------------------------------------------------------------------

    private void checkProjectionVariables(Select select)
    {
        if(!select.getGroupByConditions().isEmpty() && select.getProjections().isEmpty())
            exceptions.add(new TranslateException(ErrorType.invalidProjection, select.getRange()));

        if(!isInAggregateMode(select))
            return;

        HashSet<String> groupVars = new HashSet<>();

        for(GroupCondition groupCond : select.getGroupByConditions())
        {
            if(groupCond.getVariable() != null)
                groupVars.add(groupCond.getVariable().getName());
            else if(groupCond.getExpression() instanceof Variable)
                groupVars.add(((Variable) groupCond.getExpression()).getName());
        }

        for(Expression havingCond : select.getHavingConditions())
            checkExpressionForGroupedSolutions(havingCond, groupVars);

        HashSet<String> vars = new HashSet<>();
        for(Projection projection : select.getProjections())
        {
            if(vars.contains(projection.getVariable().getName()))
                exceptions.add(new TranslateException(ErrorType.repeatOfProjectionVariable,
                        projection.getVariable().getRange(), projection.getVariable().toString()));

            vars.add(projection.getVariable().getName());

            if(projection.getExpression() != null)
                checkExpressionForGroupedSolutions(projection.getExpression(), groupVars);
            else
                checkExpressionForGroupedSolutions(projection.getVariable(), groupVars);
        }

        for(OrderCondition orderCond : select.getOrderByConditions())
            checkExpressionForGroupedSolutions(orderCond.getExpression(), groupVars);
    }


    /**
     * Checks whether the given function is an aggregate function (according to SPARQL 1.1).
     *
     * @param functionName Name of the SPARQL function.
     * @return true if the given function is an aggregate function.
     */
    private boolean isAggregateFunction(String functionName)
    {
        functionName = functionName.toUpperCase(Locale.US);

        switch(functionName)
        {
            // aggregate functions according to SPARQL 1.1
            case "COUNT":
            case "SUM":
            case "MIN":
            case "MAX":
            case "AVG":
            case "GROUP_CONCAT":
            case "SAMPLE":
                return true;
        }

        return false;
    }

    //-------------------------------------------------------------------------


    /**
     * Gets a new temporary variable name.
     *
     * @return New temp variable name.
     */
    private String getNewTmpVarName()
    {
        while(true)
        {
            String name = tempVarPrefix + (uniqueVarID <= 9 ? "0" : "") + Integer.toString(uniqueVarID++);

            if(!queryVariables.contains(name))
                return name;
        }
    }


    private String getNewFakeStarName()
    {
        while(true)
        {
            String name = tempStarFakePrefix + Integer.toString(uniqueStarFakeID++);

            if(!queryVariables.contains(name))
                return name;
        }
    }


    /**
     * Gets (new) temporary variable name used as a replacement for a blank node.
     *
     * @return Replacement variable.
     */
    private String getTmpVarNameForBlankNode(String blankNodeName)
    {
        String mappedVarName = blankNodesTmpVarHashes.get(blankNodeName);

        if(mappedVarName == null)
        {
            mappedVarName = getNewTmpVarName();
            blankNodesTmpVarHashes.put(blankNodeName, mappedVarName);
        }

        return mappedVarName;
    }

    //-------------------------------------------------------------------------


    /**
     * Builds a string from the list of stringable items.
     *
     * @param list Input list of items.
     * @param separatorStr Separator string.
     * @return String based on the input list.
     */
    private static <T> String convertStringableListToString(Iterable<T> list, String separatorStr)
    {
        StringBuilder strBuf = new StringBuilder();
        boolean first = true;

        for(T item : list)
        {
            if(!first)
                strBuf.append(separatorStr);
            else
                first = false;

            strBuf.append(item.toString());
        }

        return strBuf.toString();
    }


    /**
     * Adds a newline to the current string-builder object.
     *
     * @param strBuf Current string-builder object.
     */
    private void newLine(StringBuilder strBuf)
    {
        strBuf.append("\n");
        for(int i = 0; i < queryDepth * indentMultiplier; ++i)
        {
            strBuf.append(" ");
        }
    }


    /**
     * Indent the query block.
     *
     * @param block Input block.
     * @param depth Indentation depth.
     * @return Indented query block.
     */
    private String indentBlock(String block, int depth)
    {
        StringBuilder strBuf = new StringBuilder();

        for(int i = 0; i < indentMultiplier * depth; ++i)
        {
            strBuf.append(" ");
        }

        return block.replaceAll("\n", "\n" + strBuf.toString());
    }
}

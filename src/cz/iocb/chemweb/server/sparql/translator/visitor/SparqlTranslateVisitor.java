package cz.iocb.chemweb.server.sparql.translator.visitor;

import java.util.ArrayList;
import java.util.EnumSet;
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
import cz.iocb.chemweb.server.sparql.parser.model.expression.ExistsExpression;
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
import cz.iocb.chemweb.server.sparql.translator.visitor.expressions.ExpressionTranslateFlag;
import cz.iocb.chemweb.server.sparql.translator.visitor.expressions.ExpressionTranslator;



public class SparqlTranslateVisitor extends ElementVisitor<TranslatedSegment>
{
    private static final String tempStarFakePrefix = "_star_fake_";
    private static final String tempVarPrefix = "__tmpVar";
    private static final String tempTabPrefix = "__tmpTab";
    private static final String sqlSelectSuffix = "OPTION(QUIETCAST)";
    private static final boolean useCorrectJoin = true;
    private static final int indentMultiplier = 2;

    private boolean fullQueryDecomposition;
    private boolean topLevelSelect = true;
    private int queryDepth = 0;

    private int uniqueStarFakeID = 1;
    private int uniqueTabID = 1;
    private int uniqueVarID = 1;

    private Map<String, List<String>> propertyChains = null;
    private Map<String, String> propertyGraphs = null;

    private Config proceduresConfig = null;
    private Prologue prologue;
    private final Stack<GraphOrServiceRestriction> graphOrServiceRestrictions = new Stack<>();
    private final Stack<List<DataSet>> selectDatasets = new Stack<>();
    private HashSet<String> queryVariables;
    private final HashMap<String, String> blankNodesTmpVarHashes = new HashMap<>();
    private final List<TranslateException> exceptions = new LinkedList<TranslateException>();
    private final List<TranslateException> warnings = new LinkedList<TranslateException>();

    List<String> codes = new LinkedList<String>();



    public SparqlTranslateVisitor(Map<String, String> propertyGraphs, Map<String, List<String>> propertyChains)
    {
        this.propertyChains = propertyChains;
        this.propertyGraphs = propertyGraphs;
    }


    /**
     * Constructs the translator object.
     *
     * @param sparqlQuery SPARQL query.
     * @param fullQueryDecomposition Specify whether the result query should be consisting of minimal SPARQL chunks
     *            (otherwise, only necessary decomposition is used).
     * @return String of a translated query.
     * @throws cz.iocb.chemweb.server.sparql.translator.error.TranslateException
     */
    public List<String> translate(SelectQuery sparqlQuery, boolean fullQueryDecomposition) throws TranslateExceptions
    {
        return translate(sparqlQuery, new Config(), fullQueryDecomposition);
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
    public List<String> translate(SelectQuery sparqlQuery, Config proceduresConfig, boolean fullQueryDecomposition)
            throws TranslateExceptions
    {
        this.proceduresConfig = proceduresConfig;
        this.fullQueryDecomposition = fullQueryDecomposition;

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
        this.fullQueryDecomposition = fullQueryDecomposition;

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

        if(translatedSelectClause.isSparql)
        {
            newLine(strBuf);
            strBuf.append("sparql define input:storage virtrdf:PubchemQuadStorage");

            for(Define def : prologue.getDefines())
            {
                strBuf.append(" ");
                strBuf.append(def.toString());
            }

            newLine(strBuf);
        }

        strBuf.append(translatedSelectClause.str);

        return new TranslatedSegment(strBuf.toString(), translatedSelectClause.isSparql);
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

        // if the translated WHERE clause is of type SPARQL, then translate the
        // whole SELECT as SPARQL; otherwise translate the whole SELECT as SQL
        TranslatedSegment translatedSelect = null;


        if(!select.getGroupByConditions().isEmpty())
        {
            translatedWhereClause = translateBindInGroupBy(select.getGroupByConditions(), translatedWhereClause);

            if(translatedWhereClause != null && translatedWhereClause.isSparql)
            {
                StringBuilder strBuf = new StringBuilder();

                strBuf.append("\n{");
                strBuf.append(indentBlock(translatedWhereClause.str, 1));
                strBuf.append("\n}");

                translatedWhereClause.str = strBuf.toString();
            }
        }


        if((translatedWhereClause == null || translatedWhereClause.isSparql) && !fullQueryDecomposition)
            translatedSelect = translateSelectAsSparql(select, translatedWhereClause, isTopLevel);
        else
            translatedSelect = translateSelectAsSql(select, translatedWhereClause, isTopLevel);


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

        if(translatedGroupGraphPattern != null && translatedGroupGraphPattern.isSparql)
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


        /*
         *  SPARQL GRAPH
         */
        if(translatedPattern.isSparql)
        {
            StringBuilder strBuf = new StringBuilder();

            newLine(strBuf);
            strBuf.append("GRAPH ").append(varOrIri.toString());
            strBuf.append(translatedPattern.str);

            return new TranslatedSegment(strBuf.toString(), true, translatedPattern.subqueryVars,
                    translatedPattern.possibleUnboundVars);
        }


        /*
         *  SQL GRAPH
         */
        return translatedPattern;
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


        /*
         *  SPARQL SERVICE
         */
        if(translatedPattern.isSparql)
        {
            StringBuilder strBuf = new StringBuilder();
            newLine(strBuf);

            if(isSilent)
                strBuf.append("SERVICE SILENT ");
            else
                strBuf.append("SERVICE ");

            strBuf.append(varOrIri.toString());
            strBuf.append(translatedPattern.str);

            return new TranslatedSegment(strBuf.toString(), true, translatedPattern.subqueryVars,
                    translatedPattern.possibleUnboundVars);
        }


        /*
         *  SQL SERVICE
         */
        return translatedPattern;
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
        boolean areAllTranslatedPatternsSparql = true;
        List<TranslatedSegment> translatedPatterns = new ArrayList<>();
        LinkedHashSet<String> allUsedVars = new LinkedHashSet<>();
        LinkedHashSet<String> allPossibleUnboundVars = new LinkedHashSet<>();

        for(GraphPattern pattern : union.getPatterns())
        {
            TranslatedSegment translatedPattern = visitElement(pattern);

            if(translatedPattern == null)
                continue;

            areAllTranslatedPatternsSparql &= translatedPattern.isSparql;
            translatedPatterns.add(translatedPattern);
            allUsedVars.addAll(translatedPattern.subqueryVars);
            allPossibleUnboundVars.addAll(translatedPattern.possibleUnboundVars);
        }


        if(translatedPatterns.isEmpty())
            return null;

        if(translatedPatterns.size() == 1)
            return translatedPatterns.get(0);


        StringBuilder strBuf = new StringBuilder();


        /*
         * SPARQL UNION
         */
        if(areAllTranslatedPatternsSparql && !fullQueryDecomposition)
        {
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

            return new TranslatedSegment(strBuf.toString(), true, allUsedVars, allPossibleUnboundVars);
        }


        /*
         * SQL UNION
         */
        newLine(strBuf);
        strBuf.append("(");
        boolean firstPat = true;

        for(TranslatedSegment translatedPattern : translatedPatterns)
        {
            if(!firstPat)
            {
                newLine(strBuf);
                strBuf.append("UNION ALL");
            }
            firstPat = false;


            if(translatedPattern.isSparql)
            {
                translatedPattern = wrapAndNameTable(wrapSparqlSelect(translatedPattern));
                translatedPattern.str = indentBlock(translatedPattern.str, 1);
            }

            queryDepth++;
            newLine(strBuf);
            strBuf.append("SELECT ");

            boolean first = true;

            for(String var : allUsedVars)
            {
                if(!first)
                    strBuf.append(", ");
                else
                    first = false;

                if(!translatedPattern.subqueryVars.contains(var))
                    strBuf.append("NULL AS ");

                strBuf.append("\"").append(var).append("\"");
            }

            newLine(strBuf);
            strBuf.append("FROM");

            queryDepth++;

            newLine(strBuf);
            strBuf.append("(");
            queryDepth++;
            newLine(strBuf);
            strBuf.append("SELECT *");
            newLine(strBuf);
            strBuf.append("FROM");

            strBuf.append(indentBlock(translatedPattern.str, queryDepth));

            // OPTION(QUIETCAST)
            newLine(strBuf);
            strBuf.append(sqlSelectSuffix);

            queryDepth--;
            newLine(strBuf);
            strBuf.append(") AS ").append(getNewTmpTabName());
            queryDepth--;
            queryDepth--;
        }

        newLine(strBuf);
        strBuf.append(") AS ");
        String unionedTabName = getNewTmpTabName();
        strBuf.append(unionedTabName);

        return new TranslatedSegment(strBuf.toString(), false, allUsedVars, allPossibleUnboundVars, unionedTabName);
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
        /*
         * SPARQL VALUES
         */
        LinkedHashSet<String> usedVars = new LinkedHashSet<>();

        for(Variable var : values.getVariables())
            usedVars.add(var.getName());


        LinkedHashSet<String> possibleUnboundVars = new LinkedHashSet<>();

        for(ValuesList valueList : values.getValuesLists())
            for(int i = 0; i < values.getVariables().size(); i++)
                if(valueList.getValues().get(i) == null)
                    possibleUnboundVars.add(values.getVariables().get(i).getName());

        return new TranslatedSegment("\n" + values.toString(), true, usedVars, possibleUnboundVars);
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
        /*
         * SPARQL TRIPLE
         */
        triple = eliminateBlankNodesFromTriple(triple);

        LinkedHashSet<String> usedVars = extractVariblesFromTriple(triple);

        String graph = null;

        if(graphOrServiceRestrictions.isEmpty())
        {
            // TODO: make it more general ...
            Verb predicate = triple.getPredicate();

            if(predicate instanceof IRI)
            {
                String def = ((IRI) predicate).toString(prologue, true, false);
                graph = propertyGraphs != null ? propertyGraphs.get(def) : null;
            }
        }
        else
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

        if(graph != null)
            code.append("graph ").append(graph).append(" { ");

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

        if(graph != null)
            code.append("}");


        return new TranslatedSegment(code.toString(), true, usedVars);
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
    private TranslatedSegment translateSelectAsSparql(Select select, TranslatedSegment translatedWhereClause,
            boolean isTopLevel)
    {
        if(translatedWhereClause == null)
            translatedWhereClause = new TranslatedSegment("\n{}", true);


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

        return new TranslatedSegment(strBuf.toString(), true, usedVars, possibleUnboundVars);
    }


    /**
     * Builds the SELECT query as SQL when we already have translated WHERE clause.
     *
     * @param select Details of the SELECT clause.
     * @param translatedWhereClause Translated WHERE clause of the query.
     * @param isTopLevel Is this SPARQL SELECT the top-level one?.
     * @return Translated SELECT pattern as SQL.
     */
    private TranslatedSegment translateSelectAsSql(Select select, TranslatedSegment translatedWhereClause,
            boolean isTopLevel)
    {
        if(translatedWhereClause == null)
            translatedWhereClause = new TranslatedSegment(getDummyTableStr(), false);

        if(translatedWhereClause.isSparql)
        {
            translatedWhereClause = wrapAndNameTable(wrapSparqlSelect(translatedWhereClause));
            translatedWhereClause.str = indentBlock(translatedWhereClause.str, 1);
        }

        if(select.getValues() == null)
        {
            return translateSelectAsSqlNoValues(select, translatedWhereClause, isTopLevel);
        }
        else
        {
            Select innerSelect = new Select();
            Select outerSelect = new Select();

            List<Projection> innerProjections = innerSelect.getProjections();
            List<Projection> outerProjections = outerSelect.getProjections();

            for(Projection projection : select.getProjections())
            {
                if(projection.getExpression() != null)
                {
                    Variable var = new Variable(getNewTmpVarName());

                    innerProjections.add(new Projection(projection.getExpression(), var));
                    outerProjections.add(new Projection(var, projection.getVariable()));
                }
                else
                {
                    outerProjections.add(new Projection(projection.getVariable()));
                }
            }

            if(select.getGroupByConditions() != null)
            {
                for(GroupCondition groupCond : select.getGroupByConditions())
                {
                    if(groupCond.getVariable() != null)
                        innerProjections.add(new Projection(groupCond.getVariable()));
                    else if(groupCond.getExpression() instanceof Variable)
                        innerProjections.add(new Projection((Variable) groupCond.getExpression()));
                }
            }
            else
            {
                for(String var : translatedWhereClause.subqueryVars)
                    innerProjections.add(new Projection(new Variable(var)));
            }



            outerSelect.setDistinct(select.isDistinct());
            outerSelect.setReduced(select.isReduced());
            outerSelect.setLimit(select.getLimit());
            outerSelect.setOffset(select.getOffset());
            outerSelect.getOrderByConditions().addAll(select.getOrderByConditions());

            innerSelect.getDataSets().addAll(select.getDataSets());
            innerSelect.getGroupByConditions().addAll(select.getGroupByConditions());
            innerSelect.getHavingConditions().addAll(select.getHavingConditions());



            TranslatedSegment translatedSelectClause = translateSelectAsSqlNoValues(innerSelect, translatedWhereClause,
                    false);
            TranslatedSegment translatedValuesClause = visit(select.getValues());

            translatedSelectClause = translateJoin(translatedSelectClause, translatedValuesClause);

            return translateSelectAsSqlNoValues(outerSelect, translatedSelectClause, isTopLevel);
        }
    }

    /**
     * Builds the SELECT query as SQL when we already have translated WHERE clause.
     *
     * @param select Details of the SELECT clause.
     * @param translatedWhereClause Translated WHERE clause of the query.
     * @param isTopLevel Is this SPARQL SELECT the top-level one?.
     * @return Translated SELECT pattern as SQL.
     */
    private TranslatedSegment translateSelectAsSqlNoValues(Select select, TranslatedSegment translatedWhereClause,
            boolean isTopLevel)
    {
        StringBuilder strBuf = new StringBuilder();
        LinkedHashSet<String> usedVars = new LinkedHashSet<>();
        LinkedHashSet<String> possibleUnboundVars = new LinkedHashSet<>();


        if(!isTopLevel)
        {
            translatedWhereClause.str = indentBlock(translatedWhereClause.str, 1);

            newLine(strBuf);
            strBuf.append("(");
            queryDepth++;
            newLine(strBuf);
        }


        strBuf.append("SELECT ");

        // translate DISTINCT modifier, ignore REDUCED modifier
        if(select.isDistinct()/* || select.isReduced() */)
            strBuf.append("DISTINCT ");


        // OFFSET and LIMIT modifiers translates to TOP modifier
        if(select.getOffset() != null || select.getLimit() != null)
        {
            strBuf.append("TOP ");

            boolean offsetWritten = false;
            if(select.getOffset() != null)
            {
                offsetWritten = true;
                strBuf.append(select.getOffset().toString());
            }

            if(offsetWritten)
                strBuf.append(",");

            if(select.getLimit() != null)
            {
                strBuf.append(select.getLimit().toString());
            }
            else
            {
                strBuf.append("-1");
            }

            strBuf.append(" ");
        }


        // translate projections
        if(select.getProjections().isEmpty())
        {
            if(!translatedWhereClause.subqueryVars.isEmpty())
            {
                boolean first = true;

                for(String var : translatedWhereClause.subqueryVars)
                {
                    // filter out auxiliary/artificial variables
                    if(!this.queryVariables.contains(var))
                        continue;

                    if(!first)
                        strBuf.append(", ");
                    else
                        first = false;

                    strBuf.append('"').append(var).append('"');

                    usedVars.add(var);

                    if(translatedWhereClause.possibleUnboundVars.contains(var))
                        possibleUnboundVars.add(var);
                }
            }

            if(usedVars.isEmpty())
            {
                strBuf.append("1 AS \"" + getNewFakeStarName() + "\"");
            }
        }
        else
        {
            boolean first = true;

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


                if(!first)
                    strBuf.append(", ");
                else
                    first = false;

                if(proj.getExpression() == null)
                {
                    if(!translatedWhereClause.subqueryVars.contains(var))
                        strBuf.append("NULL AS ");
                }
                else
                {
                    strBuf.append(translateExpression(proj.getExpression(), EnumSet
                            .of(ExpressionTranslateFlag.SPECIAL_RELATION_OPERATORS, ExpressionTranslateFlag.LONG_CAST),
                            translatedWhereClause));
                    strBuf.append(" AS ");
                }

                strBuf.append("\"").append(var).append("\"");
            }
        }

        // insert translated WHERE clause
        newLine(strBuf);
        strBuf.append("FROM");
        strBuf.append(translatedWhereClause.str);


        // translate GROUP BY clauses
        if(!select.getGroupByConditions().isEmpty())
        {
            newLine(strBuf);
            strBuf.append("GROUP BY ");

            boolean first = true;
            List<GroupCondition> groupByConds = select.getGroupByConditions();
            for(GroupCondition groupByCond : groupByConds)
            {
                if(!first)
                    strBuf.append(", ");
                else
                    first = false;

                if(groupByCond.getVariable() == null)
                    strBuf.append(translateExpression(groupByCond.getExpression(),
                            EnumSet.of(ExpressionTranslateFlag.SPECIAL_RELATION_OPERATORS), translatedWhereClause));
                else
                    strBuf.append(translateLiteralNodeExpression(groupByCond.getVariable()));
            }
        }


        // translate HAVING clauses
        if(!select.getHavingConditions().isEmpty())
        {
            newLine(strBuf);
            strBuf.append("HAVING ");

            boolean first = true;
            List<Expression> havingConds = select.getHavingConditions();
            for(Expression havingCond : havingConds)
            {
                if(!first)
                    strBuf.append(", ");
                else
                    first = false;

                // translate constraints while checking validity of variables used
                HashSet<String> variablesToCheck = translatedWhereClause.subqueryVars;

                for(Projection proj : select.getProjections())
                    variablesToCheck.add(proj.getVariable().getName());

                strBuf.append(translateExpression(havingCond, null, translatedWhereClause));
            }
        }


        // translate ORDER BY clauses
        if(!select.getOrderByConditions().isEmpty())
        {
            newLine(strBuf);
            strBuf.append("ORDER BY ");

            boolean first = true;
            List<OrderCondition> orderByConds = select.getOrderByConditions();
            for(OrderCondition orderByCond : orderByConds)
            {
                OrderCondition.Direction dir = OrderCondition.Direction.Ascending;

                if(orderByCond.getDirection() != OrderCondition.Direction.Unspecified)
                    dir = orderByCond.getDirection();

                if(!first)
                    strBuf.append(", ");
                else
                    first = false;

                strBuf.append(translateExpression(orderByCond.getExpression(),
                        EnumSet.of(ExpressionTranslateFlag.SPECIAL_RELATION_OPERATORS), translatedWhereClause));
                strBuf.append(" ");

                if(dir == OrderCondition.Direction.Ascending)
                    strBuf.append("ASC");
                else
                    strBuf.append("DESC");
            }
        }

        // OPTION(QUIETCAST)
        newLine(strBuf);
        strBuf.append(sqlSelectSuffix);

        String newSqlTableName = "";
        if(!isTopLevel)
        {
            queryDepth--;
            newLine(strBuf);
            strBuf.append(") AS ");
            newSqlTableName = getNewTmpTabName();
            strBuf.append(newSqlTableName);
        }


        return new TranslatedSegment(strBuf.toString(), false, usedVars, possibleUnboundVars, newSqlTableName);
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
     * @return Translated inner procedure call as SQL.
     */
    @SuppressWarnings("unused")
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


        if(false)
        /* SQL variant */
        {
            /*
             * Translate
             */
            if(procedureCallContext != null && procedureCallContext.isSparql)
            {
                procedureCallContext = wrapAndNameTable(wrapSparqlSelect(procedureCallContext));
                procedureCallContext.str = indentBlock(procedureCallContext.str, 1);
            }

            if(procedureCallContext == null)
                procedureCallContext = new TranslatedSegment(null, false); // fake context

            TranslatedSegment fakeCallTranslation = new TranslatedSegment(null, false, usedVars, possibleUnboundVars,
                    getNewTmpTabName());


            /* SELECT */
            StringBuilder strBuf = new StringBuilder();
            newLine(strBuf);
            strBuf.append("SELECT ");

            HashMap<String, PropertyDirection> joinVariables = getJoinVariables(fakeCallTranslation,
                    procedureCallContext);

            LinkedHashSet<String> finalAllUsedVars = new LinkedHashSet<>();
            finalAllUsedVars.addAll(procedureCallContext.subqueryVars);
            finalAllUsedVars.addAll(fakeCallTranslation.subqueryVars);

            LinkedHashSet<String> finalPossibleUnboundVars = new LinkedHashSet<>();
            finalPossibleUnboundVars.addAll(procedureCallContext.possibleUnboundVars);
            finalPossibleUnboundVars.addAll(fakeCallTranslation.possibleUnboundVars);

            if(!finalAllUsedVars.isEmpty())
                strBuf.append(buildInnerJoinProjection(finalAllUsedVars, joinVariables,
                        fakeCallTranslation.sqlTableName, procedureCallContext.sqlTableName));
            else
                strBuf.append("1 AS \"" + getNewFakeStarName() + "\"");


            /* FROM */
            newLine(strBuf);
            strBuf.append("FROM");

            queryDepth++;
            newLine(strBuf);
            strBuf.append("\"").append(currProcConfig.getMappedProcName()).append("\"(");

            List<String> inputParams = new ArrayList<>();
            boolean firstParam = true;

            for(int i = 0; i < parameterCalls.getArr().size(); ++i)
            {
                String inputParam = getNewTmpVarName();

                if(!firstParam)
                    strBuf.append(", ");
                else
                    firstParam = false;

                strBuf.append("\"").append(inputParam).append("\"");
                inputParams.add(inputParam);
            }

            strBuf.append(")(");

            firstParam = true;

            for(String resultVarName : resultVarList)
            {
                if(resultVarName == null)
                    resultVarName = getNewTmpVarName();

                if(!firstParam)
                    strBuf.append(", ");
                else
                    firstParam = false;

                strBuf.append("\"").append(resultVarName).append("\" any");
            }

            strBuf.append(") ").append(fakeCallTranslation.sqlTableName);


            if(procedureCallContext.str != null)
            {
                strBuf.append(",");
                strBuf.append(indentBlock(procedureCallContext.str, queryDepth + 1));
            }

            queryDepth--;


            /* WHERE */
            newLine(strBuf);
            strBuf.append("WHERE (( ");

            // passing the parameters to the delegate function
            boolean firstInputPar = true;

            for(int i = 0; i < inputParams.size(); ++i)
            {
                if(!firstInputPar)
                    strBuf.append(" AND ");
                else
                    firstInputPar = false;

                strBuf.append("\"").append(inputParams.get(i)).append("\"");
                strBuf.append(" = ");

                ParameterCall par = parameterCalls.getArr().get(i);

                //NOTE: workaround for https://github.com/openlink/virtuoso-opensource/issues/433
                strBuf.append("\"echo\"(");

                if(par.getVar() != null)
                    strBuf.append(procedureCallContext.sqlTableName).append(".\"").append(par.getVar()).append("\"");
                else
                    strBuf.append(par.getLiteralVal());

                strBuf.append(")");
            }

            strBuf.append(" ) AND ( ");

            // join condition
            strBuf.append(buildInnerJoinOnCondition(joinVariables, fakeCallTranslation.sqlTableName,
                    procedureCallContext.sqlTableName));


            // constant result value filter
            if(!varsToFilter.isEmpty())
            {
                strBuf.append(" ) AND ( ");

                for(Pair<String, Node> varToFilter : varsToFilter)
                {
                    String var = varToFilter.getKey();
                    Node filterNode = varToFilter.getValue();

                    strBuf.append(fakeCallTranslation.sqlTableName).append(".\"").append(var).append("\"");
                    strBuf.append(" = ");
                    strBuf.append(translateLiteralNodeExpression((Expression) filterNode));
                }
            }

            strBuf.append(" ))");


            // OPTION(QUIETCAST)
            newLine(strBuf);
            strBuf.append(sqlSelectSuffix);


            TranslatedSegment translatedSegment = wrapAndNameTable(
                    new TranslatedSegment(strBuf.toString(), false, finalAllUsedVars, finalPossibleUnboundVars));


            // NOTE: it is primitive, but sufficient
            if(!graphOrServiceRestrictions.isEmpty())
            {
                GraphOrServiceRestriction restriction = graphOrServiceRestrictions.peek();

                if(restriction.getVarOrIri() instanceof Variable)
                {
                    String var = ((Variable) restriction.getVarOrIri()).getName();
                    String table = getNewTmpTabName();

                    StringBuilder graphStrBuf = new StringBuilder();
                    newLine(graphStrBuf);
                    graphStrBuf.append("(SELECT ");
                    graphStrBuf.append(translateLiteralNodeExpression(procedureName));
                    graphStrBuf.append(" AS \"");
                    graphStrBuf.append(var);
                    graphStrBuf.append("\") AS ");
                    graphStrBuf.append(table);

                    LinkedHashSet<String> graphVars = new LinkedHashSet<>();
                    graphVars.add(var);

                    TranslatedSegment graphSegment = new TranslatedSegment(graphStrBuf.toString(), false, graphVars,
                            new LinkedHashSet<>(), table);

                    translatedSegment = translateJoin(translatedSegment, graphSegment);
                }
            }

            return translatedSegment;
        }
        else if(false)
        /* SERVICE variant */
        {
            //TODO: Jak se chovat uvnitř grafu či service?

            StringBuilder strBuf = new StringBuilder();

            strBuf.append("\nSERVICE <http://localhost:3030/orchem> {"); // FIXME: set IRI


            // subject

            if(procedureCallBase instanceof ProcedureCall)
            {
                ProcedureCall procedureCall = (ProcedureCall) procedureCallBase;
                Node resultNode = procedureCall.getResult();

                if(resultNode instanceof Variable)
                    strBuf.append("?").append(((Variable) resultNode).getName());
                else if(resultNode instanceof BlankNode)
                    strBuf.append("?").append(getTmpVarNameForBlankNode(((BlankNode) resultNode).getName()));
                else
                    strBuf.append(resultNode.toString());

                strBuf.append(" ");
            }
            else
            {
                strBuf.append("(");

                MultiProcedureCall multiProcedureCall = (MultiProcedureCall) procedureCallBase;

                HashMap<String, Node> actualResultCalls = new HashMap<>();

                for(Parameter par : multiProcedureCall.getResults())
                    actualResultCalls.put(par.getName().toString(), par.getValue());

                for(ResultDefinition resDef : currProcConfig.getResults())
                {
                    String resultParamName = resDef.getParamName();
                    Node resultNode = actualResultCalls.get(resultParamName);
                    actualResultCalls.remove(resultParamName);

                    if(resultNode == null)
                        strBuf.append("[]");
                    else if(resultNode instanceof Variable)
                        strBuf.append("?").append(((Variable) resultNode).getName());
                    else if(resultNode instanceof BlankNode)
                        strBuf.append("?").append(getTmpVarNameForBlankNode(((BlankNode) resultNode).getName()));
                    else
                        strBuf.append(resultNode.toString());

                    strBuf.append(" ");
                }

                strBuf.append(")");
            }


            // predicate
            strBuf.append(" <http://bioinfo.uochb.cas.cz/0.9/orchem#").append(currProcConfig.getMappedProcName())
                    .append("> ");


            // FIXME: ////////////////////////////////////////////////////
            // FIXME: default value in SQL format?
            // FIXME: no return variable?

            ParameterCalls parameterValues = new ParameterCalls();

            for(ParameterDefinition param : currProcConfig.getParameters())
            {
                String paramName = param.getParamName();
                String defaultVal = param.getDefaultValue();
                String typeIRI = param.getTypeIRI();

                if(defaultVal.isEmpty())
                    defaultVal = null;

                parameterValues.getArr().add(new ParameterCall(paramName, null, typeIRI, defaultVal));
            }


            for(ProcedureCall.Parameter par : procedureCallBase.getParameters())
            {
                String parName = par.getName().toString();
                Node paramValue = par.getValue();

                ParameterCall parameterCall = parameterValues.find(parName);

                if(parameterCall == null)
                    continue;

                if(paramValue instanceof Variable || paramValue instanceof BlankNode)
                {
                    String varName;

                    if(paramValue instanceof Variable)
                        varName = ((Variable) paramValue).getName();
                    else
                        varName = getTmpVarNameForBlankNode(((BlankNode) paramValue).getName());

                    parameterCall.setVar(varName);
                }
                else if(paramValue instanceof Literal || paramValue instanceof IRI)
                {
                    parameterCall.setLiteralVal(paramValue.toString());
                }
            }
            /////////////////////////////////////////////////////////////


            // object
            strBuf.append("(");
            for(ParameterCall par : parameterValues.getArr())
            {
                if(par.getVar() != null)
                    strBuf.append("?").append(par.getVar());
                else
                    strBuf.append(par.getLiteralVal());

                strBuf.append(" ");
            }
            strBuf.append(")");


            strBuf.append("}");

            TranslatedSegment translatedSegment = new TranslatedSegment(strBuf.toString(), true, usedVars,
                    possibleUnboundVars);


            translatedSegment = translateJoin(procedureCallContext, translatedSegment);
            return translatedSegment;
        }
        else
        /* Store variant */
        {
            TranslatedSegment originalContext = procedureCallContext;


            /*
             * Translate 1st part
             */
            LinkedHashSet<String> parameterVars = new LinkedHashSet<String>();

            for(ParameterCall par : parameterCalls.getArr())
                if(par.getVar() != null)
                    parameterVars.add(par.getVar());

            /*
            if(procedureCallContext != null && procedureCallContext.isSparql)
            {
                procedureCallContext = wrapAndNameTable(wrapSparqlContext(procedureCallContext, parameterVars));
                procedureCallContext.str = indentBlock(procedureCallContext.str, 1);
            }
            */

            if(procedureCallContext == null)
                procedureCallContext = new TranslatedSegment("", true); // fake context


            TranslatedSegment fakeCallTranslation = new TranslatedSegment(null, false, usedVars, possibleUnboundVars,
                    getNewTmpTabName());


            /* SELECT */
            /*
            String retVar = getNewTmpVarName();

            StringBuilder strBuf = new StringBuilder();
            newLine(strBuf);
            strBuf.append("SELECT ");

            strBuf.append(fakeCallTranslation.sqlTableName + ".\"" + retVar + "\"");


            // FROM
            newLine(strBuf);
            strBuf.append("FROM");

            queryDepth++;
            newLine(strBuf);
            strBuf.append("\"").append(currProcConfig.getMappedProcName()).append("_store").append("\"(");

            List<String> inputParams = new ArrayList<>();

            String inputGraphParam = getNewTmpVarName();
            strBuf.append("\"").append(inputGraphParam).append("\"");

            for(int i = 0; i < parameterCalls.getArr().size(); ++i)
            {
                String inputParam = getNewTmpVarName();
                strBuf.append(", ");
                strBuf.append("\"").append(inputParam).append("\"");
                inputParams.add(inputParam);
            }

            strBuf.append(")(");
            strBuf.append("\"").append(retVar).append("\" integer");
            strBuf.append(") ").append(fakeCallTranslation.sqlTableName);


            if(procedureCallContext.str != null)
            {
                strBuf.append(",");
                strBuf.append(indentBlock(procedureCallContext.str, queryDepth));
            }

            queryDepth--;


            // WHERE
            newLine(strBuf);
            strBuf.append("WHERE ( ");

            // passing the parameters to the delegate function
            strBuf.append("\"").append(inputGraphParam).append("\"");
            strBuf.append(" = ");
            strBuf.append("\"echo\"(");
            strBuf.append(graphID);
            strBuf.append(")");

            for(int i = 0; i < inputParams.size(); ++i)
            {
                strBuf.append(" AND ");

                strBuf.append("\"").append(inputParams.get(i)).append("\"");
                strBuf.append(" = ");

                ParameterCall par = parameterCalls.getArr().get(i);

                //NOTE: workaround for https://github.com/openlink/virtuoso-opensource/issues/433
                strBuf.append("\"echo\"(");

                if(par.getVar() != null)
                    strBuf.append(procedureCallContext.sqlTableName).append(".\"").append(par.getVar()).append("\"");
                else
                    strBuf.append(par.getLiteralVal());

                strBuf.append(")");
            }

            strBuf.append(" )");
            */



            /*
            sparql DEFINE input:inference "ontology"
            SELECT (sum(?_COUNT) as ?SUM)  WHERE
            {
            ?AMG (<http://bioinfo.uochb.cas.cz/0.9/chebi#hasRole>/ <http://bioinfo.uochb.cas.cz/0.9/chebi#inRelationWithValue>) / <http://bioinfo.uochb.cas.cz/0.9/chebi#identifier> "73190" .
            ?__tmpVar08 ^(<http://bioinfo.uochb.cas.cz/0.9/chebi#molFile>/ <http://bioinfo.uochb.cas.cz/0.9/chebi#molFileValue>) ?AMG .

            BIND (sql:simsearch_store(300, ?__tmpVar08, 'MOL', 0.95, -1) as ?_COUNT)
            };
             */

            StringBuilder strBuf = new StringBuilder();
            strBuf.append("sparql ");

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


            strBuf.append(") AS ?").append(countVar).append(")\n");


            strBuf.append("}\n");




            // OPTION(QUIETCAST)
            //newLine(strBuf);
            //strBuf.append(sqlSelectSuffix);


            //System.out.println("#############################");
            //System.out.println(strBuf.toString());
            //System.out.println("#############################");


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

                    /*
                    if(par.getValue() instanceof BlankNode)
                        strBuf2.append("?").append(getTmpVarNameForBlankNode(((BlankNode) par.getValue()).getName()));
                    else
                        strBuf2.append(par.getValue().toString());
                    */

                }
            }


            strBuf2.append(" .\n");
            strBuf2.append("}\n");

            for(Entry<String, String> pair : filterMap.entrySet())
            {
                strBuf2.append("filter(");

                /*
                strBuf2.append("?");
                strBuf2.append(pair.getKey());
                strBuf2.append(" = ");
                strBuf2.append(pair.getValue());


                strBuf2.append(" || ?");
                strBuf2.append(pair.getKey());
                strBuf2.append(" = str(");
                strBuf2.append(pair.getValue());
                strBuf2.append(")");
                */

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



            /*
            strBuf2.append("\nGRAPH <http://localhost/result").append(graphID++).append("> ");
            strBuf2.append("\n{\n");

            if(procedureCallBase instanceof ProcedureCall)
            {
                ProcedureCall procedureCall = (ProcedureCall) procedureCallBase;
                strBuf2.append("  ").append(procedureCall.getResult().toString());
            }
            else
            {
                strBuf2.append("  [");

                MultiProcedureCall multiProcedureCall = (MultiProcedureCall) procedureCallBase;


                boolean first = true;

                for(Parameter par : multiProcedureCall.getResults())
                {
                    if(par.getValue() != null)
                    {
                        if(!first)
                            strBuf2.append("; ");

                        strBuf2.append(par.getName().toString());
                        strBuf2.append(" ");
                        strBuf2.append(par.getValue().toString());

                        first = false;
                    }
                }

                strBuf2.append(" ] ");
            }

            strBuf2.append(procedureCallBase.getProcedure().toString());

            strBuf2.append("  [");


            boolean first = true;

            for(ProcedureCall.Parameter par : procedureCallBase.getParameters())
            {
                if(par.getValue() instanceof Variable || par.getValue() instanceof BlankNode)
                {
                    if(!first)
                        strBuf2.append("; ");

                    strBuf2.append(par.getName().toString());
                    strBuf2.append(" ");

                    if(par.getValue() instanceof BlankNode)
                        strBuf2.append("?").append(getTmpVarNameForBlankNode(((BlankNode) par.getValue()).getName()));
                    else
                        strBuf2.append(par.getValue().toString());

                    first = false;
                }
            }

            strBuf2.append(" ] ");

            strBuf2.append("\n}");
            */




            //TranslatedSegment translatedSegment = wrapAndNameTable(new TranslatedSegment(strBuf.toString(), false,
            //        finalAllUsedVars, finalPossibleUnboundVars));

            TranslatedSegment procedureGraph = new TranslatedSegment(strBuf2.toString(), true,
                    fakeCallTranslation.subqueryVars, fakeCallTranslation.possibleUnboundVars);

            TranslatedSegment translatedSegment = translateJoin(originalContext, procedureGraph);

            graphID++;

            return translatedSegment;
        }
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


        LinkedList<Filter> sqlFilters = new LinkedList<Filter>();
        LinkedList<Filter> sparqlFilters = new LinkedList<Filter>();


        for(Filter filter : filters)
        {
            if(containsProcedureCall(filter.getConstraint()))
                sqlFilters.add(filter);
            else
                sparqlFilters.add(filter);
        }


        /*
         * SPARQL translation
         */
        if(prevTranslatedPattern.isSparql && !fullQueryDecomposition)
        {
            StringBuilder strBuf = new StringBuilder();
            strBuf.append(prevTranslatedPattern.str);

            for(Filter filter : sparqlFilters)
            {
                strBuf.append("\n");
                strBuf.append(filter.toString());
            }

            TranslatedSegment result = new TranslatedSegment(strBuf.toString(), true,
                    prevTranslatedPattern.subqueryVars, prevTranslatedPattern.possibleUnboundVars);

            if(sqlFilters.isEmpty())
                return result;
            else
                prevTranslatedPattern = result;
        }
        else
        {
            sqlFilters.addAll(sparqlFilters);
        }


        /*
         * SQL translation
         */
        if(prevTranslatedPattern.isSparql)
        {
            prevTranslatedPattern = wrapAndNameTable(wrapSparqlSelect(prevTranslatedPattern));
            prevTranslatedPattern.str = indentBlock(prevTranslatedPattern.str, 1);
        }

        StringBuilder strBuf = new StringBuilder();
        newLine(strBuf);
        strBuf.append("(");

        queryDepth++;
        newLine(strBuf);
        strBuf.append("SELECT ");


        if(prevTranslatedPattern.subqueryVars.isEmpty())
            strBuf.append("TOP 1 1 AS \"").append(getNewTmpVarName()).append("\"");

        boolean first = true;
        for(String var : prevTranslatedPattern.subqueryVars)
        {
            if(!first)
                strBuf.append(", ");
            else
                first = false;

            strBuf.append("\"").append(var).append("\"");
        }

        newLine(strBuf);
        strBuf.append("FROM");

        prevTranslatedPattern.str = indentBlock(prevTranslatedPattern.str, 2);
        strBuf.append(prevTranslatedPattern.str);


        newLine(strBuf);
        strBuf.append("WHERE ");
        strBuf.append("( ");

        boolean firstInWhere = true;

        for(Pattern pat : sqlFilters)
        {
            if(!firstInWhere)
                strBuf.append(" AND ");
            else
                firstInWhere = false;

            strBuf.append(translateExpression(((Filter) pat).getConstraint(), null, prevTranslatedPattern));
        }

        strBuf.append(" )");


        // OPTION(QUIETCAST)
        newLine(strBuf);
        strBuf.append(sqlSelectSuffix);

        queryDepth--;
        newLine(strBuf);
        strBuf.append(") AS ");
        String newTabName = getNewTmpTabName();
        strBuf.append(newTabName);


        return new TranslatedSegment(strBuf.toString(), false, prevTranslatedPattern.subqueryVars,
                prevTranslatedPattern.possibleUnboundVars, newTabName);
    }


    /**
     * Processes the BIND clause - move the binded expression to the projection of SQL SELECT.
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

        /*
         * SPARQL translation
         */
        if((prevTranslatedPattern == null || prevTranslatedPattern.isSparql) && !fullQueryDecomposition)
        {
            if(prevTranslatedPattern != null)
                strBuf.append(prevTranslatedPattern.str);

            strBuf.append("\n" + bindPat.toString());

            return new TranslatedSegment(strBuf.toString(), true, usedVariables, possibleUnboundVars);
        }


        /*
         * SQL translation
         */
        if(prevTranslatedPattern == null)
            prevTranslatedPattern = new TranslatedSegment(getDummyTableStr(), false);

        if(prevTranslatedPattern.isSparql)
        {
            prevTranslatedPattern = wrapAndNameTable(wrapSparqlSelect(prevTranslatedPattern));
            prevTranslatedPattern.str = indentBlock(prevTranslatedPattern.str, 1);
        }


        newLine(strBuf);
        strBuf.append("(");

        queryDepth++;
        newLine(strBuf);
        strBuf.append("SELECT ");


        boolean first = true;
        for(String var : prevTranslatedPattern.subqueryVars)
        {
            if(!first)
                strBuf.append(", ");
            else
                first = false;

            strBuf.append("\"").append(var).append("\"");
        }


        if(!first)
            strBuf.append(", ");

        strBuf.append("(");
        strBuf.append(translateExpression(bindPat.getExpression(),
                EnumSet.of(ExpressionTranslateFlag.SPECIAL_RELATION_OPERATORS, ExpressionTranslateFlag.LONG_CAST),
                prevTranslatedPattern));
        strBuf.append(") AS ");



        strBuf.append("\"").append(varName).append("\"");


        newLine(strBuf);
        strBuf.append("FROM");

        prevTranslatedPattern.str = indentBlock(prevTranslatedPattern.str, 2);
        strBuf.append(prevTranslatedPattern.str);

        // OPTION(QUIETCAST)
        newLine(strBuf);
        strBuf.append(sqlSelectSuffix);

        queryDepth--;
        newLine(strBuf);
        strBuf.append(") AS ");
        String newTabName = getNewTmpTabName();
        strBuf.append(newTabName);

        return new TranslatedSegment(strBuf.toString(), false, usedVariables, possibleUnboundVars, newTabName);
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

        /*
         * SPARQL JOIN
         */
        if(prevTranslatedPattern.isSparql && translatedPattern.isSparql && !fullQueryDecomposition)
        {
            strBuf.append(prevTranslatedPattern.str);
            strBuf.append(translatedPattern.str);

            return new TranslatedSegment(strBuf.toString(), true, allUsedVars, allPossibleUnboundVars);
        }


        /*
         * SQL JOIN
         */
        if(prevTranslatedPattern.isSparql)
        {
            prevTranslatedPattern = wrapAndNameTable(wrapSparqlSelect(prevTranslatedPattern));
            prevTranslatedPattern.str = indentBlock(prevTranslatedPattern.str, 1);
        }

        if(translatedPattern.isSparql)
        {
            translatedPattern = wrapAndNameTable(wrapSparqlSelect(translatedPattern));
            translatedPattern.str = indentBlock(translatedPattern.str, 1);
        }


        HashMap<String, PropertyDirection> joinVariables = getJoinVariables(prevTranslatedPattern, translatedPattern);


        newLine(strBuf);
        strBuf.append("(");
        queryDepth++;


        newLine(strBuf);
        strBuf.append("SELECT ");
        strBuf.append(buildInnerJoinProjection(allUsedVars, joinVariables, prevTranslatedPattern.sqlTableName,
                translatedPattern.sqlTableName));
        newLine(strBuf);
        strBuf.append("FROM");

        prevTranslatedPattern.str = indentBlock(prevTranslatedPattern.str, 2);
        strBuf.append(prevTranslatedPattern.str);

        newLine(strBuf);
        strBuf.append("INNER JOIN");

        translatedPattern.str = indentBlock(translatedPattern.str, 2);
        strBuf.append(translatedPattern.str);


        newLine(strBuf);
        strBuf.append("ON ( ");
        strBuf.append(buildInnerJoinOnCondition(joinVariables, prevTranslatedPattern.sqlTableName,
                translatedPattern.sqlTableName));
        strBuf.append(" )");


        // OPTION(QUIETCAST)
        newLine(strBuf);
        strBuf.append(sqlSelectSuffix);


        queryDepth--;
        newLine(strBuf);
        strBuf.append(") AS ");

        String joinedTableName = getNewTmpTabName();
        strBuf.append(joinedTableName);

        return new TranslatedSegment(strBuf.toString(), false, allUsedVars, allPossibleUnboundVars, joinedTableName);
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



        boolean isSqlFilter = optionalFilters.stream().anyMatch(x -> containsProcedureCall(x.getConstraint()));
        StringBuilder strBuf = new StringBuilder();



        /*
         * SPARQL OPTIONAL
         */
        if((prevTranslatedPattern == null || prevTranslatedPattern.isSparql)
                && (translatedPattern == null || translatedPattern.isSparql) && !isSqlFilter && !fullQueryDecomposition)
        {
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

            return new TranslatedSegment(strBuf.toString(), true, allUsedVars, allPossibleUnboundVars);
        }


        /*
         * SQL OPTIONAL
         */
        if(prevTranslatedPattern == null)
            prevTranslatedPattern = new TranslatedSegment(getDummyTableStr(), false);

        if(prevTranslatedPattern.isSparql)
        {
            prevTranslatedPattern = wrapAndNameTable(wrapSparqlSelect(prevTranslatedPattern));
            prevTranslatedPattern.str = indentBlock(prevTranslatedPattern.str, 1);
        }

        if(translatedPattern.isSparql)
        {
            translatedPattern = wrapAndNameTable(wrapSparqlSelect(translatedPattern));
            translatedPattern.str = indentBlock(translatedPattern.str, 1);
        }


        HashMap<String, PropertyDirection> joinVariables = getJoinVariables(prevTranslatedPattern, translatedPattern);
        HashMap<String, String> variablesMap = new HashMap<>();
        LinkedHashSet<String> possibleUnboundVars = new LinkedHashSet<>();

        for(String var : allUsedVars)
        {
            PropertyDirection unboundProperty = joinVariables.get(var);
            String leftVar = prevTranslatedPattern.sqlTableName + ".\"" + var + "\"";
            String rightVar = translatedPattern.sqlTableName + ".\"" + var + "\"";

            if(unboundProperty != null)
            {
                if(!useCorrectJoin)
                {
                    variablesMap.put(var, rightVar);
                }
                else if(unboundProperty == PropertyDirection.BOTH)
                {
                    variablesMap.put(var, "COALESCE(" + leftVar + ", " + rightVar + ")");
                }
                else if(unboundProperty == PropertyDirection.LEFT)
                {
                    variablesMap.put(var, rightVar);
                }
                else if(unboundProperty == PropertyDirection.RIGHT || unboundProperty == PropertyDirection.NONE)
                {
                    variablesMap.put(var, leftVar);
                }
            }
            else if(prevTranslatedPattern.subqueryVars.contains(var))
            {
                variablesMap.put(var, leftVar);
            }
            else
            {
                variablesMap.put(var, rightVar);
            }

            if(prevTranslatedPattern.possibleUnboundVars.contains(var)
                    && translatedPattern.possibleUnboundVars.contains(var))
                possibleUnboundVars.add(var);
        }


        String filterExpression = null;

        for(Filter filter : optionalFilters)
        {
            String translatedConstraint = "("
                    + translateExpression(filter.getConstraint(), null, variablesMap, possibleUnboundVars) + ")";

            if(filterExpression == null)
                filterExpression = translatedConstraint;
            else
                filterExpression = filterExpression + " AND " + translatedConstraint;
        }


        newLine(strBuf);
        strBuf.append("(");
        queryDepth++;


        newLine(strBuf);
        strBuf.append("SELECT ");
        strBuf.append(buildInnerJoinProjection(allUsedVars, joinVariables, prevTranslatedPattern.sqlTableName,
                translatedPattern.sqlTableName));
        newLine(strBuf);
        strBuf.append("FROM");

        prevTranslatedPattern.str = indentBlock(prevTranslatedPattern.str, 2);
        strBuf.append(prevTranslatedPattern.str);

        newLine(strBuf);
        strBuf.append("LEFT JOIN");

        translatedPattern.str = indentBlock(translatedPattern.str, 2);
        strBuf.append(translatedPattern.str);


        newLine(strBuf);
        strBuf.append("ON ( ");
        strBuf.append(buildInnerJoinOnCondition(joinVariables, prevTranslatedPattern.sqlTableName,
                translatedPattern.sqlTableName));

        if(filterExpression != null)
        {
            strBuf.append(" AND ");
            strBuf.append(filterExpression);
        }

        strBuf.append(" )");


        // OPTION(QUIETCAST)
        newLine(strBuf);
        strBuf.append(sqlSelectSuffix);


        queryDepth--;
        newLine(strBuf);
        strBuf.append(") AS ");

        String joinedTableName = getNewTmpTabName();
        strBuf.append(joinedTableName);

        return new TranslatedSegment(strBuf.toString(), false, allUsedVars, allPossibleUnboundVars, joinedTableName);
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


        /*
         * SPARQL MINUS
         */
        if(prevTranslatedPattern.isSparql && translatedPattern.isSparql && !fullQueryDecomposition)
        {
            strBuf.append(prevTranslatedPattern.str);
            strBuf.append("\n");
            strBuf.append("MINUS");
            strBuf.append(translatedPattern.str);

            return new TranslatedSegment(strBuf.toString(), true, prevTranslatedPattern.subqueryVars,
                    prevTranslatedPattern.possibleUnboundVars);
        }


        /*
         * SQL MINUS
         */
        if(prevTranslatedPattern.isSparql)
        {
            prevTranslatedPattern = wrapAndNameTable(wrapSparqlSelect(prevTranslatedPattern));
            //prevTranslatedPattern.str = indentBlock(prevTranslatedPattern.str, 1);
        }

        if(translatedPattern.isSparql)
        {
            translatedPattern = wrapAndNameTable(wrapSparqlSelect(translatedPattern));
            //translatedPattern.str = indentBlock(translatedPattern.str, 1);
        }


        newLine(strBuf);
        strBuf.append("(");
        queryDepth++;

        newLine(strBuf);
        strBuf.append("SELECT *");
        newLine(strBuf);
        strBuf.append("FROM");

        prevTranslatedPattern.str = indentBlock(prevTranslatedPattern.str, 2);
        strBuf.append(prevTranslatedPattern.str);

        newLine(strBuf);
        strBuf.append("WHERE ");


        strBuf.append("NOT EXISTS (");
        ++queryDepth;
        newLine(strBuf);

        // select compatible solution
        strBuf.append("SELECT TOP 1 1 AS __existsRetVal");
        newLine(strBuf);
        strBuf.append("FROM");

        translatedPattern.str = indentBlock(translatedPattern.str, 3);
        strBuf.append(translatedPattern.str);

        newLine(strBuf);
        strBuf.append("WHERE ( ");

        HashMap<String, PropertyDirection> joinVariables = getJoinVariables(prevTranslatedPattern, translatedPattern);

        if(joinVariables.isEmpty())
            strBuf.append("0");
        else
            strBuf.append(buildInnerJoinOnCondition(joinVariables, prevTranslatedPattern.sqlTableName,
                    translatedPattern.sqlTableName));

        strBuf.append(" )");
        --queryDepth;
        newLine(strBuf);
        strBuf.append(")");


        // OPTION(QUIETCAST)
        newLine(strBuf);
        strBuf.append(sqlSelectSuffix);

        --queryDepth;
        newLine(strBuf);
        strBuf.append(") AS ");

        String joinedTableName = getNewTmpTabName();
        strBuf.append(joinedTableName);

        return new TranslatedSegment(strBuf.toString(), false, prevTranslatedPattern.subqueryVars,
                prevTranslatedPattern.possibleUnboundVars, joinedTableName);
    }


    /**
     * Constructs a wrapping SPARQL SELECT which can be then used inside the SQL query.
     *
     * @param translatedGraphPattern SPARQL subquery to be wrapped.
     * @return Wrapped SPARQL subquery.
     */
    private TranslatedSegment wrapSparqlSelect(TranslatedSegment translatedGraphPattern)
    {
        // e.g. ?X ?P ?Y --> sparql SELECT * WHERE { ?X ?P ?Y . }

        LinkedHashSet<String> usedVars = new LinkedHashSet<>();
        usedVars.addAll(translatedGraphPattern.subqueryVars);

        StringBuilder strBuf = new StringBuilder();

        newLine(strBuf);
        strBuf.append("sparql");

        for(Define def : prologue.getDefines())
        {
            strBuf.append(" ");
            strBuf.append(def.toString());
        }

        strBuf.append(" SELECT *");

        for(DataSet dataSet : selectDatasets.peek())
        {
            strBuf.append(" ");
            strBuf.append(dataSet.toString());
        }

        strBuf.append(" WHERE");
        newLine(strBuf);
        strBuf.append("{");

        int blockIndent = 2;

        if(!graphOrServiceRestrictions.isEmpty())
        {
            // NOTE: The pattern should contain all restrictions, but we used virtuoso semantics.
            GraphOrServiceRestriction restriction = graphOrServiceRestrictions.peek();

            if(restriction.getVarOrIri() instanceof Variable)
                usedVars.add(((Variable) restriction.getVarOrIri()).getName());

            blockIndent++;
            queryDepth++;
            newLine(strBuf);

            if(restriction.isRestrictionType(GraphOrServiceRestriction.RestrictionType.GRAPH_RESTRICTION))
                strBuf.append("GRAPH ");
            else if(restriction.isSilent())
                strBuf.append("SERVICE SILENT ");
            else
                strBuf.append("SERVICE ");

            strBuf.append(restriction.getVarOrIri().toString());
            newLine(strBuf);
            strBuf.append("{");
        }

        strBuf.append(indentBlock(translatedGraphPattern.str, blockIndent));

        if(!graphOrServiceRestrictions.isEmpty())
        {
            newLine(strBuf);
            strBuf.append("}");
            queryDepth--;
        }

        newLine(strBuf);
        strBuf.append("}");

        return new TranslatedSegment(strBuf.toString(), false, usedVars, translatedGraphPattern.possibleUnboundVars);
    }

    /*
    private TranslatedSegment wrapSparqlContext(TranslatedSegment translatedGraphPattern, LinkedHashSet<String> parVars)
    {
        // e.g. ?X ?P ?Y --> sparql SELECT * WHERE { ?X ?P ?Y . }

        LinkedHashSet<String> usedVars = new LinkedHashSet<>();
        usedVars.addAll(translatedGraphPattern.subqueryVars);

        StringBuilder strBuf = new StringBuilder();

        newLine(strBuf);
        strBuf.append("sparql");

        for(Define def : prologue.getDefines())
        {
            strBuf.append(" ");
            strBuf.append(def.toString());
        }

        strBuf.append(" SELECT DISTINCT ");

        for(String var : parVars)
            strBuf.append("?").append(var).append(" ");


        for(DataSet dataSet : selectDatasets.peek())
        {
            strBuf.append(" ");
            strBuf.append(dataSet.toString());
        }

        strBuf.append(" WHERE");
        newLine(strBuf);
        strBuf.append("{");

        int blockIndent = 2;

        if(!graphOrServiceRestrictions.isEmpty())
        {
            // NOTE: The pattern should contain all restrictions, but we used virtuoso semantics.
            GraphOrServiceRestriction restriction = graphOrServiceRestrictions.peek();

            if(restriction.getVarOrIri() instanceof Variable)
                usedVars.add(((Variable) restriction.getVarOrIri()).getName());

            blockIndent++;
            queryDepth++;
            newLine(strBuf);

            if(restriction.isRestrictionType(GraphOrServiceRestriction.RestrictionType.GRAPH_RESTRICTION))
                strBuf.append("GRAPH ");
            else if(restriction.isSilent())
                strBuf.append("SERVICE SILENT ");
            else
                strBuf.append("SERVICE ");

            strBuf.append(restriction.getVarOrIri().toString());
            newLine(strBuf);
            strBuf.append("{");
        }

        strBuf.append(indentBlock(translatedGraphPattern.str, blockIndent));

        if(!graphOrServiceRestrictions.isEmpty())
        {
            newLine(strBuf);
            strBuf.append("}");
            queryDepth--;
        }

        newLine(strBuf);
        strBuf.append("}");

        return new TranslatedSegment(strBuf.toString(), false, usedVars, translatedGraphPattern.possibleUnboundVars);
    }
    */


    /**
     * Wraps the SQL table with brackets and name it.
     *
     * @param tableResult Translated SQL table.
     * @return Wrapped and named SQL table.
     */
    private TranslatedSegment wrapAndNameTable(TranslatedSegment tableResult)
    {
        StringBuilder strBuf = new StringBuilder();

        newLine(strBuf);
        strBuf.append("(");

        tableResult.str = indentBlock(tableResult.str, 1);
        strBuf.append(tableResult.str);

        newLine(strBuf);
        strBuf.append(") AS ");
        String newSqlTableName = getNewTmpTabName();
        strBuf.append(newSqlTableName);

        tableResult.str = strBuf.toString();
        tableResult.sqlTableName = newSqlTableName;

        return tableResult;
    }

    //-------------------------------------------------------------------------


    /**
     * Constructs the exist-constraint - used for the translation of MINUS and EXISTS SPARQL patterns.
     *
     * @param variablesMap Join variables and their table references.
     * @param existInPattern Pattern by which we will filter.
     * @param isNegated true if the expression is negated (NOT EXISTS).
     * @param minusSemantics true if the condition is used for the translation of SPARQL MINUS pattern; false if used
     *            for the EXIST pattern.
     * @return Translated exist-constraint.
     */
    private String buildFilterExistConstraint(HashMap<String, String> variablesMap,
            LinkedHashSet<String> possibleUnboundedVars, TranslatedSegment existInPattern, boolean filterMode,
            boolean isNegated)
    {
        StringBuilder strBuf = new StringBuilder();

        if(isNegated)
        {
            if(filterMode)
                strBuf.append("NOT ");
            else
                strBuf.append("__not ");
        }

        if(filterMode)
            strBuf.append("EXISTS (");
        else
            strBuf.append("( COALESCE ( (");

        ++queryDepth;
        newLine(strBuf);
        strBuf.append("SELECT TOP 1 1 AS __existsRetVal");
        newLine(strBuf);
        strBuf.append("FROM");

        existInPattern.str = indentBlock(existInPattern.str, 3);
        strBuf.append(existInPattern.str);

        newLine(strBuf);
        strBuf.append("WHERE ( ");
        strBuf.append(buildExistsJoinCondition(variablesMap, possibleUnboundedVars, existInPattern));
        strBuf.append(" )");
        --queryDepth;
        newLine(strBuf);


        if(filterMode)
            strBuf.append(")");
        else
            strBuf.append("), 0))");

        return strBuf.toString();
    }


    /**
     * Constructs the condition for the translation of SPARQL EXISTS clause. for the INNER JOINs of two tables.
     *
     * @param variablesMap Join variables and their table references.
     * @param currTmpTableName Name of the second table to be joined.
     * @param minusSemantics true if the condition is used for the translation of SPARQL MINUS pattern; false if used
     *            for the EXIST pattern.
     * @return String with the condition that is to be used inside the SQL WHERE clause.
     */
    private String buildExistsJoinCondition(HashMap<String, String> variablesMap,
            LinkedHashSet<String> possibleUnboundedVars, TranslatedSegment existInPattern)
    {
        StringBuilder strBuf = new StringBuilder();
        boolean first = true;

        for(String varStr : existInPattern.subqueryVars)
        {
            String mapValue = variablesMap.get(varStr);

            if(mapValue == null)
                continue;

            if(!first)
                strBuf.append(" AND ");
            else
                first = false;

            String firstTabVar = mapValue;
            String secondTabVar = existInPattern.sqlTableName + ".\"" + varStr + "\"";

            if(possibleUnboundedVars.contains(varStr))
            {
                strBuf.append("(");
                strBuf.append(firstTabVar).append(" = ").append(secondTabVar);
                strBuf.append(" OR ");
                strBuf.append(firstTabVar).append(" IS NULL");
                strBuf.append(")");
            }
            else
            {
                strBuf.append(firstTabVar).append(" = ").append(secondTabVar);
            }
        }

        if(first)
            return "1";

        return strBuf.toString();
    }


    private String buildInnerJoinProjection(LinkedHashSet<String> allUsedVars,
            HashMap<String, PropertyDirection> joinVariables, String prevTmpTableName, String currTmpTableName)
    {
        StringBuilder strBuf = new StringBuilder();
        boolean firstVar = true;

        for(String var : allUsedVars)
        {
            if(!firstVar)
                strBuf.append(", ");
            else
                firstVar = false;

            PropertyDirection unboundProperty = joinVariables.get(var);

            if(unboundProperty != null) // var is shared between the two tables
            {
                String leftVar = prevTmpTableName + ".\"" + var + "\"";
                String rightVar = currTmpTableName + ".\"" + var + "\"";

                if(!useCorrectJoin)
                {
                    strBuf.append(leftVar).append(" AS ");
                }
                else if(unboundProperty == PropertyDirection.BOTH)
                {
                    strBuf.append("COALESCE(").append(leftVar).append(", ").append(rightVar).append(") AS ");
                }
                else if(unboundProperty == PropertyDirection.LEFT)
                {
                    strBuf.append(rightVar).append(" AS ");
                }
                else if(unboundProperty == PropertyDirection.RIGHT || unboundProperty == PropertyDirection.NONE)
                {
                    strBuf.append(leftVar).append(" AS ");
                }
            }

            strBuf.append("\"").append(var).append("\"");
        }

        return strBuf.toString();
    }


    /**
     * Constructs the ON condition for the INNER JOINs of two tables.
     *
     * @param joinVariables Shared variables between the two tables.
     * @param prevTmpTableName Name of the first table to be joined.
     * @param currTmpTableName Name of the second table to be joined.
     * @return String with the condition that is used in the SQL WHERE clause.
     */
    private String buildInnerJoinOnCondition(HashMap<String, PropertyDirection> joinVariables, String prevTmpTableName,
            String currTmpTableName)
    {
        if(joinVariables.isEmpty())
            return "1";

        StringBuilder strBuf = new StringBuilder();
        boolean first = true;

        for(Entry<String, PropertyDirection> joinVar : joinVariables.entrySet())
        {
            String varStr = joinVar.getKey();
            PropertyDirection varUnbound = joinVar.getValue();

            if(!first)
                strBuf.append(" AND ");
            else
                first = false;

            strBuf.append("( ");

            String leftVar = prevTmpTableName + ".\"" + varStr + "\"";
            String rightVar = currTmpTableName + ".\"" + varStr + "\"";

            strBuf.append(leftVar).append(" = ").append(rightVar);

            if(useCorrectJoin)
            {
                if(varUnbound == PropertyDirection.LEFT || varUnbound == PropertyDirection.BOTH)
                    strBuf.append(" OR ").append(leftVar).append(" IS NULL");

                if(varUnbound == PropertyDirection.RIGHT || varUnbound == PropertyDirection.BOTH)
                    strBuf.append(" OR ").append(rightVar).append(" IS NULL");
            }

            strBuf.append(" )");
        }

        return strBuf.toString();
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


    /**
     * Translates an expression (delegated to the ExpressionTranslatorEx).
     *
     * @param expression Expression to be translated.
     * @return Translated expression.
     */
    private String translateLiteralNodeExpression(Expression expression)
    {
        ExpressionTranslator exprTranslator = new ExpressionTranslator(exceptions, warnings, null);

        return exprTranslator.translate(expression);
    }


    /*
    private String translateLiteralNodeExpression(Expression expression, EnumSet<ExpressionTranslateFlag> translateModes)
    {
        ExpressionTranslator exprTranslator = new ExpressionTranslator(exceptions, warnings, translateModes);

        return exprTranslator.translate(expression);
    }
    */

    private String translateExpression(Expression expression, EnumSet<ExpressionTranslateFlag> translateModes,
            TranslatedSegment context)
    {
        HashMap<String, String> variablesMap = new HashMap<>();

        for(String var : context.subqueryVars)
            variablesMap.put(var, context.sqlTableName + ".\"" + var + "\"");

        return translateExpression(expression, translateModes, variablesMap, context.possibleUnboundVars);
    }


    private String translateExpression(Expression expression, EnumSet<ExpressionTranslateFlag> translateModes,
            HashMap<String, String> variablesMap, final LinkedHashSet<String> possibleUnboundedVars)
    {
        return new ExpressionTranslator(exceptions, warnings, translateModes)
        {
            @Override
            public String visit(ExistsExpression existsExpression)
            {
                TranslatedSegment existInPattern = existsExpression.getPattern().accept(SparqlTranslateVisitor.this);

                if(existInPattern == null)
                {
                    if(existsExpression.isNegated())
                        return "0";
                    else
                        return "1";
                }

                if(existInPattern.isSparql)
                {
                    existInPattern = wrapAndNameTable(wrapSparqlSelect(existInPattern));
                    existInPattern.str = indentBlock(existInPattern.str, 1);
                }


                boolean filterMode = !translateFlags.contains(ExpressionTranslateFlag.SPECIAL_RELATION_OPERATORS);

                return buildFilterExistConstraint(variables, possibleUnboundedVars, existInPattern, filterMode,
                        existsExpression.isNegated());
            }
        }.setVariables(variablesMap).translate(expression);
    }

    //-------------------------------------------------------------------------


    /**
     * Collects shared variables between the two translated segments, that will be used for a join.
     *
     * @param seg1 The first translated segment.
     * @param seg2 The second translated segment.
     * @return Collection of variables that are shared between the joining tables.
     */
    private HashMap<String, PropertyDirection> getJoinVariables(TranslatedSegment seg1, TranslatedSegment seg2)
    {
        HashMap<String, PropertyDirection> listVar = new HashMap<>();

        LinkedHashSet<String> varsFirst = seg1.subqueryVars;
        LinkedHashSet<String> unboundVarsFirst = seg1.possibleUnboundVars;
        LinkedHashSet<String> varsSecond = seg2.subqueryVars;
        LinkedHashSet<String> unboundVarsSecond = seg2.possibleUnboundVars;

        if(varsFirst == null || varsSecond == null || varsFirst.isEmpty() || varsSecond.isEmpty())
            return listVar;

        for(String var : varsFirst)
        {
            if(varsSecond.contains(var))
            {
                // var is a shared variable - is it potentially unbound on
                // either side?

                PropertyDirection property = PropertyDirection.NONE;
                if(unboundVarsFirst != null && unboundVarsFirst.contains(var))
                    property = PropertyDirection.LEFT;
                if(unboundVarsSecond != null && unboundVarsSecond.contains(var))
                {
                    if(property == PropertyDirection.LEFT)
                        property = PropertyDirection.BOTH;
                    else
                        property = PropertyDirection.RIGHT;
                }

                listVar.put(var, property);
            }
        }

        return listVar;
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


    private boolean containsProcedureCall(Expression expression)
    {
        if(expression == null)
            return false;

        Boolean ret = new ElementVisitor<Boolean>()
        {
            @Override
            public Boolean visit(ProcedureCall par)
            {
                return true;
            }

            @Override
            public Boolean visit(MultiProcedureCall par)
            {
                return true;
            }

            @Override
            protected Boolean aggregateResult(List<Boolean> results)
            {
                return results.stream().anyMatch(x -> (x != null && x));
            }

        }.visitElement(expression);

        return ret != null && ret;
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
     * Constructs SQL dummy table.
     *
     * @return String with SQL dummy table.
     */
    private String getDummyTableStr()
    {
        StringBuilder strBuf = new StringBuilder();
        newLine(strBuf);
        strBuf.append("(SELECT 1 AS \"").append(getNewTmpVarName()).append("\") AS ").append(getNewTmpTabName());
        return strBuf.toString();
    }


    /**
     * Gets a new temporary table name.
     *
     * @return New temp table name.
     */
    private String getNewTmpTabName()
    {
        return tempTabPrefix + (uniqueTabID <= 9 ? "0" : "") + Integer.toString(uniqueTabID++);
    }


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

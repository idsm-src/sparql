package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLtrLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfRtlLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.estimateAsUnion;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeInSegmentClass;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.request.Result.ResultType;
import cz.iocb.sparql.engine.translator.ResultHandler;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.ServiceRuntimeException;
import cz.iocb.sparql.engine.translator.StoredResultHandler;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Placeholder of a federated SERVICE call. During the first optimisation pass it only tracks its bindings; in the
 * second pass ({@code evalServices}) {@link #eval} sends the pattern to the remote endpoint and the stub is replaced by
 * the received solutions. It cannot be translated to SQL itself.
 */
public final class SqlServiceStub extends SqlIntercode
{
    /**
     * State shared by the copies of one stub created during optimisation.
     */
    private static class SharedState
    {
        /**
         * Evaluated results by context.
         */
        public Map<SqlIntercode, SqlIntercode> results = new HashMap<>();


        /**
         * Creates an empty state.
         */
        SharedState()
        {
        }

    }


    /**
     * SAX handler of a SPARQL 1.2 XML results document. It collects the received solutions (merged with the context
     * solution they were asked for) into a {@link ResultHandler}, reading IRIs, blank nodes, literals (with a language
     * tag, a base direction or a datatype) and triple terms, the latter nested to any depth.
     */
    private static final class ResultsHandler extends DefaultHandler
    {
        /**
         * Components of a triple term being read.
         */
        private static final class Triple
        {
            /**
             * Subject, predicate and object.
             */
            final RdfTerm[] components = new RdfTerm[3];

            /**
             * Index of the component being read.
             */
            int position;


            /**
             * Creates the triple with no components read yet.
             */
            Triple()
            {
            }
        }


        /**
         * Namespace of the {@code xml:lang} attribute.
         */
        private static final String xmlNamespace = "http://www.w3.org/XML/1998/namespace";

        /**
         * Namespace of the {@code its:dir} attribute.
         */
        private static final String itsNamespace = "http://www.w3.org/2005/11/its";

        /**
         * Variables of the pattern by their name in the sent query.
         */
        private final Map<String, Variable> serviceVariables;

        /**
         * Context solution the received solutions are merged with.
         */
        private final Map<Variable, RdfTerm> defaultResult;

        /**
         * Collector of the solutions.
         */
        private final ResultHandler results;

        /**
         * Prefix of the received blank node labels, unique per call.
         */
        private final String blankNodePrefix;

        /**
         * Segment of the received blank nodes.
         */
        private final int segment;

        /**
         * Variables bound in the current solution.
         */
        private final Set<Variable> used = new HashSet<>();

        /**
         * The current solution.
         */
        private final Map<Variable, RdfTerm> result = new HashMap<>();

        /**
         * Triple terms being read, innermost last.
         */
        private final Deque<Triple> triples = new ArrayDeque<>();

        /**
         * Whether the current solution is inconsistent with the context and is to be dropped.
         */
        private boolean skip;

        /**
         * Variable of the binding being read, null outside a binding.
         */
        private Variable variable;

        /**
         * Text of the term being read, null outside a term.
         */
        private StringBuilder data;

        /**
         * Datatype attribute of the literal being read.
         */
        private String datatype;

        /**
         * Language tag attribute of the literal being read.
         */
        private String lang;

        /**
         * Base direction attribute of the literal being read; null when absent or empty.
         */
        private String direction;


        /**
         * Creates the handler.
         *
         * @param serviceVariables variables of the pattern by their name in the sent query
         * @param defaultResult context solution the received solutions are merged with
         * @param results collector of the solutions
         * @param blankNodePrefix prefix of the received blank node labels
         * @param segment segment of the received blank nodes
         */
        ResultsHandler(Map<String, Variable> serviceVariables, Map<Variable, RdfTerm> defaultResult,
                ResultHandler results, String blankNodePrefix, int segment)
        {
            this.serviceVariables = serviceVariables;
            this.defaultResult = defaultResult;
            this.results = results;
            this.blankNodePrefix = blankNodePrefix;
            this.segment = segment;
        }


        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if(localName.equalsIgnoreCase("result"))
            {
                skip = false;
                used.clear();
                result.clear();
                result.putAll(defaultResult);
                triples.clear();

                if(results.size() >= serviceResultLimit)
                    throw new SAXException();
            }
            else if(localName.equalsIgnoreCase("binding"))
            {
                variable = serviceVariables.get(attributes.getValue("name"));
            }
            else if(localName.equalsIgnoreCase("literal"))
            {
                lang = attributes.getValue(xmlNamespace, "lang");
                direction = attributes.getValue(itsNamespace, "dir");
                datatype = attributes.getValue("datatype");
                data = new StringBuilder();

                if(direction != null && direction.isEmpty())
                    direction = null;
            }
            else if(localName.equalsIgnoreCase("uri") || localName.equalsIgnoreCase("bnode"))
            {
                data = new StringBuilder();
            }
            else if(localName.equalsIgnoreCase("triple"))
            {
                triples.push(new Triple());
            }
            else if(localName.equalsIgnoreCase("subject") && !triples.isEmpty())
            {
                triples.peek().position = 0;
            }
            else if(localName.equalsIgnoreCase("predicate") && !triples.isEmpty())
            {
                triples.peek().position = 1;
            }
            else if(localName.equalsIgnoreCase("object") && !triples.isEmpty())
            {
                triples.peek().position = 2;
            }
        }


        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            if(localName.equalsIgnoreCase("result"))
            {
                if(skip)
                    return;

                try
                {
                    results.add(result);
                }
                catch(SQLException e)
                {
                    throw new SQLRuntimeException(e);
                }
            }
            else if(localName.equalsIgnoreCase("binding"))
            {
                variable = null;
            }
            else if(localName.equalsIgnoreCase("uri"))
            {
                addTerm(new Iri(takeData()));
            }
            else if(localName.equalsIgnoreCase("bnode"))
            {
                addTerm(new StrBlankNode(blankNodePrefix + takeData(), segment));
            }
            else if(localName.equalsIgnoreCase("literal"))
            {
                String value = takeData();

                if(direction != null)
                    addTerm(new DirLangStringLiteral(value, lang, parseDirection()));
                else if(lang != null)
                    addTerm(new LangStringLiteral(value, lang));
                else if(datatype != null)
                    addTerm(new TypedLiteral(value, new Iri(datatype)));
                else
                    addTerm(new TypedLiteral(value, xsdStringIri));
            }
            else if(localName.equalsIgnoreCase("triple") && !triples.isEmpty())
            {
                RdfTerm[] components = triples.pop().components;

                if(components[0] == null || components[1] == null || components[2] == null)
                    return; // should not happen if the response is valid

                addTerm(new TripleTerm(components[0], components[1], components[2]));
            }
        }


        @Override
        public void characters(char ch[], int start, int length)
        {
            if(data != null)
                data.append(ch, start, length);
        }


        /**
         * Base direction of the literal being read, which has to be {@code ltr} or {@code rtl} and has to come with a
         * language tag.
         *
         * @return base direction of the literal being read
         * @throws SAXException if the direction is not valid or the literal has no language tag
         */
        private Direction parseDirection() throws SAXException
        {
            Direction result = Direction.fromText(direction);

            if(result == null)
                throw new SAXException("invalid base direction '" + direction + "'");

            if(lang == null)
                throw new SAXException("base direction without language tag");

            return result;
        }


        /**
         * Text of the term just read; the collection of text is stopped.
         *
         * @return text of the term just read
         */
        private String takeData()
        {
            String value = data.toString();
            data = null;

            return value;
        }


        /**
         * Places the term just read: as a component of the innermost triple term being read, or as the value of the
         * current binding. A binding of a variable that the context binds to a blank node or to a different term makes
         * the solution inconsistent, and it is dropped.
         *
         * @param term the term
         */
        private void addTerm(RdfTerm term)
        {
            if(!triples.isEmpty())
            {
                Triple triple = triples.peek();
                triple.components[triple.position] = term;
                return;
            }

            if(variable == null)
                return; // should not happen if the response is valid

            if(!used.add(variable))
                return; // should not happen if the response is valid

            if(defaultResult.get(variable) instanceof BlankNode)
            {
                skip = true;
                return;
            }

            if(defaultResult.containsKey(variable) && !defaultResult.get(variable).equals(term))
            {
                // should not happen if the response is correct
                skip = true;
                return;
            }

            result.put(variable, term);
        }
    }


    /**
     * User-Agent header sent to remote endpoints.
     */
    private static final String userAgent;

    /**
     * Maximum number of HTTP redirects followed.
     */
    private static final int serviceRedirectLimit = 3;

    /**
     * Maximum number of context solutions sent in one call.
     */
    private static final int serviceContextLimit = 1000;

    /**
     * Maximum number of solutions accepted from the endpoint.
     */
    private static final int serviceResultLimit = 10000000;

    /**
     * Solutions the call is evaluated for.
     */
    private final SqlIntercode context;

    /**
     * Endpoint IRI.
     */
    private final RdfTerm name;

    /**
     * Variables of the pattern by their name in the sent query.
     */
    private final Map<String, Variable> serviceVariables;

    /**
     * SPARQL text of the pattern.
     */
    private final String serviceCode;

    /**
     * SILENT modifier.
     */
    private final boolean silent;

    /**
     * Class of the blank nodes received from the endpoint.
     */
    private final StrBlankNodeInSegmentClass blankNodeClass;

    /**
     * State shared with the other copies of the stub.
     */
    private final SharedState state;


    static
    {
        String version = SqlServiceStub.class.getPackage().getImplementationVersion();

        if(version == null)
            version = "devel";

        userAgent = "IDSM SPARQL engine/" + version
                + " (https://github.com/idsm-src/sparql; jakub.galgonek@uochb.cas.cz)";
    }


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param name the endpoint IRI
     * @param serviceCode SPARQL text of the pattern
     * @param serviceVariables variables of the pattern by their name in the sent query
     * @param context solutions the call is evaluated for
     * @param blankNodeClass the blank node class
     * @param silent the SILENT modifier
     * @param state state shared by the copies of the stub
     */
    protected SqlServiceStub(VariableBindings bindings, RdfTerm name, String serviceCode,
            Map<String, Variable> serviceVariables, SqlIntercode context, StrBlankNodeInSegmentClass blankNodeClass,
            boolean silent, SharedState state)
    {
        super(bindings, false);

        this.context = context;
        this.name = name;
        this.serviceCode = serviceCode;
        this.serviceVariables = serviceVariables;
        this.silent = silent;
        this.blankNodeClass = blankNodeClass;
        this.state = state;
    }


    /**
     * Stub with fresh shared state; the received values may take any configured class.
     *
     * @param request the current request
     * @param name the endpoint IRI
     * @param serviceCode SPARQL text of the pattern
     * @param serviceVariables variables of the pattern by their name in the sent query
     * @param context solutions the call is evaluated for
     * @param blankNodeClass the blank node class
     * @param silent the SILENT modifier
     * @return stub with fresh shared state; the received values may take any configured class
     */
    public static SqlIntercode create(Request request, RdfTerm name, String serviceCode,
            Map<String, Variable> serviceVariables, SqlIntercode context, StrBlankNodeInSegmentClass blankNodeClass,
            boolean silent)
    {
        return create(request, name, serviceCode, serviceVariables, context, blankNodeClass, silent, null,
                new SharedState());
    }


    /**
     * Stub with the given shared state, exposing only what the parent needs.
     *
     * @param request the current request
     * @param name the endpoint IRI
     * @param serviceCode SPARQL text of the pattern
     * @param serviceVariables variables of the pattern by their name in the sent query
     * @param context solutions the call is evaluated for
     * @param blankNodeClass the blank node class
     * @param silent the SILENT modifier
     * @param restrictions what the parent needs of the variables
     * @param state state shared by the copies of the stub
     * @return stub with the given shared state, exposing only what the parent needs
     */
    protected static SqlIntercode create(Request request, RdfTerm name, String serviceCode,
            Map<String, Variable> serviceVariables, SqlIntercode context, StrBlankNodeInSegmentClass blankNodeClass,
            boolean silent, Restrictions restrictions, SharedState state)
    {
        ClassRelations relations = request.getConfiguration();

        Set<ResourceClass> resourceClasses = new HashSet<>();
        resourceClasses.add(unsupportedIri);
        resourceClasses.add(unsupportedType);
        resourceClasses.add(rdfLangString);
        resourceClasses.add(rdfLtrLangString);
        resourceClasses.add(rdfRtlLangString);
        resourceClasses.add(blankNodeClass);

        request.getConfiguration().getIriClasses().forEach(c -> resourceClasses.add(c));
        request.getConfiguration().getDatatypes()
                .forEach(d -> resourceClasses.addAll(estimateAsUnion(d.getBaseLiteralClass())));

        VariableBindings bindings = new VariableBindings();

        for(VariableBinding v : context.getVariableBindings().getValues())
        {
            if(!v.canBeNull())
            {
                Variable var = v.getVariable();
                VariableBinding binding = new VariableBinding(var, v.canBeNull());

                for(ResourceClass c : resourceClasses)
                    binding.addMapping(c, c.createColumns(request.getColumnMap(), var));

                bindings.add(binding);
            }
        }

        for(Variable var : serviceVariables.values())
        {
            if(!bindings.getVariables().contains(var))
            {
                VariableBinding binding = new VariableBinding(var, true);

                for(ResourceClass c : resourceClasses)
                    binding.addMapping(c, c.createColumns(request.getColumnMap(), var));

                bindings.add(binding);
            }
        }

        return new SqlServiceStub(bindings.restrict(relations, restrictions), name, serviceCode, serviceVariables,
                context, blankNodeClass, silent, state);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        if(evalServices)
            return eval(request, context, restrictions);


        Restrictions contextRestrictions = new Restrictions(restrictions);

        for(Variable var : serviceVariables.values())
            contextRestrictions.add(var);

        SqlIntercode optContext = context.optimize(request, contextRestrictions, reduced, evalServices);


        if(optContext.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(name instanceof Variable variable)
        {
            VariableBinding binding = optContext.getVariableBindings().get(new Variable(variable.getName()));

            if(binding == null || binding.getCompatibleClasses(relations, iri).isEmpty())
                return SqlNoSolution.get();
        }

        if(optContext instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
                childs.add(create(request, name, serviceCode, serviceVariables, child, blankNodeClass, silent,
                        restrictions, state));

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        if(optContext == context && restrictions.isOptimized(relations, bindings))
            return this;

        return create(request, name, serviceCode, serviceVariables, optContext, blankNodeClass, silent, restrictions,
                state);
    }


    @Override
    public String translate(Request request)
    {
        throw new RuntimeException();
    }


    /**
     * Evaluates the call for the solutions of {@code context}: the SPARQL text extended by VALUES of the shared
     * variables is sent over the SPARQL protocol (in chunks when the context is large), the XML results are collected
     * by a {@link StoredResultHandler}, and the result joined with the context is returned. Results are cached per
     * context in the state shared by the stubs of one call; a failing endpoint is an error unless the service is
     * SILENT.
     *
     * @param request the current request
     * @param context solutions the call is evaluated for
     * @param restrictions what the parent needs of the variables
     * @return the received solutions joined with the context
     */
    public SqlIntercode eval(Request request, SqlIntercode context, Restrictions restrictions)
    {
        if(state.results.containsKey(context))
        {
            System.err.println("use result from state");
            return state.results.get(context);
        }


        /* create variable lists */

        Set<Variable> contextVariables = context.getVariableBindings().getVariables();
        Set<Variable> mergedVariables = new HashSet<>(contextVariables);
        Set<String> sharedVariables = new HashSet<>();

        for(Entry<String, Variable> e : serviceVariables.entrySet())
        {
            mergedVariables.add(e.getValue());

            if(contextVariables.contains(e.getValue()))
                sharedVariables.add(e.getKey());
        }


        List<RdfTerm[]> rows = new ArrayList<>();
        Map<Variable, Integer> varIndexes = null;

        if(context.equals(SqlEmptySolution.get()))
        {
            rows.add(new RdfTerm[0]);
            varIndexes = new HashMap<>();
        }
        else
        {
            /* evaluate context pattern */

            BigInteger limit = BigInteger.valueOf(serviceContextLimit + 1);
            List<Variable> vars = new ArrayList<>(contextVariables);
            SqlSelect query = SqlSelect.createTopLevel(request, vars, context, null, limit).optimize(request, true);

            String code = query.translate(request);

            try(Result result = new Result(ResultType.SELECT, query.getResultDescription(),
                    request.getStatement().executeQuery(code), request.getBegin(), request.getTimeout()))
            {
                varIndexes = result.getVariableIndexes();

                while(result.next())
                {
                    rows.add(result.getRow());

                    if(rows.size() > serviceContextLimit)
                    {
                        //TODO: use failback variant
                        throw new ServiceException(MessageType.serviceContextLimitExceeded.getText());
                    }
                }
            }
            catch(ServiceException e)
            {
                throw new ServiceRuntimeException(e);
            }
            catch(SQLException e)
            {
                throw new SQLRuntimeException(e);
            }
        }


        /* create result */

        try(ResultHandler results = new StoredResultHandler(request, restrictions))
        {
            int call = 0;

            for(RdfTerm[] row : rows)
            {
                String endpoint = null;

                if(name instanceof Iri iri)
                    endpoint = iri.getValue();
                else if(row[varIndexes.get(name)] instanceof Iri iri)
                    endpoint = iri.getValue();
                else
                    continue;


                /* build default result */

                Map<Variable, RdfTerm> defaultResult = new HashMap<>();

                for(Variable variable : contextVariables)
                {
                    Integer idx = varIndexes.get(variable);

                    if(idx != null)
                        defaultResult.put(variable, row[idx]);
                }


                /* build service query */

                StringBuilder sparqlQueryBuilder = new StringBuilder();
                sparqlQueryBuilder.append("select * where ");
                sparqlQueryBuilder.append(serviceCode);

                if(!sharedVariables.isEmpty())
                {
                    sparqlQueryBuilder.append("values (");

                    for(String var : sharedVariables)
                        sparqlQueryBuilder.append(" ?").append(var);

                    sparqlQueryBuilder.append(") {(");

                    for(String variable : sharedVariables)
                    {
                        RdfTerm term = row[varIndexes.get(serviceVariables.get(variable))];

                        if(term != null && isDataTerm(term))
                            sparqlQueryBuilder.append(term).append(" ");
                        else
                            sparqlQueryBuilder.append("undef ");
                    }

                    sparqlQueryBuilder.append(")}");
                }


                /* open connection */

                HttpURLConnection connection = null;

                try
                {
                    String url = endpoint;

                    for(int i = 0; i <= serviceRedirectLimit && url != null; i++)
                    {
                        connection = (HttpURLConnection) (new URI(url)).toURL().openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("content-type",
                                "application/x-www-form-urlencoded; charset=UTF-8");
                        connection.setRequestProperty("user-agent", userAgent);
                        connection.setRequestProperty("accept", "application/sparql-results+xml");
                        connection.setDoOutput(true);

                        try(OutputStream out = connection.getOutputStream())
                        {
                            out.write(
                                    ("query=" + URLEncoder.encode(sparqlQueryBuilder.toString(), "UTF-8")).getBytes());
                        }

                        url = connection.getHeaderField("Location");
                    }

                    if(connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                        throw new IOException(connection.getResponseMessage());
                }
                catch(IOException | URISyntaxException e)
                {
                    e.printStackTrace();

                    if(!silent)
                        throw new ServiceException(String.format(MessageType.badServiceEndpoint.getText(), endpoint));

                    return context;
                }


                /* receive result */

                try
                {
                    ResultsHandler handler = new ResultsHandler(serviceVariables, defaultResult, results, call++ + "r",
                            blankNodeClass.getSegment());

                    try(InputStream input = connection.getInputStream())
                    {
                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        factory.setNamespaceAware(true);
                        SAXParser saxParser = factory.newSAXParser();
                        saxParser.parse(input, handler);
                    }
                }
                catch(ParserConfigurationException | SAXException | IOException e)
                {
                    if(e instanceof SAXException && results.size() >= serviceResultLimit)
                    {
                        throw new ServiceException(
                                String.format(MessageType.serviceResultLimitExceeded.getText(), endpoint));
                    }
                    else
                    {
                        e.printStackTrace();
                        throw new ServiceException(String.format(MessageType.badServiceEndpoint.getText(), endpoint));
                    }
                }
            }

            SqlIntercode result = results.get();

            if(context.isDeterministic())
                state.results.put(context, result);

            return result;
        }
        catch(ServiceException e)
        {
            throw new ServiceRuntimeException(e);
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    /**
     * True if the term can be written as a constant of a VALUES clause: an IRI, a literal, or a triple term whose
     * subject and predicate are IRIs and whose object is such a term again (blank nodes and variables cannot be sent).
     *
     * @param term the RDF term
     * @return true if the term can be written as a constant of a VALUES clause, false otherwise
     */
    private static boolean isDataTerm(RdfTerm term)
    {
        if(term instanceof Iri || term instanceof Literal)
            return true;

        return term instanceof TripleTerm triple && triple.getSubject() instanceof Iri
                && triple.getPredicate() instanceof Iri && isDataTerm(triple.getObject());
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return true;
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        return context.getVirtualTables();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("service");

        String[] lines = serviceCode.split("\n");

        for(String line : lines)
        {
            indentInfo(builder, indent, true);
            builder.append(line);
        }

        indentChild(builder, indent, true);
        context.generateExplanation(builder, getIndent(indent, true));
    }


    /**
     * Variables of the SERVICE pattern by their name in the sent query.
     *
     * @return variables of the SERVICE pattern by their name in the sent query
     */
    public Map<String, Variable> getServiceVariables()
    {
        return serviceVariables;
    }


    /**
     * SPARQL text of the SERVICE pattern.
     *
     * @return SPARQL text of the SERVICE pattern
     */
    public String getServiceCode()
    {
        return serviceCode;
    }


    /**
     * Solutions the call is evaluated for.
     *
     * @return solutions the call is evaluated for
     */
    public SqlIntercode getContext()
    {
        return context;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlServiceStub imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(silent, imcode.silent))
            return false;

        if(!Objects.equals(name, imcode.name))
            return false;

        if(!Objects.equals(blankNodeClass, imcode.blankNodeClass))
            return false;

        if(!Objects.equals(serviceCode, imcode.serviceCode))
            return false;

        if(!Objects.equals(serviceVariables, imcode.serviceVariables))
            return false;

        if(!Objects.equals(context, imcode.context))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(silent, name, blankNodeClass, serviceCode, serviceVariables, context);
    }
}

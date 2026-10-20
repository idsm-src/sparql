package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLtrLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfRtlLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.tripleTerm;
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
import java.util.LinkedHashMap;
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
import cz.iocb.sparql.engine.config.ServiceSettings;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeInSegmentClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
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
     * One request to an endpoint; the context solutions asking the same query of the same endpoint share it.
     *
     * @param endpoint URL of the endpoint
     * @param query the SPARQL query sent
     */
    private static record Call(String endpoint, String query)
    {
    }


    /**
     * SAX handler of a SPARQL 1.2 XML results document. It collects the received solutions, each merged with every
     * context solution of the request consistent with it, into a {@link ResultHandler}, reading IRIs, blank nodes,
     * literals (with a language tag, a base direction or a datatype) and triple terms, the latter nested to any depth.
     * The received typed literals are brought to their canonical forms when asked for.
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
         * Current request, whose configuration knows the datatypes of the received literals.
         */
        private final Request request;

        /**
         * Variables of the pattern by their name in the sent query.
         */
        private final Map<String, Variable> serviceVariables;

        /**
         * Context solutions that asked the query of the request, which the received solutions are merged with.
         */
        private final List<Map<Variable, RdfTerm>> contexts;

        /**
         * Collector of the solutions.
         */
        private final ResultHandler results;

        /**
         * Prefix of the received blank node labels, unique per request.
         */
        private final String blankNodePrefix;

        /**
         * Segment of the received blank nodes.
         */
        private final int segment;

        /**
         * Whether the received typed literals are brought to their canonical forms.
         */
        private final boolean canonicalize;

        /**
         * Maximum number of solutions the collector may hold.
         */
        private final int limit;

        /**
         * Variables bound in the received solution being read.
         */
        private final Set<Variable> used = new HashSet<>();

        /**
         * The received solution being read.
         */
        private final Map<Variable, RdfTerm> result = new HashMap<>();

        /**
         * Triple terms being read, innermost last.
         */
        private final Deque<Triple> triples = new ArrayDeque<>();

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
         * @param request the current request
         * @param serviceVariables variables of the pattern by their name in the sent query
         * @param contexts context solutions that asked the query of the request
         * @param results collector of the solutions
         * @param blankNodePrefix prefix of the received blank node labels
         * @param segment segment of the received blank nodes
         * @param canonicalize whether the received typed literals are brought to their canonical forms
         * @param limit maximum number of solutions the collector may hold
         */
        ResultsHandler(Request request, Map<String, Variable> serviceVariables, List<Map<Variable, RdfTerm>> contexts,
                ResultHandler results, String blankNodePrefix, int segment, boolean canonicalize, int limit)
        {
            this.request = request;
            this.serviceVariables = serviceVariables;
            this.contexts = contexts;
            this.results = results;
            this.blankNodePrefix = blankNodePrefix;
            this.segment = segment;
            this.canonicalize = canonicalize;
            this.limit = limit;
        }


        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if(localName.equalsIgnoreCase("result"))
            {
                used.clear();
                result.clear();
                triples.clear();
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
                for(Map<Variable, RdfTerm> context : contexts)
                {
                    if(!isConsistent(context))
                        continue;

                    if(results.size() >= limit)
                        throw new SAXException();

                    Map<Variable, RdfTerm> solution = new HashMap<>(context);
                    solution.putAll(result);

                    try
                    {
                        results.add(solution);
                    }
                    catch(SQLException e)
                    {
                        throw new SQLRuntimeException(e);
                    }
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
                else
                    addTerm(typedLiteral(value, datatype != null ? new Iri(datatype) : xsdStringIri));
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
         * Typed literal of the lexical form, brought to the canonical form of its datatype when canonical literals are
         * asked for, the datatype is known to the configuration and the form is valid.
         *
         * @param value the lexical form
         * @param type the datatype IRI
         * @return typed literal of the lexical form
         */
        private TypedLiteral typedLiteral(String value, Iri type)
        {
            if(canonicalize)
            {
                Datatype datatype = request.getConfiguration().getDatatype(type);

                if(datatype != null && datatype.isValidForm(value) && !datatype.isCanonicalForm(value))
                    return new TypedLiteral(datatype.getCanonicalLexicalForm(value), type);
            }

            return new TypedLiteral(value, type);
        }


        /**
         * True if the received solution is consistent with the context solution: a variable bound by both has the same
         * term in both, and a variable the context binds to a blank node is not bound by the received solution, as a
         * blank node of this endpoint never equals a term of a remote one.
         *
         * @param context the context solution
         * @return true if the received solution is consistent with the context solution, false otherwise
         */
        private boolean isConsistent(Map<Variable, RdfTerm> context)
        {
            for(Entry<Variable, RdfTerm> binding : result.entrySet())
            {
                RdfTerm bound = context.get(binding.getKey());

                if(bound instanceof BlankNode || bound != null && !bound.equals(binding.getValue()))
                    return false;
            }

            return true;
        }


        /**
         * Places the term just read: as a component of the innermost triple term being read, or as the value of the
         * current binding (a repeated binding of a variable is ignored).
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

            result.put(variable, term);
        }
    }


    /**
     * User-Agent header sent to remote endpoints.
     */
    private static final String userAgent;

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
        resourceClasses.add(tripleTerm);
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


        Restrictions contextRestrictions = getContextRestrictions(restrictions);

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
     * Restrictions for the context of the call: what the parent needs of the variables, extended by the variables of
     * the call, which the context may bind as well, and by the variable naming the endpoint, whose value each context
     * solution has to supply.
     *
     * @param restrictions what the parent needs of the variables
     * @return restrictions for the context of the call
     */
    private Restrictions getContextRestrictions(Restrictions restrictions)
    {
        Restrictions contextRestrictions = new Restrictions(restrictions);

        for(Variable var : serviceVariables.values())
            contextRestrictions.add(var);

        if(name instanceof Variable variable)
            contextRestrictions.add(variable);

        return contextRestrictions;
    }


    /**
     * Evaluates the call for the solutions of {@code context}: the SPARQL text, extended by a VALUES clause with the
     * values of the shared variables that the settings of the endpoint allow to send, is sent over the SPARQL protocol
     * for each context solution, the context solutions asking the same query of the same endpoint sharing one request
     * (within this evaluation; the members of a union context are evaluated by stubs of their own), the XML results are
     * collected by a {@link StoredResultHandler}, and the result joined with the context is returned. Results are
     * cached per context in the state shared by the stubs of one call; a failing endpoint is an error unless the
     * service is SILENT, when the call yields the solutions of the context with the variables of the call unbound. The
     * context and result limits are those of the endpoint when the call names it, and the defaults of the configuration
     * when the endpoint is a variable; the other settings are those of the endpoint of each request.
     *
     * @param request the current request
     * @param context solutions the call is evaluated for
     * @param restrictions what the parent needs of the variables
     * @return the received solutions joined with the context
     */
    public SqlIntercode eval(Request request, SqlIntercode context, Restrictions restrictions)
    {
        if(state.results.containsKey(context))
            return state.results.get(context);


        SparqlDatabaseConfiguration config = request.getConfiguration();
        ServiceSettings settings = name instanceof Iri iri ? config.getServiceSettings(iri) :
                config.getServiceSettings();


        /* create variable lists */

        Set<Variable> contextVariables = context.getVariableBindings().getVariables();
        List<String> sharedVariables = new ArrayList<>();

        for(Entry<String, Variable> e : serviceVariables.entrySet())
            if(contextVariables.contains(e.getValue()))
                sharedVariables.add(e.getKey());


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

            BigInteger limit = BigInteger.valueOf(settings.contextLimit() + 1);
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

                    if(rows.size() > settings.contextLimit())
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


        /* group the context solutions by the request they need */

        LinkedHashMap<Call, List<Map<Variable, RdfTerm>>> calls = new LinkedHashMap<>();

        for(RdfTerm[] row : rows)
        {
            String endpoint = null;

            if(name instanceof Iri iri)
                endpoint = iri.getValue();
            else if(row[varIndexes.get(name)] instanceof Iri iri)
                endpoint = iri.getValue();
            else
                continue;


            Map<Variable, RdfTerm> solution = new HashMap<>();

            for(Variable variable : contextVariables)
            {
                Integer idx = varIndexes.get(variable);

                if(idx != null)
                    solution.put(variable, row[idx]);
            }


            boolean literals = config.getServiceSettings(new Iri(endpoint)).sendLiterals();
            String query = buildQuery(sharedVariables, row, varIndexes, literals);

            calls.computeIfAbsent(new Call(endpoint, query), _ -> new ArrayList<>()).add(solution);
        }


        /* create result */

        try(ResultHandler results = new StoredResultHandler(request, restrictions))
        {
            int call = 0;

            for(Entry<Call, List<Map<Variable, RdfTerm>>> entry : calls.entrySet())
            {
                String endpoint = entry.getKey().endpoint();
                String query = entry.getKey().query();
                ServiceSettings endpointSettings = config.getServiceSettings(new Iri(endpoint));


                /* open connection */

                HttpURLConnection connection = null;

                try
                {
                    String url = endpoint;

                    for(int i = 0; i <= endpointSettings.redirectLimit() && url != null; i++)
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
                            out.write(("query=" + URLEncoder.encode(query, "UTF-8")).getBytes());
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

                    // the context may hold earlier calls, which the result must not keep unevaluated
                    return context.optimize(request, getContextRestrictions(restrictions), false, true);
                }


                /* receive result */

                try
                {
                    ResultsHandler handler = new ResultsHandler(request, serviceVariables, entry.getValue(), results,
                            call++ + "r", blankNodeClass.getSegment(), endpointSettings.canonicalizeLiterals(),
                            settings.resultLimit());

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
                    if(e instanceof SAXException && results.size() >= settings.resultLimit())
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
     * Query sent to the endpoint for a context solution: the SPARQL text of the pattern, followed by a VALUES clause
     * binding the shared variables to the values of the solution when some of them can be sent; the others are
     * {@code undef}, and a VALUES clause of nothing but {@code undef} is left out. A blank node or a variable cannot be
     * sent, a literal (and a triple term containing one) only when the endpoint asks for it ({@code literals}).
     *
     * @param sharedVariables names of the variables of the pattern that the context binds
     * @param row the context solution
     * @param varIndexes positions of the variables in the solution
     * @param literals whether literal values are sent
     * @return query sent to the endpoint for the context solution
     */
    private String buildQuery(List<String> sharedVariables, RdfTerm[] row, Map<Variable, Integer> varIndexes,
            boolean literals)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("select * where ");
        builder.append(serviceCode);

        List<String> values = new ArrayList<>();
        boolean bound = false;

        for(String variable : sharedVariables)
        {
            RdfTerm term = row[varIndexes.get(serviceVariables.get(variable))];

            if(term != null && isSendable(term, literals))
            {
                values.add(term.toString());
                bound = true;
            }
            else
            {
                values.add("undef");
            }
        }

        if(bound)
        {
            builder.append("values (");

            for(String variable : sharedVariables)
                builder.append(" ?").append(variable);

            builder.append(") {(");

            for(String value : values)
                builder.append(value).append(" ");

            builder.append(")}");
        }

        return builder.toString();
    }


    /**
     * True if the term can be sent as a constant of a VALUES clause: an IRI, a literal when literals are sent, or a
     * triple term whose subject and predicate are IRIs and whose object is such a term again (blank nodes and variables
     * cannot be sent).
     *
     * @param term the RDF term
     * @param literals whether literals are sent
     * @return true if the term can be sent as a constant of a VALUES clause, false otherwise
     */
    private static boolean isSendable(RdfTerm term, boolean literals)
    {
        if(term instanceof Iri)
            return true;

        if(term instanceof Literal)
            return literals;

        return term instanceof TripleTerm triple && triple.getSubject() instanceof Iri
                && triple.getPredicate() instanceof Iri && isSendable(triple.getObject(), literals);
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

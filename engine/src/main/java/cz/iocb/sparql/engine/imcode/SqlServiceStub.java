package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
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
import java.util.ArrayList;
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
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeConstantSegmentClass;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
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



public final class SqlServiceStub extends SqlIntercode
{
    private static class SharedState
    {
        public Map<SqlIntercode, SqlIntercode> results = new HashMap<>();
    }


    private static final String userAgent;

    private static final int serviceRedirectLimit = 3;
    private static final int serviceContextLimit = 1000;
    private static final int serviceResultLimit = 10000000;

    private final SqlIntercode context;
    private final RdfTerm name;
    private final Map<String, Variable> serviceVariables;
    private final String serviceCode;
    private final boolean silent;
    private final StrBlankNodeConstantSegmentClass blankNodeClass;
    private final SharedState state;


    static
    {
        String version = SqlServiceStub.class.getPackage().getImplementationVersion();

        if(version == null)
            version = "devel";

        userAgent = "IDSM SPARQL engine/" + version
                + " (https://github.com/idsm-src/sparql; jakub.galgonek@uochb.cas.cz)";
    }


    protected SqlServiceStub(VariableBindings bindings, RdfTerm name, String serviceCode,
            Map<String, Variable> serviceVariables, SqlIntercode context,
            StrBlankNodeConstantSegmentClass blankNodeClass, boolean silent, SharedState state)
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


    public static SqlIntercode create(Request request, RdfTerm name, String serviceCode,
            Map<String, Variable> serviceVariables, SqlIntercode context,
            StrBlankNodeConstantSegmentClass blankNodeClass, boolean silent)
    {
        return create(request, name, serviceCode, serviceVariables, context, blankNodeClass, silent, null,
                new SharedState());
    }


    protected static SqlIntercode create(Request request, RdfTerm name, String serviceCode,
            Map<String, Variable> serviceVariables, SqlIntercode context,
            StrBlankNodeConstantSegmentClass blankNodeClass, boolean silent, Restrictions restrictions,
            SharedState state)
    {
        Set<ResourceClass> resourceClasses = new HashSet<>();
        resourceClasses.add(unsupportedIri);
        resourceClasses.add(unsupportedLiteral);
        resourceClasses.add(rdfLangString);
        resourceClasses.add(blankNodeClass);

        request.getConfiguration().getIriClasses().forEach(c -> resourceClasses.add(c));
        request.getConfiguration().getDatatypes().forEach(d -> resourceClasses.add(d.getGeneralLiteralClass()));

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

        return new SqlServiceStub(bindings.restrict(restrictions), name, serviceCode, serviceVariables, context,
                blankNodeClass, silent, state);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
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

            if(binding == null || binding.getCompatibleClasses(iri).isEmpty())
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


        if(optContext == context && restrictions.isOptimized(bindings))
            return this;

        return create(request, name, serviceCode, serviceVariables, optContext, blankNodeClass, silent, restrictions,
                state);
    }


    @Override
    public String translate(Request request)
    {
        throw new RuntimeException();
    }


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

                        if(term != null && (term instanceof Iri || term instanceof Literal))
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
                    final int bnprefix = call++;

                    DefaultHandler handler = new DefaultHandler()
                    {
                        Set<Variable> used = new HashSet<>();
                        Map<Variable, RdfTerm> result = new HashMap<>();

                        boolean skip;
                        Variable variable;
                        StringBuilder data;
                        String datatype;
                        String lang;

                        @Override
                        public void startElement(String uri, String localName, String qName, Attributes attributes)
                                throws SAXException
                        {
                            if(qName.equalsIgnoreCase("result"))
                            {
                                skip = false;
                                used.clear();
                                result.clear();
                                result.putAll(defaultResult);

                                if(results.size() >= serviceResultLimit)
                                    throw new SAXException();
                            }
                            else if(qName.equalsIgnoreCase("binding"))
                            {
                                variable = serviceVariables.get(attributes.getValue("name"));
                            }
                            else if(qName.equalsIgnoreCase("literal"))
                            {
                                lang = attributes.getValue("xml:lang");
                                datatype = attributes.getValue("datatype");
                                data = new StringBuilder();
                            }
                            else if(qName.equalsIgnoreCase("uri") || qName.equalsIgnoreCase("bnode"))
                            {
                                data = new StringBuilder();
                            }
                        }

                        @Override
                        public void endElement(String uri, String localName, String qName) throws SAXException
                        {
                            if(qName.equalsIgnoreCase("result") && !skip)
                            {
                                try
                                {
                                    results.add(result);
                                }
                                catch(SQLException e)
                                {
                                    throw new SQLRuntimeException(e);
                                }
                            }
                            else if(qName.equalsIgnoreCase("binding"))
                            {
                                variable = null;
                            }

                            RdfTerm term = null;

                            if(qName.equalsIgnoreCase("uri"))
                                term = new Iri(data.toString());
                            else if(qName.equalsIgnoreCase("bnode"))
                                term = new StrBlankNode(bnprefix + "r" + data.toString(), blankNodeClass.getSegment());
                            else if(!qName.equalsIgnoreCase("literal"))
                                return;
                            else if(lang != null)
                                term = new LangStringLiteral(data.toString(), lang);
                            else if(datatype != null)
                                term = new TypedLiteral(data.toString(), new Iri(datatype));
                            else
                                term = new TypedLiteral(data.toString(), xsdStringIri);

                            if(variable == null /*|| !serviceVariables.contains(variable)*/)
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

                        @Override
                        public void characters(char ch[], int start, int length)
                        {
                            if(data != null)
                                data.append(new String(ch, start, length));
                        }
                    };

                    try(InputStream input = connection.getInputStream())
                    {
                        SAXParserFactory factory = SAXParserFactory.newInstance();
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


    @Override
    public boolean hasServiceSubpattern()
    {
        return true;
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


    public Map<String, Variable> getServiceVariables()
    {
        return serviceVariables;
    }


    public String getServiceCode()
    {
        return serviceCode;
    }


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

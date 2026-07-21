package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdStringType;
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
import cz.iocb.sparql.engine.mapping.BlankNodeLiteral;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.VarOrIri;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.pattern.GraphPattern;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.request.IriNode;
import cz.iocb.sparql.engine.request.LanguageTaggedLiteral;
import cz.iocb.sparql.engine.request.RdfNode;
import cz.iocb.sparql.engine.request.ReferenceNode;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.request.Result.ResultType;
import cz.iocb.sparql.engine.request.TypedLiteral;
import cz.iocb.sparql.engine.translator.ResultHandler;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.ServiceRuntimeException;
import cz.iocb.sparql.engine.translator.ServiceTranslateVisitor;
import cz.iocb.sparql.engine.translator.StoredResultHandler;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public final class SqlServiceStub extends SqlIntercode
{
    private static class SharedState
    {
        public Map<SqlIntercode, SqlIntercode> results = new HashMap<SqlIntercode, SqlIntercode>();
    }


    private static final String userAgent;

    private static final int serviceRedirectLimit = 3;
    private static final int serviceContextLimit = 1000;
    private static final int serviceResultLimit = 10000000;

    private final SqlIntercode context;
    private final VarOrIri name;
    private final GraphPattern pattern;
    private final boolean silent;
    private final UserStrBlankNodeClass blankNodeClass;
    private final SharedState state;


    static
    {
        String version = SqlServiceStub.class.getPackage().getImplementationVersion();

        if(version == null)
            version = "devel";

        userAgent = "IDSM SPARQL engine/" + version
                + " (https://github.com/idsm-src/sparql; jakub.galgonek@uochb.cas.cz)";
    }


    protected SqlServiceStub(UsedVariables variables, VarOrIri name, GraphPattern pattern, SqlIntercode context,
            UserStrBlankNodeClass blankNodeClass, boolean silent, SharedState state)
    {
        super(variables, false);

        this.context = context;
        this.name = name;
        this.pattern = pattern;
        this.silent = silent;
        this.blankNodeClass = blankNodeClass;
        this.state = state;
    }


    public static SqlIntercode create(Request request, VarOrIri name, GraphPattern pattern, SqlIntercode context,
            UserStrBlankNodeClass blankNodeClass, boolean silent)
    {
        return create(request, name, pattern, context, blankNodeClass, silent, null, new SharedState());
    }


    protected static SqlIntercode create(Request request, VarOrIri name, GraphPattern pattern, SqlIntercode context,
            UserStrBlankNodeClass blankNodeClass, boolean silent, Restrictions restrictions, SharedState state)
    {
        Set<ResourceClass> resourceClasses = new HashSet<ResourceClass>();
        resourceClasses.add(unsupportedIri);
        resourceClasses.add(unsupportedLiteral);
        resourceClasses.add(rdfLangString);
        resourceClasses.add(blankNodeClass);

        request.getConfiguration().getIriClasses().forEach(c -> resourceClasses.add(c));
        request.getConfiguration().getDataTypes().forEach(d -> resourceClasses.add(d.getGeneralLiteralClass()));

        UsedVariables variables = new UsedVariables();

        for(UsedVariable v : context.getVariables().getValues())
        {
            if(!v.canBeNull())
            {
                String varName = v.getName();
                UsedVariable var = new UsedVariable(varName, v.canBeNull());

                for(ResourceClass c : resourceClasses)
                    var.addMapping(c, c.createColumns(request.getColumnMap(), varName));

                variables.add(var);
            }
        }

        for(Variable v : pattern.getVariablesInScope())
        {
            String varName = v.getSqlName();

            if(!variables.getNames().contains(varName))
            {
                UsedVariable var = new UsedVariable(varName, true);

                for(ResourceClass c : resourceClasses)
                    var.addMapping(c, c.createColumns(request.getColumnMap(), varName));

                variables.add(var);
            }
        }

        return new SqlServiceStub(variables.restrict(restrictions), name, pattern, context, blankNodeClass, silent,
                state);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        if(evalServices)
            return eval(request, context, restrictions);


        Restrictions contextRestrictions = new Restrictions(restrictions);

        for(Variable var : pattern.getVariablesInScope())
            contextRestrictions.add(var.getSqlName());

        SqlIntercode optContext = context.optimize(request, contextRestrictions, reduced, evalServices);


        if(optContext.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(name instanceof Variable nameVar)
        {
            UsedVariable var = optContext.getVariables().get(nameVar.getSqlName());

            if(var == null || var.getCompatibleClasses(iri).isEmpty())
                return SqlNoSolution.get();
        }

        if(optContext instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : union.getChilds())
                childs.add(create(request, name, pattern, child, blankNodeClass, silent, restrictions, state));

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        if(optContext == context && restrictions.isOptimized(variables))
            return this;

        return create(request, name, pattern, optContext, blankNodeClass, silent, restrictions, state);
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

        Set<String> contextVariables = context.getVariables().getNames();
        HashMap<String, String> varmap = new HashMap<String, String>();
        Set<String> mergedVariables = new HashSet<String>(contextVariables);
        Set<Variable> sharedVariables = new HashSet<Variable>();

        for(Variable var : pattern.getVariablesInScope())
        {
            mergedVariables.add(var.getSqlName());
            varmap.put(var.getName(), var.getSqlName());

            if(contextVariables.contains(var.getSqlName()))
                sharedVariables.add(var);
        }


        ArrayList<RdfNode[]> rows = new ArrayList<RdfNode[]>();
        HashMap<String, Integer> varIndexes = null;

        if(context.equals(SqlEmptySolution.get()))
        {
            rows.add(new RdfNode[0]);
            varIndexes = new HashMap<String, Integer>();
        }
        else
        {
            /* evaluate context pattern */

            BigInteger limit = BigInteger.valueOf(serviceContextLimit + 1);
            ArrayList<String> vars = new ArrayList<String>(contextVariables);
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

        ServiceTranslateVisitor visitor = new ServiceTranslateVisitor();
        String serviceCode = visitor.getResultCode(pattern);

        try(ResultHandler results = new StoredResultHandler(request, restrictions))
        {
            int call = 0;

            for(RdfNode[] row : rows)
            {
                String endpoint = null;

                if(name instanceof IRI iri)
                    endpoint = iri.getValue();
                else if(row[varIndexes.get(((Variable) name).getSqlName())] instanceof IriNode)
                    endpoint = row[varIndexes.get(((Variable) name).getSqlName())].getValue();
                else
                    continue;


                /* build default result */

                HashMap<String, Node> defaultResult = new HashMap<String, Node>();

                for(String variable : contextVariables)
                {
                    Integer idx = varIndexes.get(variable);

                    if(idx == null)
                        continue;

                    RdfNode term = row[idx];

                    Node node = null;

                    if(term instanceof IriNode)
                        node = new IRI(term.getValue());
                    else if(term instanceof LanguageTaggedLiteral literal)
                        node = new Literal(term.getValue(), literal.getLanguage());
                    else if(term instanceof TypedLiteral literal)
                        node = new Literal(term.getValue(),
                                request.getConfiguration().getDataType(new IRI(literal.getDatatype().getValue())),
                                new IRI(literal.getDatatype().getValue()));
                    else if(term instanceof ReferenceNode)
                        node = new BlankNodeLiteral(term.getValue(), context.getVariables().get(variable).getClasses());

                    defaultResult.put(variable, node);
                }


                /* build service query */

                StringBuilder sparqlQueryBuilder = new StringBuilder();
                sparqlQueryBuilder.append("select * where ");
                sparqlQueryBuilder.append(serviceCode);

                if(!sharedVariables.isEmpty())
                {
                    sparqlQueryBuilder.append("values (");

                    for(Variable var : sharedVariables)
                        sparqlQueryBuilder.append(" ?").append(var.getName());

                    sparqlQueryBuilder.append(") {(");

                    for(Variable variable : sharedVariables)
                    {
                        RdfNode term = row[varIndexes.get(variable.getSqlName())];

                        if(term != null && (term instanceof IriNode || term.isLiteral()))
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
                        Set<String> used = new HashSet<String>();
                        HashMap<String, Node> result = new HashMap<String, Node>();

                        boolean skip;
                        String variable;
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
                                variable = varmap.get(attributes.getValue("name"));
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

                            Node node = null;

                            if(qName.equalsIgnoreCase("uri"))
                                node = new IRI(data.toString());
                            else if(qName.equalsIgnoreCase("bnode"))
                                node = new BlankNodeLiteral(bnprefix + "r" + data.toString(), blankNodeClass);
                            else if(!qName.equalsIgnoreCase("literal"))
                                return;
                            else if(lang != null)
                                node = new Literal(data.toString(), lang);
                            else if(datatype != null)
                                node = new Literal(data.toString(),
                                        request.getConfiguration().getDataType(new IRI(datatype)), new IRI(datatype));
                            else
                                node = new Literal(data.toString(), xsdStringType);

                            if(variable == null /*|| !serviceVariables.contains(variable)*/)
                                return; // should not happen if the response is valid

                            if(!used.add(variable))
                                return; // should not happen if the response is valid

                            if(defaultResult.get(variable) instanceof BlankNodeLiteral)
                            {
                                skip = true;
                                return;
                            }

                            if(defaultResult.containsKey(variable) && !defaultResult.get(variable).equals(node))
                            {
                                // should not happen if the response is correct
                                skip = true;
                                return;
                            }

                            result.put(variable, node);
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

        ServiceTranslateVisitor visitor = new ServiceTranslateVisitor();
        String serviceCode = visitor.getResultCode(pattern);

        String[] lines = serviceCode.split("\n");

        for(String line : lines)
        {
            indentInfo(builder, indent, true);
            builder.append(line);
        }

        indentChild(builder, indent, true);
        context.generateExplanation(builder, getIndent(indent, true));
    }


    public GraphPattern getPattern()
    {
        return pattern;
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

        if(!Objects.equals(pattern, imcode.pattern))
            return false;

        if(!Objects.equals(context, imcode.context))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(silent, name, blankNodeClass, pattern, context);
    }
}

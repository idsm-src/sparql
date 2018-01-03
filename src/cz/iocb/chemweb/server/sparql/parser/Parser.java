package cz.iocb.chemweb.server.sparql.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import cz.iocb.chemweb.server.sparql.grammar.SparqlLexer;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser;
import cz.iocb.chemweb.server.sparql.parser.error.ErrorType;
import cz.iocb.chemweb.server.sparql.parser.error.ParseException;
import cz.iocb.chemweb.server.sparql.parser.error.ParseExceptions;
import cz.iocb.chemweb.server.sparql.parser.error.UncheckedParseException;
import cz.iocb.chemweb.server.sparql.parser.model.Prefix;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.visitor.BaseVisitor;
import cz.iocb.chemweb.server.sparql.parser.visitor.QueryVisitor;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;



/**
 * Reads a SPARQL query and returns its representation using an AST.
 */
public class Parser
{
    private final LinkedHashMap<String, ProcedureDefinition> procedures;
    private final List<Prefix> predefinedPrefixes;

    public Parser()
    {
        procedures = new LinkedHashMap<String, ProcedureDefinition>();
        predefinedPrefixes = new ArrayList<>();
    }

    public Parser(LinkedHashMap<String, ProcedureDefinition> procedures, HashMap<String, String> predefinedPrefixes)
    {
        this.procedures = procedures;
        this.predefinedPrefixes = new ArrayList<>();

        for(Entry<String, String> entry : predefinedPrefixes.entrySet())
            this.predefinedPrefixes.add(new Prefix(entry.getKey(), entry.getValue()));
    }

    public LinkedHashMap<String, ProcedureDefinition> getProcedures()
    {
        return procedures;
    }

    public List<Prefix> getPredefinedPrefixes()
    {
        return predefinedPrefixes;
    }

    /**
     * Parses a SPARQL query contained in the given file.
     */
    public SelectQuery parseFile(String fileName) throws IOException, ParseExceptions
    {
        return parse(new ANTLRFileStream(fileName));
    }

    /**
     * Parses a SPARQL query contained in the given string.
     */
    public SelectQuery parse(String sparqlString) throws ParseExceptions
    {
        return parse(new ANTLRInputStream(sparqlString));
    }

    /**
     */
    public SelectQuery parse(CharStream stream) throws ParseExceptions
    {
        ParseResult result = tryParse(stream);

        if(!result.getExceptions().isEmpty())
            throw new ParseExceptions(result.getExceptions());

        return result.getResult();
    }

    /**
     * Parses a SPARQL query contained in the given file. If parsing fails, returns a partial result or null, and
     * exceptions.
     */
    public ParseResult tryParseFile(String fileName) throws IOException
    {
        return tryParse(new ANTLRFileStream(fileName));
    }

    /**
     * Parses a SPARQL query contained in the given string. If parsing fails, returns a partial result or null, and
     * exceptions.
     */
    public ParseResult tryParse(String sparqlString)
    {
        return tryParse(new ANTLRInputStream(sparqlString));
    }

    private static boolean isImportantException(ParseException exception)
    {
        if(!(exception.getCause() instanceof UncheckedParseException))
            return true;

        UncheckedParseException cause = (UncheckedParseException) exception.getCause();

        return cause.getCause() == null;
    }

    /**
     * Parses a SPARQL query contained in the given stream. If parsing fails, returns a partial result or null, and
     * exceptions.
     */
    public ParseResult tryParse(CharStream stream)
    {
        SelectQuery result = null;
        List<ParseException> exceptions = new ArrayList<>();

        try
        {
            SparqlLexer lex = new SparqlLexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lex);

            RecordErrorsListener errorListener = new RecordErrorsListener();

            SparqlParser parser = new SparqlParser(tokens);
            parser.removeErrorListeners(); // do not log to the console
            parser.addErrorListener(errorListener);
            ParserRuleContext t = parser.query();

            exceptions.addAll(errorListener.getExceptions());

            QueryVisitor.setProcedures(procedures);
            QueryVisitor.setPredefinedPrefixes(predefinedPrefixes);
            QueryVisitor.setExceptionConsumer(exceptions::add);

            QueryVisitor visitor = new QueryVisitor();
            result = (SelectQuery) visitor.visit(t);


            // NOTE: added by galgonek
            HashSet<String> usedVariables = new HashSet<>();
            new BaseVisitor<Void>()
            {
                @Override
                public Void visitVar(SparqlParser.VarContext ctx)
                {
                    usedVariables.add(ctx.getText().substring(1));
                    return null;
                }

            }.visit(t);

            if(result != null)
                result.setQueryVariables(usedVariables);
        }
        catch(UncheckedParseException e)
        {
            exceptions.add(new ParseException(e));
        }

        List<ParseException> importantExceptions = exceptions.stream().filter(Parser::isImportantException)
                .collect(Collectors.toList());

        return new ParseResult(result, importantExceptions.isEmpty() ? exceptions : importantExceptions);
    }
}

/**
 * Records exceptions encountered during parsing into a list.
 */
class RecordErrorsListener extends BaseErrorListener
{
    private final List<ParseException> exceptions = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e)
    {
        Range range;
        if(offendingSymbol instanceof Token)
            range = Range.compute((Token) offendingSymbol, (Token) offendingSymbol);
        else
            range = new Range(new Position(line, charPositionInLine), null);

        exceptions.add(new ParseException(ErrorType.syntaxError, range, msg));
    }

    public List<ParseException> getExceptions()
    {
        return exceptions;
    }
}

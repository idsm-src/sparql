package cz.iocb.sparql.engine.parser;

import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.grammar.SparqlLexer;
import cz.iocb.sparql.engine.grammar.SparqlParser;
import cz.iocb.sparql.engine.model.base.Range;



/**
 * Reads a SPARQL query and returns its representation using an AST.
 */
public class Parser
{
    /**
     * Messages the syntax errors are appended to.
     */
    private final List<TranslateMessage> messages;


    /**
     * Creates a parser that appends syntax errors to {@code messages}.
     *
     * @param messages the message list to append to
     */
    public Parser(List<TranslateMessage> messages)
    {
        this.messages = messages;
    }


    /**
     * Parses a SPARQL query and returns its parse tree. The escape sequences of IRIs and strings are resolved by the
     * visitors building the syntax tree, as SPARQL 1.2 processes them during parsing (so that they cannot occur in
     * prefixed names, variables or keywords); syntax errors are reported as messages, not thrown.
     *
     * @param query the query text
     * @return the parse tree
     */
    public ParserRuleContext parse(String query)
    {
        return parse(CharStreams.fromString(query));
    }


    /**
     * Runs the ANTLR lexer and parser on the stream, collecting syntax errors as messages.
     *
     * @param stream character stream of the query
     * @return the parse tree
     */
    private ParserRuleContext parse(CharStream stream)
    {
        BaseErrorListener errorListener = new BaseErrorListener()
        {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                    int charPositionInLine, String msg, RecognitionException e)
            {
                Range range;
                if(offendingSymbol instanceof Token token)
                    range = Range.compute(token, token);
                else
                    range = new Range(new Position(line, charPositionInLine), null);

                messages.add(new TranslateMessage(MessageType.syntaxError, range, msg));
            }
        };


        SparqlLexer lex = new SparqlLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lex);

        SparqlParser parser = new SparqlParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        ParserRuleContext result = parser.query();

        return result;
    }
}

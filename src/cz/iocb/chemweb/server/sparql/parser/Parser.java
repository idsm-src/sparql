package cz.iocb.chemweb.server.sparql.parser;

import java.util.List;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import cz.iocb.chemweb.server.sparql.error.MessageType;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.server.sparql.grammar.SparqlLexer;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser;



/**
 * Reads a SPARQL query and returns its representation using an AST.
 */
public class Parser
{
    private final List<TranslateMessage> messages;


    public Parser(List<TranslateMessage> messages)
    {
        this.messages = messages;
    }


    /**
     * Parses a SPARQL query contained in the given string.
     */
    public ParserRuleContext parse(String sparqlString)
    {
        return parse(new ANTLRInputStream(sparqlString));
    }


    public ParserRuleContext parse(CharStream stream)
    {
        BaseErrorListener errorListener = new BaseErrorListener()
        {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                    int charPositionInLine, String msg, RecognitionException e)
            {
                Range range;
                if(offendingSymbol instanceof Token)
                    range = Range.compute((Token) offendingSymbol, (Token) offendingSymbol);
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

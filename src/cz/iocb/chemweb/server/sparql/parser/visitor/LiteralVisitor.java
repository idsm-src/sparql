package cz.iocb.chemweb.server.sparql.parser.visitor;

import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BooleanLiteralContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.NumericLiteralContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.NumericLiteralNegativeContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.NumericLiteralPositiveContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.RdfLiteralContext;
import cz.iocb.chemweb.server.sparql.parser.Range;
import cz.iocb.chemweb.server.sparql.parser.Xsd;
import cz.iocb.chemweb.server.sparql.parser.error.ErrorType;
import cz.iocb.chemweb.server.sparql.parser.error.UncheckedParseException;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class LiteralVisitor extends BaseVisitor<Literal>
{
    @Override
    public Literal visitRdfLiteral(RdfLiteralContext ctx)
    {
        Literal result = new Literal(unquote(ctx.string().getText()));

        if(ctx.LANGTAG() != null)
        {
            result.setLanguageTag(ctx.LANGTAG().getText());
        }

        if(ctx.iri() != null)
        {
            try
            {
                result.setTypeIri(new IriVisitor().visit(ctx.iri()));
            }
            catch (UncheckedParseException ex)
            {
                throw ex;
            }
            // FIXME: it is needed?
            catch (Throwable ex)
            {
                throw new UncheckedParseException(ErrorType.parsingException, ex, Range.compute(ctx), ex.getMessage());
            }
        }

        return result;
    }

    private static String unescape(String text)
    {
        // [160] ECHAR ::= '\' [tbnrf\"']
        return text.replace("\\t", "\t").replace("\\b", "\b").replace("\\n", "\n").replace("\\r", "\r")
                .replace("\\\"", "\"").replace("\\'", "'");
    }

    public static String unquote(String text)
    {
        if(text.startsWith("\"\"\"") && text.endsWith("\"\"\"") || text.startsWith("'''") && text.endsWith("'''"))
            text = text.substring(3, text.length() - 3);
        else if(text.startsWith("\"") && text.endsWith("\"") || text.startsWith("'") && text.endsWith("'"))
            text = text.substring(1, text.length() - 1);
        else
            throw new IllegalArgumentException();

        return unescape(text);
    }

    private static Literal createNumericLiteral(String text)
    {
        String type;
        if(text.contains("e") || text.contains("E"))
            type = Xsd.DOUBLE;
        else if(text.contains("."))
            type = Xsd.DECIMAL;
        else
            type = Xsd.INTEGER;

        return new Literal(text, new IRI(type));
    }

    @Override
    public Literal visitNumericLiteral(NumericLiteralContext ctx)
    {
        return createNumericLiteral(ctx.getText());
    }

    @Override
    public Literal visitNumericLiteralPositive(NumericLiteralPositiveContext ctx)
    {
        return createNumericLiteral(ctx.getText());
    }

    @Override
    public Literal visitNumericLiteralNegative(NumericLiteralNegativeContext ctx)
    {
        return createNumericLiteral(ctx.getText());
    }

    @Override
    public Literal visitBooleanLiteral(BooleanLiteralContext ctx)
    {
        return new Literal(ctx.getText(), new IRI(Xsd.BOOLEAN));
    }
}

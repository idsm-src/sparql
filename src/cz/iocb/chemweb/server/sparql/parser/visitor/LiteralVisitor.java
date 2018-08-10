package cz.iocb.chemweb.server.sparql.parser.visitor;

import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdBooleanIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDecimalIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDoubleIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntegerIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdStringIri;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BooleanLiteralContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.NumericLiteralContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.NumericLiteralNegativeContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.NumericLiteralPositiveContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.RdfLiteralContext;
import cz.iocb.chemweb.server.sparql.parser.Range;
import cz.iocb.chemweb.server.sparql.parser.error.ErrorType;
import cz.iocb.chemweb.server.sparql.parser.error.UncheckedParseException;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class LiteralVisitor extends BaseVisitor<Literal>
{
    private final QueryVisitorContext context;

    public LiteralVisitor(QueryVisitorContext context)
    {
        this.context = context;
    }

    @Override
    public Literal visitRdfLiteral(RdfLiteralContext ctx)
    {
        if(ctx.LANGTAG() != null)
            return new Literal(unquote(ctx.string().getText()), ctx.LANGTAG().getText().substring(1));

        if(ctx.iri() == null)
            return new Literal(unquote(ctx.string().getText()), xsdStringIri);

        try
        {
            Literal literal = new Literal(unquote(ctx.string().getText()), new IriVisitor(context).visit(ctx.iri()));

            if(!literal.isTypeSupported())
            {
                //TODO: warning: type is not supported
            }
            else if(literal.getValue() == null)
            {
                //TODO: warning: lexical value is not supported
            }

            return literal;
        }
        catch(UncheckedParseException ex)
        {
            throw ex;
        }
        catch(Throwable ex) //TODO: is it needed?
        {
            throw new UncheckedParseException(ErrorType.parsingException, ex, Range.compute(ctx), ex.getMessage());
        }
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
        IRI type;

        if(text.contains("e") || text.contains("E"))
            type = xsdDoubleIri;
        else if(text.contains("."))
            type = xsdDecimalIri;
        else
            type = xsdIntegerIri;

        return new Literal(text, type);
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
        return new Literal(ctx.getText(), xsdBooleanIri);
    }
}

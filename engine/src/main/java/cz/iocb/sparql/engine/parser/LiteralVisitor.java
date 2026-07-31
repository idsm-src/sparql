package cz.iocb.sparql.engine.parser;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import java.util.List;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.grammar.SparqlParser.BooleanLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NumericLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NumericLiteralNegativeContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NumericLiteralPositiveContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.RdfLiteralContext;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.Prologue;
import cz.iocb.sparql.engine.model.base.Range;
import cz.iocb.sparql.engine.model.expression.LiteralNode;
import cz.iocb.sparql.engine.rdf.Iri;



public class LiteralVisitor extends BaseVisitor<LiteralNode>
{
    private final Prologue prologue;
    private final List<TranslateMessage> messages;


    public LiteralVisitor(Prologue prologue, List<TranslateMessage> messages)
    {
        this.prologue = prologue;
        this.messages = messages;
    }


    @Override
    public LiteralNode visitRdfLiteral(RdfLiteralContext ctx)
    {
        String value = unquote(ctx.string().getText());

        if(containsInvalidSurrogatePairs(value))
            messages.add(new TranslateMessage(MessageType.partialSurrogatePair, Range.compute(ctx.string())));

        if(ctx.LANGTAG() != null)
        {
            String tag = ctx.LANGTAG().getText().substring(1);

            if(!tag.matches("""
                    ([A-Za-z]{2,3}(-[A-Za-z]{3}){0,3}|[A-Za-z]{4,8})\
                    (-[A-Za-z]{4})?(-([A-Za-z]{2}|[0-9]{3}))?(-([A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*\
                    (-[0-9A-WY-Za-wy-z](-[A-Za-z0-9]{2,8})+)*(-x(-[A-Za-z0-9]{1,8})+)?|x(-[A-Za-z0-9]{1,8})+\
                    |i-ami|i-bnn|i-default|i-enochian|i-hak|i-klingon|i-lux|i-mingo|i-navajo|i-pwn\
                    |i-tao|i-tay|i-tsu|sgn-BE-FR|sgn-BE-NL|sgn-CH-DE"""))
                messages.add(new TranslateMessage(MessageType.invalidLanguageTag,
                        Range.compute(ctx.LANGTAG().getSymbol(), ctx.LANGTAG().getSymbol()), tag));

            return new LiteralNode(value, tag);
        }
        else if(ctx.iri() != null)
        {
            IriNode type = new IriVisitor(prologue, messages).visit(ctx.iri());

            if(type == null)
                return new LiteralNode(value, new IriNode(xsdStringIri.getValue()));

            return new LiteralNode(value, type);
        }
        else
        {
            return new LiteralNode(value, new IriNode(xsdStringIri.getValue()));
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


    private static LiteralNode createNumericLiteral(String text)
    {
        Iri type;

        if(text.contains("e") || text.contains("E"))
            type = xsdDoubleIri;
        else if(text.contains("."))
            type = xsdDecimalIri;
        else
            type = xsdIntegerIri;

        return new LiteralNode(text, new IriNode(type.getValue()));
    }


    @Override
    public LiteralNode visitNumericLiteral(NumericLiteralContext ctx)
    {
        return createNumericLiteral(ctx.getText());
    }


    @Override
    public LiteralNode visitNumericLiteralPositive(NumericLiteralPositiveContext ctx)
    {
        return createNumericLiteral(ctx.getText());
    }


    @Override
    public LiteralNode visitNumericLiteralNegative(NumericLiteralNegativeContext ctx)
    {
        return createNumericLiteral(ctx.getText());
    }


    @Override
    public LiteralNode visitBooleanLiteral(BooleanLiteralContext ctx)
    {
        return new LiteralNode(ctx.getText(), new IriNode(xsdBooleanIri.getValue()));
    }


    private static boolean containsInvalidSurrogatePairs(String str)
    {
        for(int i = 0; i < str.length(); i++)
        {
            char ch = str.charAt(i);

            if(Character.isHighSurrogate(ch))
            {
                if(i + 1 < str.length() && Character.isLowSurrogate(str.charAt(i + 1)))
                    i++;
                else
                    return true;
            }
            else if(Character.isLowSurrogate(ch))
            {
                return true;
            }
        }

        return false;
    }
}

package cz.iocb.chemweb.server.sparql.parser.visitor;

import static cz.iocb.chemweb.server.sparql.parser.StreamUtils.mapList;
import static java.util.stream.Collectors.toList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BlankNodeContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BlankNodePropertyListContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BlankNodePropertyListPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BooleanLiteralContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.CollectionContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.CollectionPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.IriContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.NilContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.NumericLiteralContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ObjectPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.RdfLiteralContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VarContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VarOrIRIContext;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNodePropertyList;
import cz.iocb.chemweb.server.sparql.parser.model.triple.ComplexNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Property;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RdfCollection;



public class NodeVisitor extends BaseVisitor<ComplexNode>
{
    private final Prologue prologue;
    private final VariableScopes scopes;
    private final List<TranslateMessage> messages;


    public NodeVisitor(Prologue prologue, VariableScopes scopes, List<TranslateMessage> messages)
    {
        this.prologue = prologue;
        this.scopes = scopes;
        this.messages = messages;
    }


    @Override
    public ComplexNode visitObjectPath(ObjectPathContext ctx)
    {
        ComplexNode node = visit(ctx.graphNodePath());

        return node;
    }


    @Override
    public Variable visitVar(VarContext ctx)
    {
        return new Variable(scopes.addToScope(ctx.getText()), ctx.getText());
    }


    @Override
    public IRI visitIri(IriContext ctx)
    {
        return new IriVisitor(prologue, messages).visit(ctx);
    }


    public VarOrIri parseVarOrIri(VarOrIRIContext ctx)
    {
        if(ctx.var() != null)
            return (Variable) visit(ctx.var());

        if(ctx.iri() != null)
            return (IRI) visit(ctx.iri());

        return null;
    }


    @Override
    public ComplexNode visitBlankNode(BlankNodeContext ctx)
    {
        if(ctx.anon() != null)
        {
            return new BlankNodePropertyList();
        }

        return new BlankNode(ctx.getText());
    }


    @Override
    public ComplexNode visitNil(NilContext ctx)
    {
        return new RdfCollection();
    }


    @Override
    public RdfCollection visitCollectionPath(CollectionPathContext ctx)
    {
        List<ComplexNode> nodes = mapList(ctx.graphNodePath(), this::visit);

        return new RdfCollection(nodes);
    }


    @Override
    public RdfCollection visitCollection(CollectionContext ctx)
    {
        List<ComplexNode> nodes = mapList(ctx.graphNode(), this::visit);

        return new RdfCollection(nodes);
    }


    @Override
    public BlankNodePropertyList visitBlankNodePropertyListPath(BlankNodePropertyListPathContext ctx)
    {
        List<Property> properties = new PropertiesVisitor(prologue, scopes, messages)
                .visit(ctx.propertyListPathNotEmpty()).collect(toList());

        return new BlankNodePropertyList(properties);
    }


    @Override
    public BlankNodePropertyList visitBlankNodePropertyList(BlankNodePropertyListContext ctx)
    {
        List<Property> properties = new PropertiesVisitor(prologue, scopes, messages).visit(ctx.propertyListNotEmpty())
                .collect(toList());

        return new BlankNodePropertyList(properties);
    }


    @Override
    public Literal visitRdfLiteral(RdfLiteralContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitRdfLiteral(ctx);
    }


    @Override
    public Literal visitNumericLiteral(NumericLiteralContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitNumericLiteral(ctx);
    }


    @Override
    public Literal visitBooleanLiteral(BooleanLiteralContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitBooleanLiteral(ctx);
    }
}

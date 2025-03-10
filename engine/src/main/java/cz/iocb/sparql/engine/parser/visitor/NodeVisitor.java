package cz.iocb.sparql.engine.parser.visitor;

import java.util.List;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.grammar.SparqlParser.BlankNodeContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BlankNodePropertyListContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BlankNodePropertyListPathContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BooleanLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.CollectionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.CollectionPathContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.IriContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NilContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NumericLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ObjectPathContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.RdfLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.VarContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.VarOrIRIContext;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.Prologue;
import cz.iocb.sparql.engine.parser.model.VarOrIri;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.BlankNode;
import cz.iocb.sparql.engine.parser.model.triple.BlankNodePropertyList;
import cz.iocb.sparql.engine.parser.model.triple.ComplexNode;
import cz.iocb.sparql.engine.parser.model.triple.Property;
import cz.iocb.sparql.engine.parser.model.triple.RdfCollection;



public class NodeVisitor extends BaseVisitor<ComplexNode>
{
    private final SparqlDatabaseConfiguration config;
    private final Prologue prologue;
    private final VariableScopes scopes;
    private final List<TranslateMessage> messages;


    public NodeVisitor(SparqlDatabaseConfiguration config, Prologue prologue, VariableScopes scopes,
            List<TranslateMessage> messages)
    {
        this.config = config;
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
        List<ComplexNode> nodes = ctx.graphNodePath().stream().map(this::visit).toList();

        return new RdfCollection(nodes);
    }


    @Override
    public RdfCollection visitCollection(CollectionContext ctx)
    {
        List<ComplexNode> nodes = ctx.graphNode().stream().map(this::visit).toList();

        return new RdfCollection(nodes);
    }


    @Override
    public BlankNodePropertyList visitBlankNodePropertyListPath(BlankNodePropertyListPathContext ctx)
    {
        List<Property> properties = new PropertiesVisitor(config, prologue, scopes, messages)
                .visit(ctx.propertyListPathNotEmpty()).toList();

        return new BlankNodePropertyList(properties);
    }


    @Override
    public BlankNodePropertyList visitBlankNodePropertyList(BlankNodePropertyListContext ctx)
    {
        List<Property> properties = new PropertiesVisitor(config, prologue, scopes, messages)
                .visit(ctx.propertyListNotEmpty()).toList();

        return new BlankNodePropertyList(properties);
    }


    @Override
    public Literal visitRdfLiteral(RdfLiteralContext ctx)
    {
        return new LiteralVisitor(config, prologue, messages).visitRdfLiteral(ctx);
    }


    @Override
    public Literal visitNumericLiteral(NumericLiteralContext ctx)
    {
        return new LiteralVisitor(config, prologue, messages).visitNumericLiteral(ctx);
    }


    @Override
    public Literal visitBooleanLiteral(BooleanLiteralContext ctx)
    {
        return new LiteralVisitor(config, prologue, messages).visitBooleanLiteral(ctx);
    }
}

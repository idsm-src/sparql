package cz.iocb.sparql.engine.parser;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.grammar.SparqlParser.AnnotationBlockContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.AnnotationBlockPathContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BlankNodeContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BlankNodePropertyListContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BlankNodePropertyListPathContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BooleanLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.CollectionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.CollectionPathContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.IriContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NilContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NumericLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ObjectContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ObjectPathContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.PropertyListContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.RdfLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ReifiedTripleBlockContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ReifiedTripleBlockPathContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ReifiedTripleContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ReifierContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.TripleTermContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.TriplesSameSubjectContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.TriplesSameSubjectPathContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.VarContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.VarOrIriContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.VarOrReifierIdContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.VerbContext;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.Prologue;
import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.expression.LiteralNode;
import cz.iocb.sparql.engine.model.triple.AnnotatedNode;
import cz.iocb.sparql.engine.model.triple.Annotation;
import cz.iocb.sparql.engine.model.triple.AnnotationBlock;
import cz.iocb.sparql.engine.model.triple.BlankNode;
import cz.iocb.sparql.engine.model.triple.BlankNodePropertyList;
import cz.iocb.sparql.engine.model.triple.ComplexNode;
import cz.iocb.sparql.engine.model.triple.ComplexTriple;
import cz.iocb.sparql.engine.model.triple.ComplexTripleTerm;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.triple.Property;
import cz.iocb.sparql.engine.model.triple.RdfCollection;
import cz.iocb.sparql.engine.model.triple.ReifiedTriple;
import cz.iocb.sparql.engine.model.triple.Reifier;



/**
 * Builds the graph terms of triples: variables, IRIs, literals, blank nodes, triple terms, collections, blank node
 * property lists, reified triples and annotated objects (the latter five still in their syntax-sugar form), and the
 * triples of a triples-same-subject rule.
 */
public class NodeVisitor extends BaseVisitor<ComplexNode>
{
    /**
     * Configuration of the endpoint.
     */
    private final SparqlDatabaseConfiguration config;

    /**
     * Prologue of the query.
     */
    private final Prologue prologue;

    /**
     * Variable scopes of the query.
     */
    private final VariableScopes scopes;

    /**
     * Messages collected during parsing.
     */
    private final List<TranslateMessage> messages;


    /**
     * Creates the visitor.
     *
     * @param config the endpoint configuration
     * @param prologue the prologue of the query
     * @param scopes the variable scopes
     * @param messages the message list to append to
     */
    public NodeVisitor(SparqlDatabaseConfiguration config, Prologue prologue, VariableScopes scopes,
            List<TranslateMessage> messages)
    {
        this.config = config;
        this.prologue = prologue;
        this.scopes = scopes;
        this.messages = messages;
    }


    /**
     * Parses a triples-same-subject rule of a triples block into a triple with syntax sugar.
     *
     * @param ctx the parse tree node
     * @return the triple
     */
    public ComplexTriple parseTriples(TriplesSameSubjectPathContext ctx)
    {
        PropertiesVisitor propertiesVisitor = new PropertiesVisitor(config, prologue, scopes, messages);

        if(ctx.varOrTerm() != null)
            return new ComplexTriple(visit(ctx.varOrTerm()),
                    propertiesVisitor.visit(ctx.propertyListPathNotEmpty()).toList());

        if(ctx.triplesNodePath() != null)
            return new ComplexTriple(visit(ctx.triplesNodePath()),
                    propertiesVisitor.visit(ctx.propertyListPath()).toList());

        ReifiedTripleBlockPathContext blockCtx = ctx.reifiedTripleBlockPath();

        return new ComplexTriple(visit(blockCtx.reifiedTriple()),
                propertiesVisitor.visit(blockCtx.propertyListPath()).toList());
    }


    /**
     * Parses a triples-same-subject rule of a template into a triple with syntax sugar.
     *
     * @param ctx the parse tree node
     * @return the triple
     */
    public ComplexTriple parseTriples(TriplesSameSubjectContext ctx)
    {
        if(ctx.varOrTerm() != null)
            return new ComplexTriple(visit(ctx.varOrTerm()), parseProperties(ctx.propertyListNotEmpty()));

        if(ctx.triplesNode() != null)
            return new ComplexTriple(visit(ctx.triplesNode()), parseProperties(ctx.propertyList()));

        ReifiedTripleBlockContext blockCtx = ctx.reifiedTripleBlock();

        return new ComplexTriple(visit(blockCtx.reifiedTriple()), parseProperties(blockCtx.propertyList()));
    }


    /**
     * Parses a possibly empty plain property list.
     *
     * @param ctx the parse tree node
     * @return the properties
     */
    private List<Property> parseProperties(PropertyListContext ctx)
    {
        if(ctx.propertyListNotEmpty() == null)
            return new ArrayList<>();

        return parseProperties(ctx.propertyListNotEmpty());
    }


    /**
     * Parses a non-empty property list, plain or with paths.
     *
     * @param ctx the PropertyListNotEmpty or PropertyListPathNotEmpty parse tree node
     * @return the properties
     */
    private List<Property> parseProperties(ParserRuleContext ctx)
    {
        return new PropertiesVisitor(config, prologue, scopes, messages).visit(ctx).toList();
    }


    @Override
    public ComplexNode visitObjectPath(ObjectPathContext ctx)
    {
        ComplexNode node = visit(ctx.graphNodePath());
        List<Annotation> annotations = parseAnnotations(ctx.annotationPath());

        if(annotations.isEmpty())
            return node;

        return new AnnotatedNode(node, annotations);
    }


    @Override
    public ComplexNode visitObject(ObjectContext ctx)
    {
        ComplexNode node = visit(ctx.graphNode());
        List<Annotation> annotations = parseAnnotations(ctx.annotation());

        if(annotations.isEmpty())
            return node;

        return new AnnotatedNode(node, annotations);
    }


    /**
     * Parses the elements of an annotation, reifiers and annotation blocks, in their order.
     *
     * @param ctx the AnnotationPath or Annotation parse tree node
     * @return the elements of the annotation, empty when there are none
     */
    private List<Annotation> parseAnnotations(ParserRuleContext ctx)
    {
        List<Annotation> annotations = new ArrayList<>();

        for(int i = 0; i < ctx.getChildCount(); i++)
        {
            ParseTree child = ctx.getChild(i);

            if(child instanceof ReifierContext reifierCtx)
                annotations.add(withRange(new Reifier(parseReifier(reifierCtx)), reifierCtx));
            else if(child instanceof AnnotationBlockPathContext blockCtx)
                annotations.add(
                        withRange(new AnnotationBlock(parseProperties(blockCtx.propertyListPathNotEmpty())), blockCtx));
            else if(child instanceof AnnotationBlockContext blockCtx)
                annotations.add(
                        withRange(new AnnotationBlock(parseProperties(blockCtx.propertyListNotEmpty())), blockCtx));
        }

        return annotations;
    }


    /**
     * Parses the node of a reifier: a variable, an IRI or a labelled blank node. Null when the node is omitted or is an
     * anonymous blank node, as both stand for a fresh blank node.
     *
     * @param ctx the parse tree node
     * @return the reifier node, or null
     */
    private Node parseReifier(ReifierContext ctx)
    {
        VarOrReifierIdContext idCtx = ctx.varOrReifierId();

        if(idCtx == null)
            return null;

        if(idCtx.var() != null)
            return (VariableNode) visit(idCtx.var());

        if(idCtx.iri() != null)
            return (IriNode) visit(idCtx.iri());

        if(idCtx.blankNode().BLANK_NODE_LABEL() != null)
            return withRange(new BlankNode(idCtx.blankNode().getText()), idCtx.blankNode());

        return null;
    }


    @Override
    public ReifiedTriple visitReifiedTriple(ReifiedTripleContext ctx)
    {
        ComplexNode subject = visit(ctx.reifiedTripleSubject());
        VarOrIri predicate = parseVerb(ctx.verb());
        ComplexNode object = visit(ctx.reifiedTripleObject());
        Node reifier = ctx.reifier() != null ? parseReifier(ctx.reifier()) : null;

        return new ReifiedTriple(subject, predicate, object, reifier);
    }


    @Override
    public ComplexTripleTerm visitTripleTerm(TripleTermContext ctx)
    {
        ComplexNode subject = visit(ctx.tripleTermSubject());
        VarOrIri predicate = parseVerb(ctx.verb());
        ComplexNode object = visit(ctx.tripleTermObject());

        return new ComplexTripleTerm(subject, predicate, object);
    }


    /**
     * Parses the predicate of a reified triple or a triple term: an IRI, a variable, or {@code rdf:type} for {@code a}.
     *
     * @param ctx the parse tree node
     * @return the predicate
     */
    private VarOrIri parseVerb(VerbContext ctx)
    {
        // the Verb rule yields only IRIs and variables
        return (VarOrIri) new VerbVisitor(prologue, scopes, messages).visit(ctx);
    }


    @Override
    public VariableNode visitVar(VarContext ctx)
    {
        return new VariableNode(scopes.addToScope(ctx.getText()), ctx.getText());
    }


    @Override
    public IriNode visitIri(IriContext ctx)
    {
        return new IriVisitor(prologue, messages).visit(ctx);
    }


    /**
     * Parses a {@code VarOrIri} rule into a variable or an IRI; null if it is neither.
     *
     * @param ctx the parse tree node
     * @return the variable or IRI, or null
     */
    public VarOrIri parseVarOrIri(VarOrIriContext ctx)
    {
        if(ctx.var() != null)
            return (VariableNode) visit(ctx.var());

        if(ctx.iri() != null)
            return (IriNode) visit(ctx.iri());

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
    public LiteralNode visitRdfLiteral(RdfLiteralContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitRdfLiteral(ctx);
    }


    @Override
    public LiteralNode visitNumericLiteral(NumericLiteralContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitNumericLiteral(ctx);
    }


    @Override
    public LiteralNode visitBooleanLiteral(BooleanLiteralContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitBooleanLiteral(ctx);
    }
}

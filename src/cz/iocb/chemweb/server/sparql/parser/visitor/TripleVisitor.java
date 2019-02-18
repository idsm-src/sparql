package cz.iocb.chemweb.server.sparql.parser.visitor;

import static cz.iocb.chemweb.server.sparql.parser.StreamUtils.mapList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ObjectListContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ObjectListPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ObjectPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PropertyListNotEmptyContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PropertyListPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PropertyListPathNotEmptyContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PropertyListPathNotEmptyListContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.RdfLiteralContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.TriplesSameSubjectPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VarContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VarOrIRIContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VerbContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VerbPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VerbSimpleContext;
import cz.iocb.chemweb.server.sparql.parser.Rdf;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNodePropertyList;
import cz.iocb.chemweb.server.sparql.parser.model.triple.ComplexNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.ComplexTriple;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Property;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RdfCollection;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;



public class TripleVisitor
{
    private final Prologue prologue;
    private final List<TranslateMessage> messages;


    public TripleVisitor(Prologue prologue, List<TranslateMessage> messages)
    {
        this.prologue = prologue;
        this.messages = messages;
    }


    public ComplexTriple parseTriple(TriplesSameSubjectPathContext ctx)
    {
        NodeVisitor nodeVisitor = new NodeVisitor(prologue, messages);
        PropertiesVisitor propertiesVisitor = new PropertiesVisitor(prologue, messages);

        ComplexNode node;
        Stream<Property> properties;

        if(ctx.varOrTerm() != null)
        {
            node = nodeVisitor.visit(ctx.varOrTerm());
            properties = propertiesVisitor.visit(ctx.propertyListPathNotEmpty());
        }
        else
        {
            node = nodeVisitor.visit(ctx.triplesNodePath());
            properties = propertiesVisitor.visit(ctx.propertyListPath());
        }

        return new ComplexTriple(node, properties.collect(Collectors.toList()));
    }
}


class NodeVisitor extends BaseVisitor<ComplexNode>
{
    private final Prologue prologue;
    private final List<TranslateMessage> messages;


    public NodeVisitor(Prologue prologue, List<TranslateMessage> messages)
    {
        this.prologue = prologue;
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
        return new Variable(ctx.getText());
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

        return (IRI) visit(ctx.iri());
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
        List<Property> properties = new PropertiesVisitor(prologue, messages).visit(ctx.propertyListPathNotEmpty())
                .collect(Collectors.toList());

        return new BlankNodePropertyList(properties);
    }


    @Override
    public BlankNodePropertyList visitBlankNodePropertyList(BlankNodePropertyListContext ctx)
    {
        List<Property> properties = new PropertiesVisitor(prologue, messages).visit(ctx.propertyListNotEmpty())
                .collect(Collectors.toList());

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


class PropertiesVisitor extends BaseVisitor<Stream<Property>>
{
    private final Prologue prologue;
    private final List<TranslateMessage> messages;


    public PropertiesVisitor(Prologue prologue, List<TranslateMessage> messages)
    {
        this.prologue = prologue;
        this.messages = messages;
    }


    private Verb parseVerb(VerbPathContext verbPathCtx, VerbSimpleContext verbSimpleCtx)
    {
        if(verbPathCtx == null && verbSimpleCtx == null)
            return new Variable("");

        if(verbPathCtx != null)
        {
            return new PathVisitor(prologue, messages).visit(verbPathCtx.path());
        }

        return withRange(new Variable(verbSimpleCtx.var().getText()), verbSimpleCtx);
    }


    private List<ComplexNode> parseNodes(ObjectListPathContext ctx)
    {
        if(ctx == null)
            return new LinkedList<ComplexNode>();

        NodeVisitor nodeVisitor = new NodeVisitor(prologue, messages);

        return mapList(ctx.objectPath(), nodeVisitor::visit);
    }


    private List<ComplexNode> parseNodes(ObjectListContext ctx)
    {
        NodeVisitor nodeVisitor = new NodeVisitor(prologue, messages);

        return mapList(ctx.object(), nodeVisitor::visit);
    }


    private Property parseProperty(PropertyListPathNotEmptyListContext ctx)
    {
        Verb verb = parseVerb(ctx.verbPath(), ctx.verbSimple());
        List<ComplexNode> nodes = parseNodes(ctx.objectListPath());

        return withRange(new Property(verb, nodes), ctx);
    }


    @Override
    public Stream<Property> visitPropertyListPathNotEmpty(PropertyListPathNotEmptyContext ctx)
    {
        Verb verb = parseVerb(ctx.verbPath(), ctx.verbSimple());
        List<ComplexNode> nodes = parseNodes(ctx.objectListPath());

        Property head = new Property(verb, nodes);
        Stream<Property> tail = ctx.propertyListPathNotEmptyList().stream().map(this::parseProperty);

        // this is likely going to be be O(n^2), hopefully that's not a problem
        return Stream.concat(Stream.of(head), tail);
    }


    @Override
    public Stream<Property> visitPropertyListNotEmpty(PropertyListNotEmptyContext ctx)
    {
        VerbVisitor verbVisitor = new VerbVisitor(prologue, messages);

        List<Property> properties = new ArrayList<>();

        for(int i = 0; i < ctx.verb().size(); i++)
        {
            Verb verb = verbVisitor.visit(ctx.verb(i));
            List<ComplexNode> nodes = parseNodes(ctx.objectList(i));

            properties.add(new Property(verb, nodes));
        }

        return properties.stream();
    }


    @Override
    public Stream<Property> visitPropertyListPath(PropertyListPathContext ctx)
    {
        if(ctx.propertyListPathNotEmpty() == null)
            return Stream.empty();

        return super.visitPropertyListPath(ctx);
    }
}


class VerbVisitor extends BaseVisitor<Verb>
{
    private final Prologue prologue;
    private final List<TranslateMessage> messages;


    public VerbVisitor(Prologue prologue, List<TranslateMessage> messages)
    {
        this.prologue = prologue;
        this.messages = messages;
    }


    @Override
    public Verb visitVerb(VerbContext ctx)
    {
        if(ctx.A() != null)
            return new IRI(Rdf.TYPE);

        return visit(ctx.varOrIRI());
    }


    @Override
    public Variable visitVar(VarContext ctx)
    {
        return new Variable(ctx.getText());
    }


    @Override
    public IRI visitIri(IriContext ctx)
    {
        return new IriVisitor(prologue, messages).visit(ctx);
    }
}

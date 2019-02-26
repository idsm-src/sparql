package cz.iocb.chemweb.server.sparql.parser.visitor;

import static cz.iocb.chemweb.server.sparql.parser.StreamUtils.mapList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.IriContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ObjectListContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ObjectListPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PropertyListNotEmptyContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PropertyListPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PropertyListPathNotEmptyContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PropertyListPathNotEmptyListContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VarContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VerbContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VerbPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VerbSimpleContext;
import cz.iocb.chemweb.server.sparql.parser.Rdf;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.triple.ComplexNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Property;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;



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

package cz.iocb.chemweb.server.sparql.translator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ConstantMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedMapping;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.parser.Element;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.AlternativePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BracketedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.InversePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.NegatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Path;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RepeatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RepeatedPath.Kind;
import cz.iocb.chemweb.server.sparql.parser.model.triple.SequencePath;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlEmptySolution;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlJoin;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlNoSolution;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlRecursive;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlTableAccess;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlUnion;



public class PathTranslateVisitor extends ElementVisitor<SqlIntercode>
{
    private final DatabaseSchema schema;
    private final List<QuadMapping> mappings;
    private final List<DataSet> datasets;

    private Node graph = null;
    private Node subject = null;
    private Node object = null;


    public PathTranslateVisitor(TranslateVisitor translateVisitor, List<DataSet> datasets)
    {
        this.schema = translateVisitor.getConfiguration().getSchema();
        this.mappings = translateVisitor.getConfiguration().getMappings();
        this.datasets = datasets;
    }


    public SqlIntercode visitElement(Element element, Node graph, Node subject, Node object)
    {
        this.graph = graph;
        return visitElement(element, subject, object);
    }


    SqlIntercode visitElement(Element element, Node subject, Node object)
    {
        Node prevSubject = this.subject;
        Node prevObject = this.object;

        this.subject = subject;
        this.object = object;

        SqlIntercode result = element.accept(this);

        this.subject = prevSubject;
        this.object = prevObject;

        return result;
    }


    @Override
    public SqlIntercode visit(AlternativePath alternativePath)
    {
        SqlIntercode result = new SqlNoSolution();

        for(Path child : alternativePath.getChildren())
            result = SqlUnion.union(result, visitElement(child, subject, object));

        return result;
    }


    @Override
    public SqlIntercode visit(SequencePath sequencePath)
    {
        List<List<Path>> paths = expandSequencePath(sequencePath.getChildren());
        SqlIntercode result = new SqlNoSolution();

        for(List<Path> path : paths)
        {
            List<Node> nodes = new ArrayList<Node>(path.size() + 1);

            nodes.add(subject);

            for(int i = 0; i < path.size() - 1; i++)
                nodes.add(BlankNode.getNewBlankNode());

            nodes.add(object);


            SqlIntercode subresult = new SqlEmptySolution();

            for(int i = 0; i < path.size(); i++)
                subresult = SqlJoin.join(schema, subresult, visitElement(path.get(i), nodes.get(i), nodes.get(i + 1)),
                        true);

            result = SqlUnion.union(result, subresult);
        }

        return result;
    }


    @Override
    public SqlIntercode visit(InversePath inversePath)
    {
        return visitElement(inversePath.getChild(), object, subject);
    }


    @Override
    public SqlIntercode visit(RepeatedPath repeatedPath)
    {
        assert repeatedPath.getKind() == Kind.OneOrMore;

        BlankNode joinNode = BlankNode.getNewBlankNode();
        String joinName = joinNode.getName();


        // if it is more suitable, the reverse order is used
        if(!(object instanceof VariableOrBlankNode) && subject instanceof VariableOrBlankNode)
        {
            SqlIntercode init = visitElement(repeatedPath.getChild(), joinNode, object);
            SqlIntercode next = visitElement(repeatedPath.getChild(), subject, joinNode);

            if(init instanceof SqlNoSolution)
                return new SqlNoSolution();

            if(SqlRecursive.getPairs(init.getVariables().get(joinName), next.getVariables().get(joinName)).isEmpty())
                return visitElement(repeatedPath.getChild(), subject, object);

            return SqlRecursive.create(init, next, object, joinNode, (VariableOrBlankNode) subject, null);
        }


        Node endNode = (object instanceof VariableOrBlankNode && !object.equals(subject)) ? object :
                BlankNode.getNewBlankNode();
        Node cndNode = (object instanceof VariableOrBlankNode && !object.equals(subject)) ? null : object;
        String endName = ((VariableOrBlankNode) endNode).getName();
        String subjectName = subject instanceof VariableOrBlankNode ? ((VariableOrBlankNode) subject).getName() : null;

        SqlIntercode init = visitElement(repeatedPath.getChild(), subject, joinNode);
        SqlIntercode next = visitElement(repeatedPath.getChild(), joinNode, endNode);


        if(init instanceof SqlNoSolution)
            return new SqlNoSolution();

        if(SqlRecursive.getPairs(init.getVariables().get(joinName), next.getVariables().get(joinName)).isEmpty())
            return visitElement(repeatedPath.getChild(), subject, object);

        if(cndNode instanceof VariableOrBlankNode)
        {
            if(SqlRecursive.getPairs(init.getVariables().get(subjectName), next.getVariables().get(endName)).isEmpty())
                return visitElement(repeatedPath.getChild(), subject, object);
        }
        else if(cndNode != null)
        {
            if(next.getVariables().get(endName).getClasses().stream().noneMatch(r -> r.match(cndNode)))
                return visitElement(repeatedPath.getChild(), subject, object);
        }


        return SqlRecursive.create(init, next, subject, joinNode, (VariableOrBlankNode) endNode, cndNode);
    }


    @Override
    public SqlIntercode visit(NegatedPath negatedPath)
    {
        List<IRI> negIriSet = new LinkedList<IRI>();
        List<IRI> invNegIriSet = new LinkedList<IRI>();

        if(negatedPath.getChild() instanceof IRI)
        {
            negIriSet.add((IRI) negatedPath.getChild());
        }
        else if(negatedPath.getChild() instanceof InversePath)
        {
            invNegIriSet.add((IRI) ((InversePath) negatedPath.getChild()).getChild());
        }
        else
        {
            for(Path child : ((AlternativePath) ((BracketedPath) negatedPath.getChild()).getChild()).getChildren())
            {
                if(child instanceof IRI)
                    negIriSet.add((IRI) child);
                else if(child instanceof InversePath)
                    invNegIriSet.add((IRI) ((InversePath) child).getChild());
            }
        }


        SqlIntercode translatedPattern = new SqlNoSolution();

        if(!negIriSet.isEmpty())
            translatedPattern = SqlUnion.union(translatedPattern, translateNegatedPath(subject, negIriSet, object));

        if(!invNegIriSet.isEmpty())
            translatedPattern = SqlUnion.union(translatedPattern, translateNegatedPath(object, invNegIriSet, subject));

        return translatedPattern;
    }


    @Override
    public SqlIntercode visit(BracketedPath bracketedPath)
    {
        return visitElement(bracketedPath.getChild(), subject, object);
    }


    @Override
    public SqlIntercode visit(IRI predicate)
    {
        return visit((VarOrIri) predicate);
    }


    @Override
    public SqlIntercode visit(Variable predicate)
    {
        return visit((VarOrIri) predicate);
    }


    public SqlIntercode visit(VarOrIri predicate)
    {
        SqlIntercode translatedPattern = new SqlNoSolution();

        matching:
        for(QuadMapping mapping : mappings)
        {
            if(!datasets.isEmpty())
            {
                ConstantIriMapping graphMapping = (ConstantIriMapping) mapping.getGraph();

                if(graphMapping == null)
                    continue;

                IRI graphIri = ((IRI) graphMapping.getValue());
                boolean useDefaultDataset = graph == null;

                if(datasets.stream().filter(d -> d.isDefault() == useDefaultDataset)
                        .noneMatch(d -> d.getSourceSelector().equals(graphIri)))
                    continue;
            }


            if(mapping.match(graph, subject, predicate, object))
            {
                SqlTableAccess translated = new SqlTableAccess(mapping.getTable(), mapping.getCondition());

                processNodeMapping(translated, graph, mapping.getGraph());
                processNodeMapping(translated, subject, mapping.getSubject());
                processNodeMapping(translated, predicate, mapping.getPredicate());
                processNodeMapping(translated, object, mapping.getObject());

                for(UsedVariable usedVariable : translated.getVariables().getValues())
                    if(usedVariable.getClasses().size() > 1)
                        continue matching;

                processNodeCondition(translated, graph, mapping.getGraph());
                processNodeCondition(translated, subject, mapping.getSubject());
                processNodeCondition(translated, predicate, mapping.getPredicate());
                processNodeCondition(translated, object, mapping.getObject());

                if(processNodesCondition(translated, graph, subject, mapping.getGraph(), mapping.getSubject()))
                    continue;

                if(processNodesCondition(translated, graph, predicate, mapping.getGraph(), mapping.getPredicate()))
                    continue;

                if(processNodesCondition(translated, graph, object, mapping.getGraph(), mapping.getObject()))
                    continue;

                if(processNodesCondition(translated, subject, predicate, mapping.getSubject(), mapping.getPredicate()))
                    continue;

                if(processNodesCondition(translated, subject, object, mapping.getSubject(), mapping.getObject()))
                    continue;


                translatedPattern = SqlUnion.union(translatedPattern, translated);
            }
        }

        return translatedPattern;
    }


    private void processNodeMapping(SqlTableAccess translated, Node node, NodeMapping mapping)
    {
        if(!(node instanceof VariableOrBlankNode))
            return;

        String name = node instanceof Variable ? ((Variable) node).getName() : '@' + ((BlankNode) node).getName();

        translated.addVariableClass(name, mapping.getResourceClass());
        translated.addMapping(name, mapping);
    }


    private void processNodeCondition(SqlTableAccess translated, Node node, NodeMapping mapping)
    {
        if(node instanceof VariableOrBlankNode && mapping instanceof ParametrisedMapping)
            translated.addNotNullCondition((ParametrisedMapping) mapping);

        if(!(node instanceof VariableOrBlankNode) && mapping instanceof ParametrisedMapping)
            translated.addValueCondition(node, (ParametrisedMapping) mapping);
    }


    private boolean processNodesCondition(SqlTableAccess translated, Node node1, Node node2, NodeMapping map1,
            NodeMapping map2)
    {
        if(!(node1 instanceof VariableOrBlankNode) || !(node2 instanceof VariableOrBlankNode))
            return false;

        if(!node1.equals(node2))
            return false;

        if(map1 instanceof ConstantMapping && map2 instanceof ConstantMapping)
        {
            if(map1.equals(map2))
                return false;
            else
                return true;
        }

        return false;
    }


    private List<List<Path>> expandSequencePath(List<Path> path)
    {
        LinkedList<List<Path>> paths = new LinkedList<List<Path>>();

        if(path.isEmpty())
        {
            paths.add(path);
        }
        else
        {
            Path first = path.get(0);

            for(List<Path> sub : expandSequencePath(path.subList(1, path.size())))
            {
                if(first instanceof RepeatedPath && ((RepeatedPath) first).getKind() == Kind.ZeroOrOne)
                {
                    LinkedList<Path> result = new LinkedList<Path>();
                    result.add(((RepeatedPath) first).getChild());
                    result.addAll(sub);
                    paths.add(result);
                    paths.add(sub);
                }
                else if(first instanceof RepeatedPath && ((RepeatedPath) first).getKind() == Kind.ZeroOrMore)
                {
                    LinkedList<Path> result = new LinkedList<Path>();
                    result.add(new RepeatedPath(Kind.OneOrMore, ((RepeatedPath) first).getChild()));
                    result.addAll(sub);
                    paths.add(result);
                    paths.add(sub);
                }
                else
                {
                    LinkedList<Path> result = new LinkedList<Path>();
                    result.add(first);
                    result.addAll(sub);
                    paths.add(result);
                }
            }
        }

        return paths;
    }


    private SqlIntercode translateNegatedPath(Node subject, List<IRI> negatedIriSet, Node object)
    {
        SqlIntercode translatedPattern = new SqlNoSolution();

        BlankNode fakePredicate = BlankNode.getNewBlankNode();

        matching:
        for(QuadMapping mapping : mappings)
        {
            if(!datasets.isEmpty())
            {
                ConstantIriMapping graphMapping = (ConstantIriMapping) mapping.getGraph();

                if(graphMapping == null)
                    continue;

                IRI graphIri = ((IRI) graphMapping.getValue());
                boolean useDefaultDataset = graph == null;

                if(datasets.stream().filter(d -> d.isDefault() == useDefaultDataset)
                        .noneMatch(d -> d.getSourceSelector().equals(graphIri)))
                    continue;
            }

            if(mapping.match(graph, subject, fakePredicate, object))
            {
                IRI predicate = (IRI) mapping.getPredicate().getValue();

                if(negatedIriSet.contains(predicate))
                    continue;


                SqlTableAccess translated = new SqlTableAccess(mapping.getTable(), mapping.getCondition());

                processNodeMapping(translated, graph, mapping.getGraph());
                processNodeMapping(translated, subject, mapping.getSubject());
                processNodeMapping(translated, object, mapping.getObject());

                for(UsedVariable usedVariable : translated.getVariables().getValues())
                    if(usedVariable.getClasses().size() > 1)
                        continue matching;

                processNodeCondition(translated, graph, mapping.getGraph());
                processNodeCondition(translated, subject, mapping.getSubject());
                processNodeCondition(translated, object, mapping.getObject());

                if(processNodesCondition(translated, graph, subject, mapping.getGraph(), mapping.getSubject()))
                    continue;

                if(processNodesCondition(translated, graph, object, mapping.getGraph(), mapping.getObject()))
                    continue;

                if(processNodesCondition(translated, subject, object, mapping.getSubject(), mapping.getObject()))
                    continue;


                translatedPattern = SqlUnion.union(translatedPattern, translated);
            }
        }

        return translatedPattern;
    }
}

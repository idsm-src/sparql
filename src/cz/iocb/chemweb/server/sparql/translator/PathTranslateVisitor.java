package cz.iocb.chemweb.server.sparql.translator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.JoinTableQuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedMapping;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.SingleTableQuadMapping;
import cz.iocb.chemweb.server.sparql.parser.Element;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.AlternativePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BracketedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.InversePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.NegatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Path;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RepeatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RepeatedPath.Kind;
import cz.iocb.chemweb.server.sparql.parser.model.triple.SequencePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlEmptySolution;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlJoin;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlNoSolution;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlRecursive;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlTableAccess;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlUnion;



public class PathTranslateVisitor extends ElementVisitor<SqlIntercode>
{
    private static final String variablePrefix = "@pathvar";
    private static int variableId = 0;

    private final Request request;
    private final DatabaseSchema schema;
    private final List<QuadMapping> mappings;
    private final List<DataSet> datasets;

    private Node graph = null;
    private Node subject = null;
    private Node object = null;


    public PathTranslateVisitor(Request request, List<DataSet> datasets)
    {
        this.request = request;
        this.schema = request.getConfiguration().getSchema();
        this.mappings = request.getConfiguration().getMappings();
        this.datasets = datasets;
    }


    public SqlIntercode translate(Node graph, Node subject, Verb predicate, Node object)
    {
        this.graph = graph;
        SqlIntercode intercode = visitElement(predicate, subject, object);

        HashSet<String> restrictions = new HashSet<String>();

        if(graph instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) graph).getSqlName());

        if(subject instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) subject).getSqlName());

        if(predicate instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) predicate).getSqlName());

        if(object instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) object).getSqlName());

        // strip path variables
        return intercode.optimize(request, restrictions, false);
    }


    SqlIntercode visitElement(Element element, Node subject, Node object)
    {
        if(element == null || subject == null || object == null)
            return SqlNoSolution.get();

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
        SqlIntercode result = SqlNoSolution.get();

        for(Path child : alternativePath.getChildren())
            result = SqlUnion.union(result, visitElement(child, subject, object));

        return result;
    }


    @Override
    public SqlIntercode visit(SequencePath sequencePath)
    {
        List<List<Path>> paths = expandSequencePath(sequencePath.getChildren());
        SqlIntercode result = SqlNoSolution.get();

        for(List<Path> path : paths)
        {
            List<Node> nodes = new ArrayList<Node>(path.size() + 1);

            nodes.add(subject);

            for(int i = 0; i < path.size() - 1; i++)
                nodes.add(new Variable(variablePrefix + variableId++));

            nodes.add(object);


            SqlIntercode subresult = SqlEmptySolution.get();

            for(int i = 0; i < path.size(); i++)
            {
                Node join = nodes.get(i);
                Node object = nodes.get(i + 1);

                HashSet<String> restrictions = new HashSet<String>();

                if(subject instanceof VariableOrBlankNode)
                    restrictions.add(((VariableOrBlankNode) subject).getSqlName());

                if(object instanceof VariableOrBlankNode)
                    restrictions.add(((VariableOrBlankNode) object).getSqlName());

                subresult = SqlJoin.join(schema, subresult, visitElement(path.get(i), join, object), restrictions);
            }

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

        Variable joinNode = new Variable(variablePrefix + variableId++);
        String joinName = joinNode.getSqlName();


        // if it is more suitable, the reverse order is used
        if(!(object instanceof VariableOrBlankNode) && subject instanceof VariableOrBlankNode)
        {
            SqlIntercode init = visitElement(repeatedPath.getChild(), joinNode, object);
            SqlIntercode next = visitElement(repeatedPath.getChild(), subject, joinNode);

            if(init == SqlNoSolution.get())
                return SqlNoSolution.get();

            if(SqlRecursive.getPairs(init.getVariables().get(joinName), next.getVariables().get(joinName)).isEmpty())
                return visitElement(repeatedPath.getChild(), subject, object);

            return SqlRecursive.create(init, next, null, joinName, ((VariableOrBlankNode) subject).getSqlName(), null,
                    request, null);
        }


        Node endNode = (object instanceof VariableOrBlankNode && !object.equals(subject)) ? object :
                new Variable(variablePrefix + variableId++);
        Node cndNode = (object instanceof VariableOrBlankNode && !object.equals(subject)) ? null : object;
        String endName = ((VariableOrBlankNode) endNode).getSqlName();
        String subjectName = subject instanceof VariableOrBlankNode ? ((VariableOrBlankNode) subject).getSqlName() :
                null;

        SqlIntercode init = visitElement(repeatedPath.getChild(), subject, joinNode);
        SqlIntercode next = visitElement(repeatedPath.getChild(), joinNode, endNode);


        if(init == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(SqlRecursive.getPairs(init.getVariables().get(joinName), next.getVariables().get(joinName)).isEmpty())
            return visitElement(repeatedPath.getChild(), subject, object);

        if(cndNode instanceof VariableOrBlankNode)
        {
            if(SqlRecursive.getPairs(init.getVariables().get(subjectName), next.getVariables().get(endName)).isEmpty())
                return visitElement(repeatedPath.getChild(), subject, object);
        }
        else if(cndNode != null)
        {
            if(next.getVariables().get(endName).getClasses().stream().noneMatch(r -> r.match(cndNode, request)))
                return visitElement(repeatedPath.getChild(), subject, object);
        }


        return SqlRecursive.create(init, next, subjectName, joinName, ((VariableOrBlankNode) endNode).getSqlName(),
                cndNode, request, null);
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


        SqlIntercode translatedPattern = SqlNoSolution.get();

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
        SqlIntercode translatedPattern = SqlNoSolution.get();

        for(QuadMapping mapping : mappings)
        {
            if(!datasets.isEmpty())
            {
                ConstantIriMapping graphMapping = mapping.getGraph();

                if(graphMapping == null)
                    continue;

                IRI graphIri = ((IRI) graphMapping.getValue());
                boolean useDefaultDataset = graph == null;

                if(datasets.stream().filter(d -> d.isDefault() == useDefaultDataset)
                        .noneMatch(d -> d.getSourceSelector().equals(graphIri)))
                    continue;
            }


            if(mapping.match(graph, subject, predicate, object, request))
            {
                SqlIntercode translated = translateMapping(mapping, graph, subject, predicate, object);
                translatedPattern = SqlUnion.union(translatedPattern, translated);
            }
        }

        return translatedPattern;
    }


    private void processNodeMapping(SqlTableAccess translated, Node node, NodeMapping mapping)
    {
        if(node == null)
            return;

        if(node instanceof VariableOrBlankNode)
        {
            String name = ((VariableOrBlankNode) node).getSqlName();

            translated.addVariableClass(name, mapping.getResourceClass());
            translated.addMapping(name, mapping);
        }

        if(node instanceof VariableOrBlankNode && mapping instanceof ParametrisedMapping)
            translated.addNotNullCondition((ParametrisedMapping) mapping);

        if(!(node instanceof VariableOrBlankNode) && mapping instanceof ParametrisedMapping)
            translated.addValueCondition(node, (ParametrisedMapping) mapping);
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
        SqlIntercode translatedPattern = SqlNoSolution.get();

        Variable fakePredicate = new Variable(variablePrefix + variableId++);

        for(QuadMapping mapping : mappings)
        {
            if(!datasets.isEmpty())
            {
                ConstantIriMapping graphMapping = mapping.getGraph();

                if(graphMapping == null)
                    continue;

                IRI graphIri = ((IRI) graphMapping.getValue());
                boolean useDefaultDataset = graph == null;

                if(datasets.stream().filter(d -> d.isDefault() == useDefaultDataset)
                        .noneMatch(d -> d.getSourceSelector().equals(graphIri)))
                    continue;
            }

            if(mapping.match(graph, subject, fakePredicate, object, request))
            {
                IRI predicate = (IRI) mapping.getPredicate().getValue();

                if(negatedIriSet.contains(predicate))
                    continue;

                SqlIntercode translated = translateMapping(mapping, graph, subject, null, object);
                translatedPattern = SqlUnion.union(translatedPattern, translated);
            }
        }

        return translatedPattern;
    }


    private SqlIntercode translateMapping(QuadMapping qmapping, Node graph, Node subject, Node predicate, Node object)
    {
        if(qmapping instanceof SingleTableQuadMapping)
        {
            SingleTableQuadMapping mapping = (SingleTableQuadMapping) qmapping;

            SqlTableAccess translated = new SqlTableAccess(schema, mapping.getTable(), mapping.getCondition());

            processNodeMapping(translated, graph, mapping.getGraph());
            processNodeMapping(translated, subject, mapping.getSubject());
            processNodeMapping(translated, predicate, mapping.getPredicate());
            processNodeMapping(translated, object, mapping.getObject());

            return translated;
        }
        else if(qmapping instanceof JoinTableQuadMapping)
        {
            JoinTableQuadMapping mapping = (JoinTableQuadMapping) qmapping;

            Variable joinvar = new Variable(variablePrefix + variableId++);


            SqlTableAccess subjectAcess = new SqlTableAccess(schema, mapping.getSubjectTable(),
                    mapping.getSubjectCondition());

            processNodeMapping(subjectAcess, graph, mapping.getGraph());
            processNodeMapping(subjectAcess, subject, mapping.getSubject());
            processNodeMapping(subjectAcess, predicate, mapping.getPredicate());
            processNodeMapping(subjectAcess, joinvar, mapping.getSubjectJoinMapping());


            SqlTableAccess objectAcess = new SqlTableAccess(schema, mapping.getObjectTable(),
                    mapping.getObjectCondition());

            //processNodeMapping(objectAcess, graph, mapping.getGraph()); // useless for ConstantIriMapping
            processNodeMapping(objectAcess, joinvar, mapping.getObjectJoinMapping());
            //processNodeMapping(objectAcess, predicate, mapping.getPredicate()); // useless for ConstantIriMapping
            processNodeMapping(objectAcess, object, mapping.getObject());

            return SqlJoin.join(schema, subjectAcess, objectAcess);
        }

        return null;
    }
}

package cz.iocb.chemweb.server.sparql.translator;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.InternalNodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.JoinTableQuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.chemweb.server.sparql.mapping.MappedNode;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.SingleTableQuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.InternalResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.Element;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
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
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlBind;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlDistinct;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlEmptySolution;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlFilter;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlJoin;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlNoSolution;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlRecursive;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlTableAccess;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlTableAccess.Condition;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlUnion;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlIri;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlLiteral;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlUnaryLogical;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlVariable;



public class PathTranslateVisitor extends ElementVisitor<SqlIntercode>
{
    private static final String variablePrefix = "@pathvar";

    private final TranslateVisitor parent;
    private final List<DataSet> datasets;

    private Node graph = null;
    private Node subject = null;
    private Node object = null;


    public PathTranslateVisitor(TranslateVisitor parent, List<DataSet> datasets)
    {
        this.parent = parent;
        this.datasets = datasets;
    }


    public SqlIntercode translate(Node graph, Node subject, Verb predicate, Node object)
    {
        if(predicate instanceof Path)
            predicate = new PathRewriteVisitor().visitElement(predicate);


        this.graph = graph;
        SqlIntercode intercode = visitElement(predicate, subject, object);

        Set<String> restrictions = new HashSet<String>();

        if(graph instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) graph).getSqlName());

        if(subject instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) subject).getSqlName());

        if(predicate instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) predicate).getSqlName());

        if(object instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) object).getSqlName());

        // strip path variables
        return intercode.restrict(restrictions);
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
        List<Path> path = sequencePath.getChildren();

        if(path.isEmpty())
        {
            if(subject instanceof VariableOrBlankNode && object instanceof VariableOrBlankNode)
            {
                String subjectName = ((VariableOrBlankNode) subject).getSqlName();
                String objectName = ((VariableOrBlankNode) object).getSqlName();

                SqlIntercode subjects = visitElement(parent.createVariable(variablePrefix), subject,
                        parent.createVariable(variablePrefix));

                SqlIntercode objects = visitElement(parent.createVariable(variablePrefix),
                        parent.createVariable(variablePrefix), subject);

                Set<String> distinctVariables = new HashSet<String>();
                distinctVariables.add(subjectName);
                distinctVariables.add(objectName);

                if(graph instanceof VariableOrBlankNode)
                    distinctVariables.add(((VariableOrBlankNode) graph).getSqlName());

                SqlIntercode child = SqlDistinct.create(SqlUnion.union(subjects, objects), distinctVariables);

                return SqlBind.bind(objectName, SqlVariable.create(subjectName, child.getVariables()), child);
            }
            else if(subject instanceof VariableOrBlankNode)
            {
                String subjectName = ((VariableOrBlankNode) subject).getSqlName();

                SqlExpressionIntercode expression = getExpression(object, new UsedVariables());

                return SqlBind.bind(subjectName, expression, SqlEmptySolution.get());
            }
            else if(object instanceof VariableOrBlankNode)
            {
                String objectName = ((VariableOrBlankNode) object).getSqlName();

                SqlExpressionIntercode expression = getExpression(subject, new UsedVariables());

                return SqlBind.bind(objectName, expression, SqlEmptySolution.get());
            }
            else
            {
                SqlIntercode child = SqlEmptySolution.get();

                SqlExpressionIntercode filter = getComparisonExpression(subject, object, false, child.getVariables());

                return SqlFilter.filter(asList(filter), child);
            }
        }


        List<Node> nodes = new ArrayList<Node>(path.size() + 1);

        nodes.add(subject);

        for(int i = 0; i < path.size() - 1; i++)
            nodes.add(parent.createVariable(variablePrefix));

        nodes.add(object);


        SqlIntercode result = SqlEmptySolution.get();

        for(int i = 0; i < path.size(); i++)
            result = (SqlJoin.join(result, visitElement(path.get(i), nodes.get(i), nodes.get(i + 1))));

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
        Set<String> distinct = Stream.of(subject, object).filter(e -> e instanceof VariableOrBlankNode)
                .map(e -> ((VariableOrBlankNode) e).getSqlName()).collect(toSet());


        if(repeatedPath.getKind() == Kind.ZeroOrOne)
        {
            SqlIntercode child = SqlDistinct.create(visitElement(repeatedPath.getChild(), subject, object), distinct);
            SqlExpressionIntercode filter = getComparisonExpression(subject, object, true, child.getVariables());

            return SqlFilter.filter(asList(filter), child);
        }


        Variable joinNode = parent.createVariable(variablePrefix);
        String joinName = joinNode.getSqlName();


        // if it is more suitable, the reverse order is used
        if(!(object instanceof VariableOrBlankNode) && subject instanceof VariableOrBlankNode)
        {
            SqlIntercode init = visitElement(repeatedPath.getChild(), subject, object);
            SqlIntercode next = visitElement(repeatedPath.getChild(), subject, joinNode);

            return SqlRecursive.create(init, next, null, joinName, ((VariableOrBlankNode) subject).getSqlName());
        }


        //NOTE: repeatedPath.getKind() == Kind.OneOrMore implies cndNode == null
        Node cndNode = (object instanceof VariableOrBlankNode && !object.equals(subject)) ? null : object;
        Node endNode = (object instanceof VariableOrBlankNode && !object.equals(subject)) ? object :
                parent.createVariable(variablePrefix);

        String beginName = subject instanceof VariableOrBlankNode ? ((VariableOrBlankNode) subject).getSqlName() : null;
        String endName = ((VariableOrBlankNode) endNode).getSqlName();

        SqlIntercode init = visitElement(repeatedPath.getChild(), subject, endNode);
        SqlIntercode next = visitElement(repeatedPath.getChild(), joinNode, endNode);

        UsedVariable initBeginVar = init.getVariables().get(beginName);
        UsedVariable nextEndVar = next.getVariables().get(endName);


        if(cndNode instanceof VariableOrBlankNode && !(new UsedPairedVariable(initBeginVar, nextEndVar)).isJoinable()
                || cndNode != null && nextEndVar.getClasses().stream().noneMatch(r -> r.match(cndNode)))
            return SqlDistinct.create(visitElement(repeatedPath.getChild(), subject, object), distinct);


        SqlIntercode intercode = SqlRecursive.create(init, next, beginName, joinName, endName);

        if(cndNode != null)
        {
            SqlExpressionIntercode filter = getComparisonExpression(cndNode, endNode, false, intercode.getVariables());
            intercode = SqlFilter.filter(asList(filter), intercode);
        }

        if(repeatedPath.getKind() == Kind.ZeroOrMore)
        {
            //NOTE: variants in which subject is equal to object are processed elsewhere

            SqlExpressionIntercode filter = getComparisonExpression(subject, object, true, intercode.getVariables());
            intercode = SqlFilter.filter(asList(filter), intercode);
        }

        return intercode;
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

        for(QuadMapping mapping : Request.currentRequest().getConfiguration().getMappings(parent.getService()))
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
            else if(graph == null && mapping.getGraph() != null)
            {
                continue;
            }

            if(mapping.match(graph, subject, predicate, object))
            {
                SqlIntercode translated = translateMapping(mapping, graph, subject, predicate, object);
                translatedPattern = SqlUnion.union(translatedPattern, translated);
            }
        }

        return translatedPattern;
    }


    private SqlIntercode translateNegatedPath(Node subject, List<IRI> negatedIriSet, Node object)
    {
        SqlIntercode translatedPattern = SqlNoSolution.get();

        Variable fakePredicate = parent.createVariable(variablePrefix);

        for(QuadMapping mapping : Request.currentRequest().getConfiguration().getMappings(parent.getService()))
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

            if(mapping.match(graph, subject, fakePredicate, object))
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

            List<MappedNode> maps = new ArrayList<MappedNode>();
            maps.add(new MappedNode(graph, mapping.getGraph()));
            maps.add(new MappedNode(subject, mapping.getSubject()));
            maps.add(new MappedNode(predicate, mapping.getPredicate()));
            maps.add(new MappedNode(object, mapping.getObject()));

            return SqlTableAccess.create(mapping.getTable(), mapping.getCondition(), maps);
        }
        else if(qmapping instanceof JoinTableQuadMapping)
        {
            JoinTableQuadMapping mapping = (JoinTableQuadMapping) qmapping;

            List<Table> tables = mapping.getTables();
            List<Condition> conditions = mapping.getConditions();
            List<JoinColumns> joinColumnsPairs = mapping.getJoinColumnsPairs();

            ResourceClass resourceClass = null;
            NodeMapping nodeMapping = null;
            Node node = subject;

            SqlIntercode result = SqlEmptySolution.get();

            for(int i = 0; i < tables.size(); i++)
            {
                List<MappedNode> maps = new ArrayList<MappedNode>();

                if(i == 0)
                {
                    maps.add(new MappedNode(graph, mapping.getGraph()));
                    maps.add(new MappedNode(predicate, mapping.getPredicate()));
                }

                if(i == 0)
                    nodeMapping = mapping.getSubject();
                else
                    nodeMapping = new InternalNodeMapping(resourceClass, joinColumnsPairs.get(i - 1).getRightColumns());

                maps.add(new MappedNode(node, nodeMapping));

                if(i < tables.size() - 1)
                {
                    resourceClass = new InternalResourceClass(joinColumnsPairs.get(i).getLeftColumns().size());
                    nodeMapping = new InternalNodeMapping(resourceClass, joinColumnsPairs.get(i).getLeftColumns());
                    node = parent.createVariable(variablePrefix);
                }
                else
                {
                    nodeMapping = mapping.getObject();
                    node = object;
                }

                maps.add(new MappedNode(node, nodeMapping));

                Table table = tables.get(i);
                Condition cnds = conditions != null ? conditions.get(i) : null;

                SqlIntercode acess = SqlTableAccess.create(table, cnds, maps);

                result = SqlJoin.join(result, acess);
            }

            return result;
        }

        return null;
    }


    private static SqlExpressionIntercode getComparisonExpression(Node node1, Node node2, boolean not,
            UsedVariables variables)
    {
        SqlExpressionIntercode expr1 = getExpression(node1, variables);
        SqlExpressionIntercode expr2 = getExpression(node2, variables);

        SqlExpressionIntercode compare = SqlBuiltinCall.create("sameterm", false, asList(expr1, expr2));

        if(not)
            return SqlUnaryLogical.create(compare);
        else
            return compare;
    }


    private static SqlExpressionIntercode getExpression(Node node, UsedVariables variables)
    {
        if(node instanceof VariableOrBlankNode)
            return SqlVariable.create(((VariableOrBlankNode) node).getSqlName(), variables);

        if(node instanceof IRI)
            return SqlIri.create((IRI) node);

        if(node instanceof Literal)
            return SqlLiteral.create((Literal) node);

        return null;
    }
}

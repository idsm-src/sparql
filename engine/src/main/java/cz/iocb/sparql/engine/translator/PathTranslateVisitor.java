package cz.iocb.sparql.engine.translator;

import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.ConstantMapping;
import cz.iocb.sparql.engine.mapping.InternalNodeMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.MappedNode;
import cz.iocb.sparql.engine.mapping.NodeMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedIriMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedMapping;
import cz.iocb.sparql.engine.mapping.QuadMapping;
import cz.iocb.sparql.engine.mapping.SingleTableQuadMapping;
import cz.iocb.sparql.engine.mapping.classes.InternalResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.Element;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.DataSet;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.VarOrIri;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.AlternativePath;
import cz.iocb.sparql.engine.parser.model.triple.BracketedPath;
import cz.iocb.sparql.engine.parser.model.triple.InversePath;
import cz.iocb.sparql.engine.parser.model.triple.NegatedPath;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.parser.model.triple.Path;
import cz.iocb.sparql.engine.parser.model.triple.RepeatedPath;
import cz.iocb.sparql.engine.parser.model.triple.RepeatedPath.Kind;
import cz.iocb.sparql.engine.parser.model.triple.SequencePath;
import cz.iocb.sparql.engine.parser.model.triple.Verb;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.imcode.SqlBind;
import cz.iocb.sparql.engine.translator.imcode.SqlDistinct;
import cz.iocb.sparql.engine.translator.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.translator.imcode.SqlFilter;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode;
import cz.iocb.sparql.engine.translator.imcode.SqlJoin;
import cz.iocb.sparql.engine.translator.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.translator.imcode.SqlRecursive;
import cz.iocb.sparql.engine.translator.imcode.SqlTableAccess;
import cz.iocb.sparql.engine.translator.imcode.SqlUnion;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



public class PathTranslateVisitor extends ElementVisitor<SqlIntercode>
{
    private static final String variablePrefix = "@pathvar";

    private final Request request;

    private final TranslateVisitor parent;
    private final List<DataSet> datasets;

    private Node graph = null;
    private Node subject = null;
    private Node object = null;


    public PathTranslateVisitor(Request request, TranslateVisitor parent, List<DataSet> datasets)
    {
        this.request = request;
        this.parent = parent;
        this.datasets = datasets;
    }


    public SqlIntercode translate(Node graph, Node subject, Verb predicate, Node object)
    {
        if(predicate instanceof Path)
            predicate = new PathRewriteVisitor().visitElement(predicate);


        this.graph = graph;
        SqlIntercode intercode = visitElement(predicate, subject, object);

        return intercode;
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
        return SqlUnion.union(request,
                alternativePath.getChildren().stream().map(c -> visitElement(c, subject, object)).toList());
    }


    @Override
    public SqlIntercode visit(SequencePath sequencePath)
    {
        List<Path> path = sequencePath.getChildren();

        List<Node> nodes = new ArrayList<Node>(path.size() + 1);

        nodes.add(subject);

        for(int i = 0; i < path.size() - 1; i++)
            nodes.add(parent.createVariable(variablePrefix));

        nodes.add(object);


        SqlIntercode result = SqlEmptySolution.get();

        for(int i = 0; i < path.size(); i++)
            result = (SqlJoin.join(request, result, visitElement(path.get(i), nodes.get(i), nodes.get(i + 1))));

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
            return SqlDistinct.create(request, SqlUnion.union(request, List.of(translateZeroPath(subject, object),
                    visitElement(repeatedPath.getChild(), subject, object))), distinct);


        Variable joinNode = parent.createVariable(variablePrefix);
        String joinName = joinNode.getSqlName();
        String graphName = graph instanceof VariableOrBlankNode v ? v.getSqlName() : null;


        // if it is more suitable, the reverse order is used
        if(!(object instanceof VariableOrBlankNode) && subject instanceof VariableOrBlankNode)
        {
            String endName = ((VariableOrBlankNode) subject).getSqlName();

            SqlIntercode init = repeatedPath.getKind() == Kind.ZeroOrMore ? translateZeroPath(object, subject) :
                    visitElement(repeatedPath.getChild(), subject, object);

            SqlIntercode next = visitElement(repeatedPath.getChild(), subject, joinNode);

            return SqlRecursive.create(request, init, next, null, joinName, endName, graphName);
        }


        Node cndNode = (object instanceof VariableOrBlankNode && !object.equals(subject)) ? null : object;
        Node endNode = (object instanceof VariableOrBlankNode && !object.equals(subject)) ? object :
                parent.createVariable(variablePrefix);

        String beginName = subject instanceof VariableOrBlankNode ? ((VariableOrBlankNode) subject).getSqlName() : null;
        String endName = ((VariableOrBlankNode) endNode).getSqlName();

        SqlIntercode init = repeatedPath.getKind() == Kind.ZeroOrMore ? translateZeroPath(subject, endNode) :
                visitElement(repeatedPath.getChild(), subject, endNode);

        SqlIntercode next = visitElement(repeatedPath.getChild(), joinNode, endNode);

        SqlIntercode intercode = SqlRecursive.create(request, init, next, beginName, joinName, endName, graphName);

        if(cndNode != null)
        {
            SqlExpressionIntercode filter = getComparisonExpression(request, cndNode, endNode, false,
                    intercode.getVariables());
            intercode = SqlFilter.filter(request, List.of(filter), intercode);
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
                if(child instanceof IRI iri)
                    negIriSet.add(iri);
                else if(child instanceof InversePath inversePath)
                    invNegIriSet.add((IRI) inversePath.getChild());
            }
        }


        if(!negIriSet.isEmpty() && invNegIriSet.isEmpty())
            return translateNegatedPath(subject, negIriSet, object);

        if(!invNegIriSet.isEmpty() && negIriSet.isEmpty())
            return translateNegatedPath(object, invNegIriSet, subject);

        return SqlUnion.union(request, List.of(translateNegatedPath(subject, negIriSet, object),
                translateNegatedPath(object, invNegIriSet, subject)));
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
        List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

        for(QuadMapping mapping : request.getConfiguration().getMappings(parent.getService()))
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

            if(mapping.match(request, graph, subject, predicate, object))
            {
                SqlIntercode translated = translateMapping(mapping, graph, subject, predicate, object,
                        new Conditions(true));
                unionList.add(translated);
            }
        }

        return SqlUnion.union(request, unionList);
    }


    private SqlIntercode translateNegatedPath(Node subject, List<IRI> negatedIriSet, Node object)
    {
        List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

        Variable fakePredicate = parent.createVariable(variablePrefix);

        for(QuadMapping mapping : request.getConfiguration().getMappings(parent.getService()))
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

            if(mapping.match(request, graph, subject, fakePredicate, object))
            {
                if(mapping.getPredicate() instanceof ConstantIriMapping constIriMapping)
                {
                    IRI predicate = (IRI) constIriMapping.getValue();

                    if(negatedIriSet.contains(predicate))
                        continue;

                    SqlIntercode translated = translateMapping(mapping, graph, subject, null, object,
                            new Conditions(true));
                    unionList.add(translated);
                }
                else
                {
                    Conditions extraConditions = new Conditions(true);

                    for(IRI node : negatedIriSet)
                    {
                        if(mapping.getPredicate().match(request, node))
                        {
                            ParametrisedIriMapping pm = (ParametrisedIriMapping) mapping.getPredicate();
                            List<Column> columns = pm.getColumns(request);
                            List<Column> values = request.getColumns(pm.getResourceClass(request), node);
                            Conditions conditions = new Conditions(false);

                            for(int i = 0; i < columns.size(); i++)
                            {
                                Condition isNull = new Condition();
                                isNull.addIsNull(columns.get(i));
                                conditions.add(isNull);

                                Condition areNotEqual = new Condition();
                                areNotEqual.addAreNotEqual(columns.get(i), values.get(i));
                                conditions.add(areNotEqual);
                            }

                            extraConditions = Conditions.and(extraConditions, conditions);
                        }
                    }

                    SqlIntercode translated = translateMapping(mapping, graph, subject, null, object, extraConditions);
                    unionList.add(translated);
                }
            }
        }

        return SqlUnion.union(request, unionList);
    }


    private SqlIntercode translateZeroPath(Node subject, Node object)
    {
        if(subject instanceof VariableOrBlankNode subjectVar && object instanceof VariableOrBlankNode objectVar)
        {
            String subjectName = subjectVar.getSqlName();
            String objectName = objectVar.getSqlName();

            SqlIntercode subjects = visitElement(parent.createVariable(variablePrefix), subject,
                    parent.createVariable(variablePrefix));

            SqlIntercode objects = visitElement(parent.createVariable(variablePrefix),
                    parent.createVariable(variablePrefix), subject);

            Set<String> distinctVariables = new HashSet<String>();
            distinctVariables.add(subjectName);
            distinctVariables.add(objectName);

            if(graph instanceof VariableOrBlankNode graphVar)
                distinctVariables.add(graphVar.getSqlName());

            SqlIntercode union = SqlUnion.union(request, List.of(subjects, objects));

            SqlIntercode bind = SqlBind.bind(request, objectName, SqlVariable.create(subjectName, union.getVariables()),
                    union);

            return SqlDistinct.create(request, bind, distinctVariables);
        }
        else if(subject instanceof VariableOrBlankNode variable)
        {
            String subjectName = variable.getSqlName();

            SqlExpressionIntercode expression = getExpression(request, object, new UsedVariables());

            return SqlBind.bind(request, subjectName, expression, SqlEmptySolution.get());
        }
        else if(object instanceof VariableOrBlankNode variable)
        {
            String objectName = variable.getSqlName();

            SqlExpressionIntercode expression = getExpression(request, subject, new UsedVariables());

            return SqlBind.bind(request, objectName, expression, SqlEmptySolution.get());
        }
        else
        {
            SqlIntercode child = SqlEmptySolution.get();

            SqlExpressionIntercode filter = getComparisonExpression(request, subject, object, false,
                    child.getVariables());

            return SqlFilter.filter(request, List.of(filter), child);
        }
    }


    private SqlIntercode translateMapping(QuadMapping qmapping, Node graph, Node subject, Node predicate, Node object,
            Conditions predicateConditions)
    {
        if(qmapping instanceof SingleTableQuadMapping mapping)
        {
            Conditions conditions = Conditions.and(mapping.getConditions(), predicateConditions);

            List<MappedNode> maps = new ArrayList<MappedNode>();
            maps.add(new MappedNode(graph, mapping.getGraph()));
            maps.add(new MappedNode(subject, mapping.getSubject()));
            maps.add(new MappedNode(predicate, mapping.getPredicate()));
            maps.add(new MappedNode(object, mapping.getObject()));

            return getTableAccess(request, mapping.getTable(), conditions, maps);
        }
        else if(qmapping instanceof JoinTableQuadMapping mapping)
        {
            List<Table> tables = mapping.getTables();
            List<JoinColumns> joinColumnsPairs = mapping.getJoinColumnsPairs();

            ResourceClass resourceClass = null;
            Node node = subject;

            SqlIntercode result = SqlEmptySolution.get();

            for(int i = 0; i < tables.size(); i++)
            {
                List<MappedNode> maps = new ArrayList<MappedNode>();

                if(i == 0)
                    maps.add(new MappedNode(graph, mapping.getGraph()));

                if(i == mapping.getSubjectTableIdx())
                    maps.add(new MappedNode(subject, mapping.getSubject()));

                if(i == mapping.getPredicateTableIdx())
                    maps.add(new MappedNode(predicate, mapping.getPredicate()));

                if(i == mapping.getObjectTableIdx())
                    maps.add(new MappedNode(object, mapping.getObject()));


                if(i > 0)
                {
                    NodeMapping nodeMapping = new InternalNodeMapping(resourceClass,
                            joinColumnsPairs.get(i - 1).getRightColumns());
                    maps.add(new MappedNode(node, nodeMapping));
                }

                if(i < tables.size() - 1)
                {
                    resourceClass = new InternalResourceClass(joinColumnsPairs.get(i).getLeftColumns().size());
                    NodeMapping nodeMapping = new InternalNodeMapping(resourceClass,
                            joinColumnsPairs.get(i).getLeftColumns());
                    node = parent.createVariable(variablePrefix);
                    maps.add(new MappedNode(node, nodeMapping));
                }


                Table table = tables.get(i);
                Conditions conditions = mapping.getConditions().get(i);

                if(i == mapping.getPredicateTableIdx())
                    conditions = Conditions.and(conditions, predicateConditions);

                SqlIntercode acess = getTableAccess(request, table, conditions, maps);

                result = SqlJoin.join(request, result, acess);
            }

            return result;
        }

        return null;
    }


    private static SqlIntercode getTableAccess(Request request, Table table, Conditions extraCondition,
            List<MappedNode> maps)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        Condition condition = new Condition();
        UsedVariables variables = new UsedVariables();

        for(MappedNode map : maps)
        {
            Node node = map.getNode();
            NodeMapping mapping = map.getMapping();

            if(node == null)
                continue;

            if(!(node instanceof VariableOrBlankNode) && mapping instanceof ConstantMapping)
                continue; //NOTE: already checked

            ResourceClass resourceClass = mapping.getResourceClass(request);
            List<Column> columns = mapping.getColumns(request);

            for(Column column : columns)
                if(schema.isNullableColumn(table, column))
                    condition.addIsNotNull(column);

            if(node instanceof VariableOrBlankNode variable)
            {
                String variableName = variable.getSqlName();
                UsedVariable other = variables.get(variableName);

                if(other == null)
                {
                    variables.add(new UsedVariable(variableName, resourceClass, columns, false));
                }
                else if(other.getResourceClass() == resourceClass)
                {
                    List<Column> current = other.getMapping(resourceClass);
                    condition.addAreEqual(columns, current);
                }
                else
                {
                    //NOTE: CommonIriClass cannot be used in mappings
                    assert other.getResourceClass().getGeneralClass() != resourceClass.getGeneralClass();
                    return SqlNoSolution.get();
                }
            }
            else if(mapping instanceof ParametrisedMapping)
            {
                List<Column> values = request.getColumns(mapping.getResourceClass(request), node);
                condition.addAreEqual(columns, values);
            }
        }

        return SqlTableAccess.create(table, Conditions.and(extraCondition, condition), variables);
    }


    private static SqlExpressionIntercode getComparisonExpression(Request request, Node node1, Node node2, boolean not,
            UsedVariables variables)
    {
        SqlExpressionIntercode expr1 = getExpression(request, node1, variables);
        SqlExpressionIntercode expr2 = getExpression(request, node2, variables);

        SqlExpressionIntercode compare = SqlBuiltinCall.create(request, "sameterm", false, List.of(expr1, expr2));

        if(not)
            return SqlUnaryLogical.create(compare);
        else
            return compare;
    }


    private static SqlExpressionIntercode getExpression(Request request, Node node, UsedVariables variables)
    {
        return switch(node)
        {
            case VariableOrBlankNode variable -> SqlVariable.create(variable.getSqlName(), variables);
            case IRI iri -> SqlIri.create(request, iri);
            case Literal literal -> SqlLiteral.create(request, literal);
            default -> null;
        };
    }
}

package cz.iocb.sparql.engine.translator;

import static cz.iocb.sparql.engine.translator.TermGenerator.getIri;
import static cz.iocb.sparql.engine.translator.TermGenerator.getTerm;
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
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.imcode.SqlBind;
import cz.iocb.sparql.engine.imcode.SqlDistinct;
import cz.iocb.sparql.engine.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.imcode.SqlFilter;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlJoin;
import cz.iocb.sparql.engine.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.imcode.SqlRecursive;
import cz.iocb.sparql.engine.imcode.SqlTableAccess;
import cz.iocb.sparql.engine.imcode.SqlUnion;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.ConstantMapping;
import cz.iocb.sparql.engine.mapping.InternalNodeMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.ParametrisedIriMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedMapping;
import cz.iocb.sparql.engine.mapping.QuadMapping;
import cz.iocb.sparql.engine.mapping.SingleTableQuadMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.InternalResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.base.Element;
import cz.iocb.sparql.engine.model.triple.AlternativePath;
import cz.iocb.sparql.engine.model.triple.BracketedPath;
import cz.iocb.sparql.engine.model.triple.InversePath;
import cz.iocb.sparql.engine.model.triple.NegatedPath;
import cz.iocb.sparql.engine.model.triple.Path;
import cz.iocb.sparql.engine.model.triple.RepeatedPath;
import cz.iocb.sparql.engine.model.triple.RepeatedPath.Kind;
import cz.iocb.sparql.engine.model.triple.SequencePath;
import cz.iocb.sparql.engine.model.triple.Verb;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Translates a triple pattern, possibly with a property path, into intermediate code over the quad mappings of the
 * current service: plain predicates become unions of table accesses over the matching mappings, alternatives unions,
 * sequences joins through fresh variables, repetitions recursive code and negated property sets filtered accesses.
 */
public class PathTranslateVisitor extends ElementVisitor<SqlIntercode>
{
    /**
     * Term bound to a position of a table access, with the mapping providing it.
     *
     * @param term the RDF term
     * @param mapping the term mapping
     */
    private static record MappedTerm(RdfTerm term, TermMapping mapping)
    {
    }


    /**
     * Name prefix of the fresh variables.
     */
    private static final String variablePrefix = "@pathvar";

    /**
     * Current request.
     */
    private final Request request;

    /**
     * Translator allocating fresh variables.
     */
    private final TranslateVisitor parent;

    /**
     * Quad mappings in effect.
     */
    private final List<QuadMapping> mappings;

    /**
     * Graph of the pattern being translated; null for the default graph.
     */
    private RdfTerm graph = null;

    /**
     * Subject of the path element being translated.
     */
    private RdfTerm subject = null;

    /**
     * Object of the path element being translated.
     */
    private RdfTerm object = null;


    /**
     * Creates the visitor over the given mappings.
     *
     * @param request the current request
     * @param parent translator allocating fresh variables
     * @param mappings the quad mappings
     */
    public PathTranslateVisitor(Request request, TranslateVisitor parent, List<QuadMapping> mappings)
    {
        this.request = request;
        this.parent = parent;
        this.mappings = mappings;
    }


    /**
     * Translates the pattern {@code subject predicate object} in the given graph (null for the default graph).
     *
     * @param graph the graph term
     * @param subject the subject term
     * @param predicate the predicate
     * @param object the object term
     * @return the resulting intermediate code
     */
    public SqlIntercode translate(RdfTerm graph, RdfTerm subject, Verb predicate, RdfTerm object)
    {
        if(predicate instanceof Path)
            predicate = new PathRewriteVisitor().visitElement(predicate);


        this.graph = graph;
        SqlIntercode intercode = visitElement(predicate, subject, object);

        return intercode;
    }


    /**
     * Translates a path element between the given subject and object; a null element or term yields no solution.
     *
     * @param element the element
     * @param subject the subject term
     * @param object the object term
     * @return the resulting intermediate code
     */
    SqlIntercode visitElement(Element element, RdfTerm subject, RdfTerm object)
    {
        if(element == null || subject == null || object == null)
            return SqlNoSolution.get();

        RdfTerm prevSubject = this.subject;
        RdfTerm prevObject = this.object;

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

        List<RdfTerm> nodes = new ArrayList<>(path.size() + 1);

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
        Set<Variable> distinct = Stream.of(subject, object).filter(e -> e instanceof Variable).map(e -> (Variable) e)
                .collect(toSet());

        if(repeatedPath.getKind() == Kind.ZeroOrOne)
            return SqlDistinct.create(request, SqlUnion.union(request, List.of(translateZeroPath(subject, object),
                    visitElement(repeatedPath.getChild(), subject, object))), distinct);


        Variable joinVar = parent.createVariable(variablePrefix);
        Variable graphVar = graph instanceof Variable v ? v : null;


        // if it is more suitable, the reverse order is used
        if(!(object instanceof Variable) && subject instanceof Variable endVar)
        {
            SqlIntercode init = repeatedPath.getKind() == Kind.ZeroOrMore ? translateZeroPath(object, subject) :
                    visitElement(repeatedPath.getChild(), subject, object);

            SqlIntercode next = visitElement(repeatedPath.getChild(), subject, joinVar);

            return SqlRecursive.create(request, init, next, null, joinVar, endVar, graphVar);
        }


        RdfTerm cndTerm = (object instanceof Variable && !object.equals(subject)) ? null : object;
        Variable beginVar = subject instanceof Variable var ? var : null;
        Variable endVar = (object instanceof Variable var && !object.equals(subject)) ? var :
                parent.createVariable(variablePrefix);

        SqlIntercode init = repeatedPath.getKind() == Kind.ZeroOrMore ? translateZeroPath(subject, endVar) :
                visitElement(repeatedPath.getChild(), subject, endVar);

        SqlIntercode next = visitElement(repeatedPath.getChild(), joinVar, endVar);

        SqlIntercode intercode = SqlRecursive.create(request, init, next, beginVar, joinVar, endVar, graphVar);

        if(cndTerm != null)
        {
            SqlExpressionIntercode filter = getComparisonExpression(request, cndTerm, endVar, false,
                    intercode.getVariableBindings());
            intercode = SqlFilter.filter(request, List.of(filter), intercode);
        }

        return intercode;
    }


    @Override
    public SqlIntercode visit(NegatedPath negatedPath)
    {
        List<Iri> negIriSet = new LinkedList<>();
        List<Iri> invNegIriSet = new LinkedList<>();

        if(negatedPath.getChild() instanceof IriNode node)
        {
            negIriSet.add(getIri(node));
        }
        else if(negatedPath.getChild() instanceof InversePath path)
        {
            invNegIriSet.add(getIri((IriNode) path.getChild()));
        }
        else
        {
            for(Path child : ((AlternativePath) ((BracketedPath) negatedPath.getChild()).getChild()).getChildren())
            {
                if(child instanceof IriNode node)
                    negIriSet.add(getIri(node));
                else if(child instanceof InversePath inversePath)
                    invNegIriSet.add(getIri((IriNode) inversePath.getChild()));
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
    public SqlIntercode visit(IriNode predicate)
    {
        return visit((VarOrIri) predicate);
    }


    @Override
    public SqlIntercode visit(VariableNode predicate)
    {
        return visit((VarOrIri) predicate);
    }


    /**
     * Translates a plain predicate: the union of the table accesses of all mappings matching the pattern.
     *
     * @param predicate the predicate: an IRI or a variable
     * @return the resulting intermediate code
     */
    public SqlIntercode visit(VarOrIri predicate)
    {
        RdfTerm predicateTerm = getTerm(predicate);

        List<SqlIntercode> unionList = new ArrayList<>();

        for(QuadMapping mapping : mappings)
        {
            if(mapping.match(request, graph, subject, predicateTerm, object))
            {
                SqlIntercode translated = translateMapping(mapping, graph, subject, predicateTerm, object,
                        new Conditions(true));
                unionList.add(translated);
            }
        }

        return SqlUnion.union(request, unionList);
    }


    /**
     * Translates {@code !(iri1|iri2|...)}: every mapping whose predicate can differ from all listed IRIs, with the
     * predicate constrained accordingly.
     *
     * @param subject the subject term
     * @param negatedIriSet IRIs excluded by the negated property set
     * @param object the object term
     * @return the resulting intermediate code
     */
    private SqlIntercode translateNegatedPath(RdfTerm subject, List<Iri> negatedIriSet, RdfTerm object)
    {
        List<SqlIntercode> unionList = new ArrayList<>();

        Variable fakePredicate = parent.createVariable(variablePrefix);

        for(QuadMapping mapping : mappings)
        {
            if(mapping.match(request, graph, subject, fakePredicate, object))
            {
                if(mapping.getPredicate() instanceof ConstantIriMapping constIriMapping)
                {
                    Iri predicate = (Iri) constIriMapping.getValue();

                    if(negatedIriSet.contains(predicate))
                        continue;

                    SqlIntercode translated = translateMapping(mapping, graph, subject, null, object,
                            new Conditions(true));
                    unionList.add(translated);
                }
                else
                {
                    Conditions extraConditions = new Conditions(true);

                    for(Iri iri : negatedIriSet)
                    {
                        if(mapping.getPredicate().match(request, iri))
                        {
                            ParametrisedIriMapping pm = (ParametrisedIriMapping) mapping.getPredicate();
                            List<Column> columns = pm.getColumns(request);
                            List<Column> values = request.getColumns(pm.getResourceClass(request), iri);
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


    /**
     * Translates the zero-length path: the subject equals the object, ranging over all nodes of the graph when both are
     * variables.
     *
     * @param subject the subject term
     * @param object the object term
     * @return the resulting intermediate code
     */
    private SqlIntercode translateZeroPath(RdfTerm subject, RdfTerm object)
    {
        if(subject instanceof Variable subjectVar && object instanceof Variable objectVar)
        {
            SqlIntercode subjects = visitElement(parent.createVariableNode(variablePrefix), subject,
                    parent.createVariable(variablePrefix));

            SqlIntercode objects = visitElement(parent.createVariableNode(variablePrefix),
                    parent.createVariable(variablePrefix), subject);

            Set<Variable> distinctVariables = new HashSet<>();
            distinctVariables.add(subjectVar);
            distinctVariables.add(objectVar);

            if(graph instanceof Variable graphVar)
                distinctVariables.add(graphVar);

            SqlIntercode union = SqlUnion.union(request, List.of(subjects, objects));

            SqlIntercode bind = SqlBind.bind(request, objectVar,
                    SqlVariable.create(union.getVariableBindings().get(subjectVar)), union);

            return SqlDistinct.create(request, bind, distinctVariables);
        }
        else if(subject instanceof Variable variable)
        {
            SqlExpressionIntercode expression = getExpression(request, object, new VariableBindings());

            return SqlBind.bind(request, variable, expression, SqlEmptySolution.get());
        }
        else if(object instanceof Variable variable)
        {
            SqlExpressionIntercode expression = getExpression(request, subject, new VariableBindings());

            return SqlBind.bind(request, variable, expression, SqlEmptySolution.get());
        }
        else
        {
            SqlIntercode child = SqlEmptySolution.get();

            SqlExpressionIntercode filter = getComparisonExpression(request, subject, object, false,
                    child.getVariableBindings());

            return SqlFilter.filter(request, List.of(filter), child);
        }
    }


    /**
     * Translates one quad mapping into a table access (or a join of table accesses for join mappings) binding the terms
     * of the pattern.
     *
     * @param qmapping the quad mapping
     * @param graph the graph term
     * @param subject the subject term
     * @param predicate the predicate term
     * @param object the object term
     * @param predicateConditions conditions on the predicate columns
     * @return the resulting intermediate code
     */
    private SqlIntercode translateMapping(QuadMapping qmapping, RdfTerm graph, RdfTerm subject, RdfTerm predicate,
            RdfTerm object, Conditions predicateConditions)
    {
        if(qmapping instanceof SingleTableQuadMapping mapping)
        {
            Conditions conditions = Conditions.and(mapping.getConditions(), predicateConditions);

            List<MappedTerm> maps = new ArrayList<>();
            maps.add(new MappedTerm(graph, mapping.getGraph()));
            maps.add(new MappedTerm(subject, mapping.getSubject()));
            maps.add(new MappedTerm(predicate, mapping.getPredicate()));
            maps.add(new MappedTerm(object, mapping.getObject()));

            return getTableAccess(request, mapping.getTable(), conditions, maps, mapping.isDistinct());
        }
        else if(qmapping instanceof JoinTableQuadMapping mapping)
        {
            List<SourceTable> tables = mapping.getTables();
            List<JoinColumns> joinColumnsPairs = mapping.getJoinColumnsPairs();

            ResourceClass resourceClass = null;
            RdfTerm term = subject;

            SqlIntercode result = SqlEmptySolution.get();

            for(int i = 0; i < tables.size(); i++)
            {
                List<MappedTerm> maps = new ArrayList<>();

                if(i == 0)
                    maps.add(new MappedTerm(graph, mapping.getGraph()));

                if(i == mapping.getSubjectTableIdx())
                    maps.add(new MappedTerm(subject, mapping.getSubject()));

                if(i == mapping.getPredicateTableIdx())
                    maps.add(new MappedTerm(predicate, mapping.getPredicate()));

                if(i == mapping.getObjectTableIdx())
                    maps.add(new MappedTerm(object, mapping.getObject()));


                if(i > 0)
                {
                    TermMapping nodeMapping = new InternalNodeMapping(resourceClass,
                            joinColumnsPairs.get(i - 1).getRightColumns());
                    maps.add(new MappedTerm(term, nodeMapping));
                }

                if(i < tables.size() - 1)
                {
                    resourceClass = new InternalResourceClass(joinColumnsPairs.get(i).getTypes());
                    TermMapping nodeMapping = new InternalNodeMapping(resourceClass,
                            joinColumnsPairs.get(i).getLeftColumns());
                    term = parent.createVariable(variablePrefix);
                    maps.add(new MappedTerm(term, nodeMapping));
                }


                SourceTable table = tables.get(i);
                Conditions conditions = mapping.getConditions().get(i);

                if(i == mapping.getPredicateTableIdx())
                    conditions = Conditions.and(conditions, predicateConditions);

                SqlIntercode acess = getTableAccess(request, table, conditions, maps, mapping.getDistinct().get(i));

                result = SqlJoin.join(request, result, acess);
            }

            return result;
        }

        return null;
    }


    /**
     * Table access binding the mapped terms: constants become conditions, repeated variables become equalities, and
     * mapped columns of variables are exposed as bindings.
     *
     * @param request the current request
     * @param table the table
     * @param extraCondition additional conditions on the table
     * @param maps terms bound to the mapped positions
     * @param distinct whether the mapping declares distinct rows
     * @return table access binding the mapped terms: constants become conditions, repeated variables become equalities,
     *         and mapped columns of variables are exposed as bindings
     */
    private static SqlIntercode getTableAccess(Request request, SourceTable table, Conditions extraCondition,
            List<MappedTerm> maps, boolean distinct)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        Condition condition = new Condition();
        VariableBindings bindings = new VariableBindings();

        for(MappedTerm map : maps)
        {
            RdfTerm term = map.term();
            TermMapping mapping = map.mapping();

            if(term == null)
                continue;

            if(!(term instanceof Variable) && mapping instanceof ConstantMapping)
                continue; //NOTE: already checked

            ResourceClass resourceClass = mapping.getResourceClass(request);
            List<Column> columns = mapping.getColumns(request);

            for(Column column : columns)
                if(schema.isNullableColumn(table, column))
                    condition.addIsNotNull(column);

            if(term instanceof Variable variable)
            {
                VariableBinding other = bindings.get(variable);

                if(other == null)
                {
                    bindings.add(new VariableBinding(variable, resourceClass, columns, false));
                }
                else if(other.getClasses().iterator().next().equals(resourceClass))
                {
                    List<Column> current = other.getMapping(resourceClass);
                    condition.addAreEqual(columns, current);
                }
                else
                {
                    //FIXME: common (general) classes cannot be used in mappings
                    assert ResourceClass.areDisjunct(other.getClasses().iterator().next(), resourceClass);
                    return SqlNoSolution.get();
                }
            }
            else if(mapping instanceof ParametrisedMapping)
            {
                List<Column> values = request.getColumns(mapping.getResourceClass(request), term);
                condition.addAreEqual(columns, values);
            }
        }

        Set<Column> distinctColumns = Set.of();

        if(distinct)
        {
            // all table columns touched by the mapped terms, including those bound to constants by the pattern
            distinctColumns = new HashSet<>(bindings.getNonConstantColumns());
            distinctColumns.addAll(condition.getNonConstantColumns());
        }

        return SqlTableAccess.create(table, Conditions.and(extraCondition, condition), bindings, false,
                distinctColumns);
    }


    /**
     * Expression testing that the two terms denote the same RDF term (or not).
     *
     * @param request the current request
     * @param term1 the first term
     * @param term2 the second term
     * @param not whether the test is negated
     * @param bindings the variable bindings
     * @return expression testing that the two terms denote the same RDF term (or not)
     */
    private static SqlExpressionIntercode getComparisonExpression(Request request, RdfTerm term1, RdfTerm term2,
            boolean not, VariableBindings bindings)
    {
        SqlExpressionIntercode expr1 = getExpression(request, term1, bindings);
        SqlExpressionIntercode expr2 = getExpression(request, term2, bindings);

        SqlExpressionIntercode compare = SqlBuiltinCall.create(request, "sameterm", false, List.of(expr1, expr2));

        if(not)
            return SqlUnaryLogical.create(compare);
        else
            return compare;
    }


    /**
     * Expression evaluating the term: a variable reference or a constant.
     *
     * @param request the current request
     * @param term the RDF term
     * @param bindings the variable bindings
     * @return expression evaluating the term: a variable reference or a constant
     */
    private static SqlExpressionIntercode getExpression(Request request, RdfTerm term, VariableBindings bindings)
    {
        return switch(term)
        {
            case Variable variable -> SqlVariable.create(bindings.get(variable));
            case Iri iri -> SqlIri.create(request, iri);
            case Literal literal -> SqlLiteral.create(request, literal);
            default -> null;
        };
    }
}

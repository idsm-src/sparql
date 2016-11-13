package cz.iocb.chemweb.server.sparql.semanticcheck.visitor;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.Position;
import cz.iocb.chemweb.server.sparql.parser.Range;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.triple.AlternativePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.InversePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.NegatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Path;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RepeatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.SequencePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;
import cz.iocb.chemweb.server.sparql.semanticcheck.error.ErrorType;
import cz.iocb.chemweb.server.sparql.semanticcheck.error.SemanticError;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.PropertiesInfo;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.inference.PathInferredTypes;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.Constraint;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.OccurenceEvaluation;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeElement;



/**
 * Property Path visitor that checks path consistency, names of properties used and infers types of its domain and
 * range.
 *
 * Path consistency means, that for consecutive properties 'a' and 'b', there must exist a non-empty intersection
 * between a's range and b's domain.
 *
 */
public class PathVisitorTypes extends ElementVisitor<PathInferredTypes>
{
    private static final TypeElement anyClass = new Constraint(ClassesInfo.anyClass);

    private final PropertiesInfo propInfo;
    private final ClassesInfo classInfo;
    private final Prologue prologue;
    private final OccurenceEvaluation tripleoc;
    private HashSet<SemanticError> semErrors;

    /**
     * Construct path visitor
     *
     * @param propinfo ontology information about properties
     * @param classInfo ontology information about classes
     */
    public PathVisitorTypes(PropertiesInfo propinfo, ClassesInfo classInfo, Prologue prologue)
    {
        this.propInfo = propinfo;
        this.classInfo = classInfo;
        this.prologue = prologue;
        this.tripleoc = new OccurenceEvaluation();
    }

    /**
     * Infer type from a triple.
     *
     * @param triple triple
     * @return Inferred type, domain and range of property path
     */
    public PathInferredTypes getType(Triple triple)
    {
        semErrors = new HashSet<>();
        Verb path = triple.getPredicate();

        PathInferredTypes type = null;
        if(path instanceof IRI)
            type = this.getType((IRI) path);
        else if(path instanceof Path)
        {
            type = getType((Path) path);
        }
        else if(path instanceof Variable)
        {
            PathInferredTypes tmp = new PathInferredTypes(classInfo);
            tmp.addToObjectUnion(PathVisitorTypes.anyClass);
            tmp.addToSubjectUnion(PathVisitorTypes.anyClass);
            type = tmp;
        }

        return type;
    }

    /**
     * Get type of IRI property from ontology
     *
     * @param path path IRI
     * @return domain and range. If not known, uses "any", if property ontology info is not available, it returns null.
     */
    private PathInferredTypes getType(IRI path)
    {
        // todo uri je bez zobaku
        String p = path.toString();

        // get range and domain
        if(propInfo != null)
        {
            TypeElement domain = propInfo.getDomain(p);
            TypeElement range = propInfo.getRange(p);
            PathInferredTypes pathType = new PathInferredTypes(classInfo);

            // if property is not in the ontology info, add semantic error
            if(!propInfo.isProperty(p))
                semErrors
                        .add(new SemanticError(ErrorType.incorrectPredicate, path.getRange(), path.toString(prologue)));

            pathType.setSubject(domain);
            pathType.setObject(range);
            if(domain == null)
                pathType.setSubject(new Constraint(ClassesInfo.anyClass));
            if(range == null)
                pathType.setObject(new Constraint(ClassesInfo.anyClass));
            pathType.setSubpath(path);

            return pathType;
        }
        else
            return null;
    }

    /**
     * Visit path
     *
     * @param path path
     * @return global type
     */
    private PathInferredTypes getType(Path path)
    {
        return visitElement(path);
    }

    /**
     * Checks whether it is possible to connect 2 types, domain and range of two consequent properties.
     *
     * @param a one type
     * @param b second type
     * @return true if types are compatible
     */
    private boolean isConnectable(TypeElement a, TypeElement b)
    {
        return this.tripleoc.visitTripletOccurence(b, a, this.classInfo);
    }

    /**
     * Get type from IRI
     *
     * @param iri IRI
     * @return IRI type
     */
    @Override
    public PathInferredTypes visit(IRI iri)
    {
        PathInferredTypes type = getType(iri);
        return type;
    }

    /**
     * Swaps subject and object type.
     *
     * @param path inverse path
     * @return type
     */
    @Override
    public PathInferredTypes visit(InversePath path)
    {
        PathInferredTypes fromchild = visitElement(path.getChild());
        fromchild.swapSubjectObject();
        return fromchild;
    }

    /**
     * Checks son elements, but propagates only type "any".
     *
     * @param path negated path
     * @return type
     */
    @Override
    public PathInferredTypes visit(NegatedPath path)
    {
        // negated path can be anything, use anyclass
        PathInferredTypes type = visitElement(path.getChild());
        type.setObject(PathVisitorTypes.anyClass);
        type.setSubject(PathVisitorTypes.anyClass);
        type.setSubpath(path);
        return type;
    }

    /**
     * Processes repetitions of repeated paths. If it is possible to repeat the path more than once, it checks its
     * connectability. If path is possible to be repeated zero times, indicator "isOmissible" is set to true.
     *
     * @param path repeated path
     * @return type
     */
    @Override
    public PathInferredTypes visit(RepeatedPath path)
    {
        PathInferredTypes fromchild = visitElement(path.getChild());
        if(path.getKind() != RepeatedPath.Kind.ZeroOrOne)
        {
            boolean pathIsOk = isConnectable(fromchild.getObject(), fromchild.getSubject());

            if(!pathIsOk)
            {
                semErrors.add(
                        new SemanticError(ErrorType.incorrectPathRepetition, path.getRange(), path.toString(prologue), // which path is incorrect
                                fromchild.getObject().toString(prologue), // ends with
                                fromchild.getSubject().toString(prologue) // starts with
                        ));
                fromchild.setObject(PathVisitorTypes.anyClass);
                fromchild.setSubject(PathVisitorTypes.anyClass);
            }
            fromchild.setSubpath(path);
        }

        // if zero repetition is possible, add special "copy" flag
        // that marks it is possible to use domain from next element and range
        // from previous element
        if(path.getKind() != RepeatedPath.Kind.OneOrMore)
        {
            fromchild.setOmmissible(true);
        }
        return fromchild;
    }

    /**
     * Uses disjunction of types from son nodes.
     *
     * @param path alternative path
     * @return type
     */
    @Override
    public PathInferredTypes visit(AlternativePath path)
    {
        PathInferredTypes type = new PathInferredTypes(classInfo);
        path.getChildren().stream().map(p -> visitElement(p)).forEach(a -> {
            type.addToObjectUnion(a.getObject());
            type.addToSubjectUnion(a.getSubject());
            // if(a.getErrors() != null)
            // for(SemanticError er : a.getErrors())
            // type.addError(er);
        });

        type.setSubpath(path);
        return type;
    }

    /**
     * Checks validity of sequence of paths. It also takes care of omissible subpaths -- it copies domains and ranges
     * for omissible start or end of sequence and tries to check if it is possible to omit subpath by ontology of
     * neighboring paths.
     *
     * @param path sequence path
     * @return type
     */
    @Override
    public PathInferredTypes visit(SequencePath path)
    {
        PathInferredTypes result = new PathInferredTypes(classInfo);
        List<PathInferredTypes> results = path.getChildren().stream().map(a -> visitElement(a))
                .collect(Collectors.toList());

        if(results.size() > 1)
        {
            boolean isOmissibleSequence = true;

            for(int i = 0; i < results.size(); i++)
            {
                // if element is in omissible sequence from the start,
                // copy its domain to the result domain
                // this is always true for first element
                if(isOmissibleSequence)
                    result.addToSubjectUnion(results.get(i).getSubject());

                // if has next, try connectability
                if(i + 1 < results.size())
                {
                    // if cannot be connected, add error
                    if(!isConnectable(results.get(i).getObject(), results.get(i + 1).getSubject()))
                    {
                        String firstPart = path.getChildren().subList(0, i + 1).stream()
                                .map(a -> ((BaseElement) a).toString(prologue)).collect(Collectors.joining("/"));

                        String nextPart = path.getChildren().subList(i + 1, results.size()).stream()
                                .map(a -> ((BaseElement) a).toString(prologue)).collect(Collectors.joining("/"));

                        // Position start =
                        // path.getChildren().get(i).getRange().getEnd();
                        // Position end = path.getChildren().get(i +
                        // 1).getRange().getStart();
                        Position start = path.getChildren().get(i).getRange().getStart();
                        Position end = path.getChildren().get(i + 1).getRange().getEnd();
                        Range r = new Range(start, end);

                        semErrors.add(new SemanticError(ErrorType.incorrectPathConnection, r, path.toString(prologue),
                                firstPart, results.get(i).getObject().toString(prologue), nextPart,
                                results.get(i + 1).getSubject().toString(prologue)));

                    }
                }

                // if element can be omitted, check connectability with previous
                // node and merge its range classes for future comparison
                if(results.get(i).isOmissible())
                {
                    // check connection
                    if(i + 1 < results.size() && i - 1 >= 0
                            && !isConnectable(results.get(i - 1).getObject(), results.get(i + 1).getSubject()))
                    {
                        semErrors.add(new SemanticError(ErrorType.incorrectPathOmit,
                                results.get(i).getSubpath().getRange(), path.toString(prologue),
                                ((BaseElement) results.get(i).getSubpath()).toString(prologue)));
                    }
                    // append range from previous (as union)
                    if(i - 1 >= 0)
                        results.get(i).addToObjectUnion(results.get(i - 1).getObject());

                } // if element cannot be omitted, omissible sequence is broken
                else if(isOmissibleSequence)
                    isOmissibleSequence = false;
            }

            // copy last element's object to result
            result.setObject(results.get(results.size() - 1).getObject());
            result.setSubpath(path);
            if(isOmissibleSequence)
                result.setOmmissible(true);

            // add merged errors
            // for(PathInferredTypes t : results)
            // if(t.getErrors() != null)
            // for(SemanticError e : t.getErrors())
            // result.addError(e);

            return result;
        }
        else if(results.size() == 1)
            return results.get(0);
        else
            return null;
    }


    public HashSet<SemanticError> getSemErrors()
    {
        return semErrors;
    }
}

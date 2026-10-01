package cz.iocb.sparql.engine.translator;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.estimateAsUnion;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.PrimitiveResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.IriCache;
import cz.iocb.sparql.engine.request.Request;



/**
 * Collects the solutions received from a federated SERVICE endpoint and turns them into intermediate code. It
 * classifies the received terms, trying for each variable the IRI classes admitted by the restrictions and moving the
 * classes that matched to the front.
 */
public abstract class ResultHandler implements AutoCloseable
{
    /**
     * Current request.
     */
    protected final Request request;

    /**
     * Variables and classes the parent needs.
     */
    protected final Restrictions restrictions;

    /**
     * Classes and columns of the IRIs received so far.
     */
    private final IriCache iriCache = new IriCache(10000);

    /**
     * Per variable, the IRI classes to try, most recently matched first.
     */
    private final Map<Variable, List<UserIriClass>> typeIriClassesMap = new HashMap<>();


    /**
     * Creates the handler; IRI classes to try are taken from the restrictions of each variable.
     *
     * @param request the current request
     * @param restrictions what the parent needs of the variables
     */
    protected ResultHandler(Request request, Restrictions restrictions)
    {
        ClassRelations relations = request.getConfiguration();

        this.request = request;
        this.restrictions = restrictions;

        List<UserIriClass> iriClasses = request.getConfiguration().getIriClasses();

        for(Variable var : restrictions.getNames())
        {
            Set<ResourceClass> restriction = restrictions.get(var);

            // an IRI of no user class is recognised only by trying all of them
            if(restriction == null || !ResourceClass.areDisjunct(relations, unsupportedIri, restriction))
                typeIriClassesMap.put(var, new LinkedList<>(iriClasses));
            else
                typeIriClassesMap.put(var, new LinkedList<>(iriClasses.stream()
                        .filter(c -> !ResourceClass.areDisjunct(relations, c, restriction)).toList()));
        }
    }


    /**
     * Class of the received term bound to the variable.
     *
     * @param request the current request
     * @param value the received term
     * @param variable the variable
     * @return class of the received term bound to the variable
     * @throws UnsupportedOperationException for a triple term, which the translator does not support yet
     */
    protected final ResourceClass getResourceClass(Request request, RdfTerm value, Variable variable)
    {
        return switch(value)
        {
            case Literal lit -> getLiteralClass(request, lit);
            case Iri iri -> getIriClass(request, iri, variable);
            case BlankNode bn -> getBlankNodeClass(request, bn);
            //TODO: SPARQL 1.2
            case TripleTerm _ -> throw new UnsupportedOperationException("triple terms are not supported yet");
            default -> null;
        };
    }


    /**
     * Class of a received IRI among the admitted user classes (see {@link Request#detectIriClass}); the classes found
     * move to the front of the admitted classes for the next lookup, as the IRIs of one variable tend to be alike.
     *
     * @param request the current request
     * @param iri the IRI
     * @param variable the variable
     * @return class of a received IRI among the admitted user classes
     */
    private ResourceClass getIriClass(Request request, Iri iri, Variable variable)
    {
        ResourceClass iriClass = iriCache.getIriClass(iri);

        if(iriClass != null)
            return iriClass;

        List<UserIriClass> iriClasses = typeIriClassesMap.get(variable);

        iriClass = request.detectIriClass(iri, iriClasses);

        for(PrimitiveResourceClass found : estimateAsUnion(iriClass))
        {
            if(found instanceof UserIriClass user && !user.equals(iriClasses.getFirst()))
            {
                iriClasses.remove(user);
                iriClasses.addFirst(user);
            }
        }

        iriCache.storeClass(iri, iriClass);

        return iriClass;
    }


    /**
     * Class of a received literal.
     *
     * @param request the current request
     * @param literal the literal
     * @return the resulting class
     */
    private ResourceClass getLiteralClass(Request request, Literal literal)
    {
        return request.getLiteralClass(literal);
    }


    /**
     * Class of a received blank node.
     *
     * @param request the current request
     * @param bnode the blank node
     * @return the resulting class
     */
    private ResourceClass getBlankNodeClass(Request request, BlankNode bnode)
    {
        return request.getBlankNodeClass(bnode);
    }


    /**
     * Constant columns representing the term in the class, cached for IRIs.
     *
     * @param request the current request
     * @param resClass the resource class
     * @param term the RDF term
     * @return constant columns representing the term in the class, cached for IRIs
     */
    public List<Column> getColumns(Request request, ResourceClass resClass, RdfTerm term)
    {
        if(term instanceof Iri iri)
            return getColumns(request, resClass, iri);

        return resClass.toColumns(request, term);
    }


    /**
     * Constant columns representing the IRI in the class, one of the classes the IRI belongs to; cached.
     *
     * @param request the current request
     * @param resClass the resource class
     * @param iri the IRI
     * @return constant columns representing the IRI in the class, cached
     */
    public List<Column> getColumns(Request request, ResourceClass resClass, Iri iri)
    {
        List<Column> columns = iriCache.getIriColumns(iri, resClass);

        if(columns != null)
            return columns;

        columns = resClass.toColumns(request, iri);
        iriCache.storeColumns(iri, resClass, columns);

        return columns;
    }


    /**
     * Adds one received solution.
     *
     * @param row the received solution
     * @throws SQLException on database errors
     */
    public abstract void add(Map<Variable, RdfTerm> row) throws SQLException;


    /**
     * Intermediate code producing the collected solutions.
     *
     * @return intermediate code producing the collected solutions
     * @throws SQLException on database errors
     */
    public abstract SqlIntercode get() throws SQLException;


    /**
     * Number of collected solutions.
     *
     * @return number of collected solutions
     */
    public abstract int size();


    @Override
    public abstract void close();
}

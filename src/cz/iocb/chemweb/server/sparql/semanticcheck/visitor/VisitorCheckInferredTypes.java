package cz.iocb.chemweb.server.sparql.semanticcheck.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Projection;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Filter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GraphPattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Minus;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.MultiProcedureCall;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Optional;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCall;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCallBase;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCallBase.Parameter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Union;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;
import cz.iocb.chemweb.server.sparql.semanticcheck.error.ErrorType;
import cz.iocb.chemweb.server.sparql.semanticcheck.error.SemanticError;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.PropertiesInfo;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.inference.InferenceState;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.inference.Occurrence;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.inference.PathInferredTypes;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.Constraint;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.OccurenceEvaluation;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.SubClassEvaluation;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeElement;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeOperations;
import cz.iocb.chemweb.server.sparql.translator.config.Config;
import cz.iocb.chemweb.server.sparql.translator.config.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.translator.config.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.translator.config.ResultDefinition;



/**
 * Main visitor that checks for semantic errors.
 */
public class VisitorCheckInferredTypes extends ElementVisitor<InferenceState>
{
    private static Constraint langStringConstraint = new Constraint(
            "<http://www.w3.org/1999/02/22-rdf-syntax-ns#langString>");
    private static Constraint stringConstraint = new Constraint("<http://www.w3.org/2001/XMLSchema#string>");

    private final PropertiesInfo propInfo;
    private final ClassesInfo classInfo;
    private final OccurenceEvaluation evalVisitor;
    private final Config procedureConfig;
    private Prologue prologue;
    private HashSet<SemanticError> semErrors;

    /**
     * Create checker without external procedure calls
     *
     * @param propinfo ontology info about properties
     * @param classInfo ontology info about classes
     */
    public VisitorCheckInferredTypes(PropertiesInfo propinfo, ClassesInfo classInfo)
    {
        this.propInfo = propinfo;
        this.classInfo = classInfo;
        this.evalVisitor = new OccurenceEvaluation();
        this.procedureConfig = null;
    }

    /**
     * Create checker with external procedure calls
     *
     * @param propinfo ontology info about properties
     * @param classInfo ontology info about classes
     * @param config procedure call config
     */
    public VisitorCheckInferredTypes(PropertiesInfo propinfo, ClassesInfo classInfo, Config config)
    {
        this.propInfo = propinfo;
        this.classInfo = classInfo;
        this.evalVisitor = new OccurenceEvaluation();
        this.procedureConfig = config;
    }

    /**
     * Check SPARQL query parse tree
     *
     * @param sparqlQuery query tree
     * @return state
     */
    public InferenceState check(SelectQuery sparqlQuery)
    {
        if(sparqlQuery != null && this.classInfo != null && this.propInfo != null)
        {
            this.prologue = sparqlQuery.getPrologue();
            semErrors = new HashSet<SemanticError>();
            InferenceState resultState = visitElement(sparqlQuery);
            return resultState;
        }
        else
            return null;
    }

    private void checkParameter(ProcedureCallBase procedure, ProcedureDefinition procDef, Parameter par,
            InferenceState state)
    {
        if(par != null)
        {
            List<ParameterDefinition> matching = procDef.getParameters().stream()
                    .filter(x -> x.getParamName().equals(par.getName().toString())).collect(Collectors.toList());

            if(matching == null || matching.isEmpty())
                return;

            if(matching.get(0).getTypeIRI() == null)
                return;


            // Constraint type = new Constraint(matching.get(0).getTypeIRI());
            TypeElement type = TypeOperations.getBaseClass(matching.get(0).getTypeIRI(), classInfo);

            if(isVariableOrBlankNode(par.getValue()))
            {
                state.addVariableOccurence(par.getValue().toString(), new Occurrence(type, par.getValue()));

                state.intersectVariableTypes(par.getValue().toString(), type);
            }
            else if(par.getValue() instanceof Literal)
            {
                checkLiteral((Literal) par.getValue(), type);
            }
        }
    }

    /**
     * Check procedure call.
     *
     * @param procedure procedure call
     * @return state
     */
    @Override
    public InferenceState visit(MultiProcedureCall procedure)
    {
        InferenceState state = null;
        if(this.procedureConfig != null && this.classInfo != null && this.propInfo != null)
        {
            state = new InferenceState(classInfo);
            ProcedureDefinition procDef = this.procedureConfig.getProcedureByName(procedure.getProcedure().toString());

            if(procDef != null)
            {
                List<Parameter> results = procedure.getResults();
                List<ResultDefinition> resultsDef = procDef.getResults();
                for(Parameter p : results)
                {
                    if(p != null && isVariableOrBlankNode(p.getValue()))
                    {
                        List<ResultDefinition> collect = resultsDef.stream()
                                .filter(x -> x.getParamName().equals(p.getName().toString()))
                                .collect(Collectors.toList());
                        if(collect != null && !collect.isEmpty())
                        {
                            ResultDefinition resDef = collect.get(0);

                            String retTypeIRI = resDef.getTypeIRI();
                            if(retTypeIRI != null)
                            {
                                // Constraint type = new Constraint(retTypeIRI);
                                TypeElement type = TypeOperations.getBaseClass(retTypeIRI, classInfo);

                                state.addVariableOccurence(p.getValue().toString(), new Occurrence(type, p.getValue()));

                                state.intersectVariableTypes(p.getValue().toString(), type);
                            }
                        }
                    }
                }

                List<ProcedureCall.Parameter> parameters = procedure.getParameters();
                if(parameters != null && !parameters.isEmpty())
                {
                    for(Parameter par : parameters)
                        checkParameter(procedure, procDef, par, state);
                }
            }
        }
        return state;
    }

    /**
     * Check procedure call.
     *
     * @param procedure procedure call
     * @return state
     */
    @Override
    public InferenceState visit(ProcedureCall procedure)
    {
        InferenceState state = null;
        if(this.procedureConfig != null && this.classInfo != null && this.propInfo != null)
        {
            state = new InferenceState(classInfo);
            ProcedureDefinition procDef = this.procedureConfig.getProcedureByName(procedure.getProcedure().toString());
            if(procDef != null)
            {
                if(isVariableOrBlankNode(procedure.getResult()) && procDef.getRetTypeIRI() != null)
                {
                    // Constraint type = new
                    // Constraint(procDef.getRetTypeIRI());
                    TypeElement type = TypeOperations.getBaseClass(procDef.getRetTypeIRI(), classInfo);

                    state.addVariableOccurence(procedure.getResult().toString(),
                            new Occurrence(type, procedure.getResult()));

                    state.intersectVariableTypes(procedure.getResult().toString(), type);
                }

                List<ProcedureCall.Parameter> parameters = procedure.getParameters();
                if(parameters != null && !parameters.isEmpty())
                {
                    for(Parameter par : parameters)
                        checkParameter(procedure, procDef, par, state);
                }

            }
        }
        return state;
    }

    /**
     * Visit triple -- infer types for variables, check literals and property paths.
     *
     * @param triple triple
     * @return state
     */
    @Override
    public InferenceState visit(Triple triple)
    {
        // InferenceState state = null;
        List<InferenceState> states = null;

        if(this.classInfo != null)
        {
            // state = new InferenceState(classInfo);
            states = new ArrayList<InferenceState>();

            // if rdf:type, and it is ?X a <class>, then use <class> as a type
            // of ?X.
            if(triple.getPredicate() instanceof IRI
                    && triple.getPredicate().toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")
                    && isVariableOrBlankNode(triple.getSubject()) && triple.getObject() instanceof IRI
                    && this.classInfo.isClass(((IRI) triple.getObject()).toString()))
            {
                // Constraint constraint = new Constraint(((IRI)
                // triple.getObject()).toString());
                TypeElement constraint = TypeOperations.getBaseClass(((IRI) triple.getObject()).toString(), classInfo);

                InferenceState state = new InferenceState(classInfo);

                state.addVariableOccurence(triple.getSubject().toString(),
                        new Occurrence(constraint, triple.getSubject()));

                state.intersectVariableTypes(triple.getSubject().toString(), constraint);

                states.add(state);
            }
            // infer type otherwise
            else if(this.classInfo != null && this.propInfo != null)
            {
                // find subject and object types
                PathVisitorTypes pv = new PathVisitorTypes(this.propInfo, this.classInfo, prologue);
                PathInferredTypes type = pv.getType(triple);

                // add errors from path
                semErrors.addAll(pv.getSemErrors());


                // add occurences and types to the state
                if(type.getSubject() != null)
                {
                    if(isVariableOrBlankNode(triple.getSubject()))
                    {
                        InferenceState state = new InferenceState(classInfo);

                        state.addVariableOccurence(triple.getSubject().toString(),
                                new Occurrence(type.getSubject(), triple.getSubject()));

                        state.intersectVariableTypes(triple.getSubject().toString(), type.getSubject());

                        states.add(state);
                    }
                    else if(triple.getSubject() instanceof Literal)
                    {
                        checkLiteral((Literal) triple.getSubject(), type.getSubject());
                    }
                }
                if(type.getObject() != null)
                {
                    if(isVariableOrBlankNode(triple.getObject()))
                    {
                        InferenceState state = new InferenceState(classInfo);

                        state.addVariableOccurence(triple.getObject().toString(),
                                new Occurrence(type.getObject(), triple.getObject()));

                        state.intersectVariableTypes(triple.getObject().toString(), type.getObject());

                        states.add(state);
                    }
                    else if(triple.getObject() instanceof Literal)
                    {
                        checkLiteral((Literal) triple.getObject(), type.getObject());
                    }
                }
                if(isVariable(triple.getPredicate()))
                {
                    InferenceState state = new InferenceState(classInfo);

                    state.addVariableOccurence(triple.getPredicate().toString(),
                            new Occurrence(ClassesInfo.propertyType, triple.getPredicate()));
                    state.intersectVariableTypes(triple.getPredicate().toString(), ClassesInfo.propertyType);

                    states.add(state);
                }
            }
        }

        return aggregateResult(states);
    }

    /**
     * Is node variable or blank node
     *
     * @param n node
     * @return true if is instance of variable or blank node
     */
    private boolean isVariableOrBlankNode(Node n)
    {
        return n instanceof Variable || n instanceof BlankNode;
    }

    /**
     * Is verb a variable
     *
     * @param v verb
     * @return true if is instance of variable
     */
    private boolean isVariable(Verb v)
    {
        return v instanceof Variable;
    }


    /**
     * Check UNION - merge states with OR in types.
     *
     * @param union union
     * @return state
     */
    @Override
    public InferenceState visit(Union union)
    {
        List<InferenceState> childrenStates = new ArrayList<>();
        InferenceState state = null;

        List<GraphPattern> patterns = union.getPatterns();
        for(GraphPattern pattern : patterns)
        {
            InferenceState childState = visitElement(pattern);
            if(childState != null)
                childrenStates.add(childState);
        }

        if(!childrenStates.isEmpty())
            for(InferenceState a : childrenStates)
                if(a != null)
                    if(state == null)
                        state = a;
                    else
                        state.unionWithState(a);

        return state;
    }

    /**
     * Check OPTIONAL - does not propagate types, only occurrences and errors
     *
     * @param optional optional
     * @return state
     */
    @Override
    public InferenceState visit(Optional optional)
    {
        GraphPattern pattern = optional.getPattern();
        InferenceState state = visitElement(pattern);

        if(state != null)
        {
            state.clearVariableTypes();
            return state;
        }
        else
            return null;
    }

    /**
     * Check MINUS - does not propagate types, only occurrences and errors
     *
     * @param minus minus
     * @return state
     */
    @Override
    public InferenceState visit(Minus minus)
    {
        GraphPattern pattern = minus.getPattern();
        InferenceState state = visitElement(pattern);

        if(state != null)
        {
            state.clearVariableTypes();
            return state;
        }
        else
            return null;
    }


    /**
     * Check FILTER NOT EXISTS - does not propagate types, only occurrences and errors
     *
     * @param filter filter
     * @return state
     */
    @Override
    public InferenceState visit(Filter filter)
    {
        InferenceState state = visitElement(filter.getConstraint());

        if(state != null)
        {
            // state.clearVariableOccurences(); //NOTE: I do not know if it is a
            // good idea.
            state.clearVariableTypes();
        }

        return state;
    }

    /**
     * Check SELECT - propagate types and occurrences only if the variable is in projections.
     *
     * @param select select
     * @return state
     */
    @Override
    public InferenceState visit(Select select)
    {
        List<Projection> projections = select.getProjections();
        InferenceState state = visitElement(select.getPattern());

        if(state != null && !projections.isEmpty())
        {
            InferenceState newstate = new InferenceState(classInfo);
            HashMap<String, HashSet<Occurrence>> occurences = state.getOccurences();
            for(Projection p : projections)
            {
                String pp = p.toString();
                if(occurences.containsKey(pp))
                    for(Occurrence o : occurences.get(pp))
                        newstate.addVariableOccurence(pp, o);
                if(state.getVariableType(pp) != null)
                    newstate.intersectVariableTypes(pp, state.getVariableType(pp));
            }
            // for(SemanticError er : state.getErrors())
            // newstate.addError(er);
            return newstate;
        }
        else
            return state;
    }



    private void checkVariables(HashMap<String, TypeElement> types, HashMap<String, HashSet<Occurrence>> occurrences)
    {
        for(String variable : occurrences.keySet())
        {
            for(Occurrence oc : occurrences.get(variable))
            {
                TypeElement type = types.get(variable);

                if(type != null && !isConsistentTypeByOccurence(type, oc))
                {
                    {
                        if(oc.element instanceof Variable)
                        {
                            semErrors.add(new SemanticError(ErrorType.inferenceErrorVar, oc.range, variable,
                                    oc.type.toString(prologue), type.toString(prologue)));
                        }
                        else
                        {
                            String name = ((BlankNode) oc.element).getName();

                            if(name.startsWith("_"))
                                semErrors.add(new SemanticError(ErrorType.inferenceErrorNamedBlankNode, oc.range, name,
                                        oc.type.toString(prologue), type.toString(prologue)));
                            else
                                semErrors.add(new SemanticError(ErrorType.inferenceErrorBlankNode, oc.range,
                                        oc.type.toString(prologue), type.toString(prologue)));
                        }
                    }
                }
            }

        }
    }


    /**
     * Check if type is consistent with occurrence
     *
     * @param type global type
     * @param oc occurrence local type
     * @return true if consistent
     */
    private boolean isConsistentTypeByOccurence(TypeElement type, Occurrence oc)
    {
        if(type != null && oc != null && this.evalVisitor != null)
            return this.evalVisitor.visitTripletOccurence(oc.type, type, classInfo);
        else
            return false;
    }

    /**
     * Default merge of states with AND operator.
     *
     * @param results results
     * @return state merged with AND
     */
    @Override
    protected InferenceState aggregateResult(List<InferenceState> results)
    {
        /*
         * for(InferenceState typeState : results) if(typeState != null)
         * for(InferenceState varState : results) if(varState != null &&
         * varState != typeState) checkVariables(typeState,
         * varState.getOccurences());
         */

        for(InferenceState varState : results)
        {
            if(varState == null)
                continue;

            InferenceState state = new InferenceState(varState.ontology);

            for(InferenceState a : results)
                if(a != null && a != varState)
                    state.intersectWithState(a);

            checkVariables(state.getVariableTypes(), varState.getOccurences());
        }



        InferenceState state = null;

        for(InferenceState a : results)
        {
            if(a == null)
                continue;

            if(state == null)
                state = a;
            else
                state.intersectWithState(a);
        }

        return state;
    }


    public HashSet<SemanticError> getSemErrors()
    {
        return semErrors;
    }


    private void checkLiteral(Literal literal, TypeElement type)
    {
        if(ClassesInfo.anyClass.equals(type.value))
            return;

        TypeElement littype;

        if(literal.getTypeIri() != null)
            littype = new Constraint(literal.getTypeIri().toString());
        else if(literal.getLanguageTag() != null)
            littype = langStringConstraint;
        else
            littype = stringConstraint;

        //TODO: check convertibility of the literal value to the denoted type (littype)

        SubClassEvaluation sce = new SubClassEvaluation();


        if(sce.isSubClass(type, langStringConstraint, classInfo))
        {
            if(literal.getLanguageTag() == null)
                semErrors.add(new SemanticError(ErrorType.langLiteralError, literal.getRange(),
                        literal.toString(prologue), littype.toString(prologue)));
        }
        else
        {
            //TODO: check convertibility of the literal value to the requested type (type)

            OccurenceEvaluation oce = new OccurenceEvaluation();

            if(!oce.visitTripletOccurence(littype, type, classInfo))
                semErrors.add(new SemanticError(ErrorType.literalError, literal.getRange(), literal.toString(prologue),
                        littype.toString(prologue), type.toString(prologue)));
        }
    }
}

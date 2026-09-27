package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.Variable;



/**
 * Triple terms whose subject, predicate and object belong to the given classes, stored as the columns of the subject
 * followed by the columns of the predicate and of the object. The predicate class is a class of IRIs, the subject and
 * the object classes are arbitrary, so a nested triple term has a class of its own. The class is a subclass of another
 * triple term class when its components are subclasses of the other's components, hence of
 * {@link BuiltinClasses#tripleTerm}, the class of all triple terms, and of the box, which holds a triple term built by
 * {@code sparql.rdfbox_create_from_tripleterm} from the boxed subject, the predicate IRI and the boxed object; two
 * triple term classes are disjoint when some pair of their components is.
 */
public final class TripleTermClass extends PrimitiveResourceClass
{
    /**
     * Class of the subjects.
     */
    private final ResourceClass subject;

    /**
     * Class of the predicates.
     */
    private final ResourceClass predicate;

    /**
     * Class of the objects.
     */
    private final ResourceClass object;


    /**
     * Creates the class of the triple terms with the given component classes.
     *
     * @param subject class of the subjects
     * @param predicate class of the predicates, a class of IRIs
     * @param object class of the objects
     */
    public TripleTermClass(ResourceClass subject, ResourceClass predicate, ResourceClass object)
    {
        super(nameOf(subject, predicate, object), sqlTypesOf(subject, predicate, object), Set.of(box));

        assert predicate.isSubclassOf(iri);

        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }


    /**
     * Name of the class of the triple terms with the given component classes.
     *
     * @param subject class of the subjects
     * @param predicate class of the predicates
     * @param object class of the objects
     * @return name of the class of the triple terms with the given component classes
     */
    private static String nameOf(ResourceClass subject, ResourceClass predicate, ResourceClass object)
    {
        return "tripleterm(" + subject.getResourceName() + "," + predicate.getResourceName() + ","
                + object.getResourceName() + ")";
    }


    /**
     * SQL types of the columns of the triple terms with the given component classes: the types of the subject followed
     * by the types of the predicate and of the object.
     *
     * @param subject class of the subjects
     * @param predicate class of the predicates
     * @param object class of the objects
     * @return SQL types of the columns of the triple terms with the given component classes
     */
    private static List<SqlType> sqlTypesOf(ResourceClass subject, ResourceClass predicate, ResourceClass object)
    {
        List<SqlType> types = new ArrayList<>();

        types.addAll(subject.getSqlTypes());
        types.addAll(predicate.getSqlTypes());
        types.addAll(object.getSqlTypes());

        return List.copyOf(types);
    }


    /**
     * Class of the subjects.
     *
     * @return class of the subjects
     */
    public ResourceClass getSubject()
    {
        return subject;
    }


    /**
     * Class of the predicates.
     *
     * @return class of the predicates
     */
    public ResourceClass getPredicate()
    {
        return predicate;
    }


    /**
     * Class of the objects.
     *
     * @return class of the objects
     */
    public ResourceClass getObject()
    {
        return object;
    }


    /**
     * The columns of the subject among the given columns of this class.
     *
     * @param columns the columns representing values of this class
     * @return the columns of the subject
     */
    public List<Column> getSubjectColumns(List<Column> columns)
    {
        return List.copyOf(columns.subList(0, subject.getColumnCount()));
    }


    /**
     * The columns of the predicate among the given columns of this class.
     *
     * @param columns the columns representing values of this class
     * @return the columns of the predicate
     */
    public List<Column> getPredicateColumns(List<Column> columns)
    {
        int begin = subject.getColumnCount();

        return List.copyOf(columns.subList(begin, begin + predicate.getColumnCount()));
    }


    /**
     * The columns of the object among the given columns of this class.
     *
     * @param columns the columns representing values of this class
     * @return the columns of the object
     */
    public List<Column> getObjectColumns(List<Column> columns)
    {
        int begin = subject.getColumnCount() + predicate.getColumnCount();

        return List.copyOf(columns.subList(begin, begin + object.getColumnCount()));
    }


    /**
     * True if the class is the given one, a triple term class whose components are superclasses of the components of
     * this class, or the box.
     */
    @Override
    protected boolean isSubclassOf(PrimitiveResourceClass resClass)
    {
        if(resClass instanceof TripleTermClass other)
            return subject.isSubclassOf(other.subject) && predicate.isSubclassOf(other.predicate)
                    && object.isSubclassOf(other.object);

        return super.isSubclassOf(resClass);
    }


    /**
     * True if no triple term belongs to both classes, i.e. some pair of their components is disjoint.
     *
     * @param other the other triple term class
     * @return true if no triple term belongs to both classes, false otherwise
     */
    public boolean isDisjunctWith(TripleTermClass other)
    {
        return areDisjunct(subject, other.subject) || areDisjunct(predicate, other.predicate)
                || areDisjunct(object, other.object);
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(box);
    }


    @Override
    public boolean match(Statement statement, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case TripleTerm triple -> subject.match(statement, triple.getSubject())
                    && predicate.match(statement, triple.getPredicate()) && object.match(statement, triple.getObject());
            default -> false;
        };
    }


    @Override
    public List<Column> toColumns(Statement statement, RdfTerm term)
    {
        if(!(term instanceof TripleTerm triple))
            throw new IllegalArgumentException();

        List<Column> columns = new ArrayList<>(getColumnCount());

        columns.addAll(subject.toColumns(statement, triple.getSubject()));
        columns.addAll(predicate.toColumns(statement, triple.getPredicate()));
        columns.addAll(object.toColumns(statement, triple.getObject()));

        return columns;
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        List<Column> subjectColumns = getSubjectColumns(columns);
        List<Column> predicateColumns = getPredicateColumns(columns);
        List<Column> objectColumns = getObjectColumns(columns);

        if(targetClass instanceof TripleTermClass other)
        {
            List<Column> result = new ArrayList<>(other.getColumnCount());

            result.addAll(subject.toClass(other.subject, subjectColumns, canBeNull));
            result.addAll(predicate.toClass(other.predicate, predicateColumns, canBeNull));
            result.addAll(object.toClass(other.object, objectColumns, canBeNull));

            return result;
        }

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_tripleterm(%s, %s, %s)",
                    subject.toClass(box, subjectColumns, canBeNull).get(0),
                    predicate.toClass(iri, predicateColumns, canBeNull).get(0),
                    object.toClass(box, objectColumns, canBeNull).get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        if(sourceClass instanceof TripleTermClass other)
        {
            List<Column> result = new ArrayList<>(getColumnCount());

            result.addAll(fromComponentClass(subject, other.subject, other.getSubjectColumns(columns), checkOptional));
            result.addAll(
                    fromComponentClass(predicate, other.predicate, other.getPredicateColumns(columns), checkOptional));
            result.addAll(fromComponentClass(object, other.object, other.getObjectColumns(columns), checkOptional));

            return result;
        }

        if(sourceClass.equals(box))
        {
            Column column = columns.get(0);
            List<Column> result = new ArrayList<>(getColumnCount());

            result.addAll(fromComponentClass(subject, box,
                    List.of(expression("sparql.rdfbox_get_tripleterm_subject(%s)", column)), checkOptional));
            result.addAll(fromComponentClass(predicate, iri,
                    List.of(expression("sparql.rdfbox_get_tripleterm_predicate(%s)", column)), checkOptional));
            result.addAll(fromComponentClass(object, box,
                    List.of(expression("sparql.rdfbox_get_tripleterm_object(%s)", column)), checkOptional));

            return result;
        }

        throw new IllegalArgumentException();
    }


    /**
     * Columns of a component class converted from the given columns of a class related to it: the component class
     * itself, one of its superclasses, or one of its subclasses (the predicate of a box is obtained in the IRI class,
     * which the predicate class may generalise).
     *
     * @param component the component class
     * @param sourceClass class of the given columns
     * @param columns the columns representing values of the source class
     * @param checkOptional indicates whether the representability check may be skipped
     * @return the columns representing values of the component class
     */
    private static List<Column> fromComponentClass(ResourceClass component, ResourceClass sourceClass,
            List<Column> columns, boolean checkOptional)
    {
        if(component.equals(sourceClass))
            return columns;

        if(component.isSubclassOf(sourceClass))
            return component.fromGeneralClass(sourceClass, columns, checkOptional);

        if(sourceClass.isSubclassOf(component))
            return sourceClass.toGeneralClass(component, columns, true);

        throw new IllegalArgumentException();
    }


    @Override
    public boolean isOptionalColumn(int index)
    {
        int subjectCount = subject.getColumnCount();
        int predicateCount = predicate.getColumnCount();

        if(index < subjectCount)
            return subject.isOptionalColumn(index);

        if(index < subjectCount + predicateCount)
            return predicate.isOptionalColumn(index - subjectCount);

        return object.isOptionalColumn(index - subjectCount - predicateCount);
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        TripleTermClass other = (TripleTermClass) object;

        return subject.equals(other.subject) && predicate.equals(other.predicate) && this.object.equals(other.object);
    }
}

package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



/**
 * The class an expression over the values of a class of several columns is evaluated in
 * ({@link ResourceClass#getExpressionClass(ResourceClass)}): the least single-column superclass of the class, chosen
 * among several minimal ones by the fewest columns and the name, as {@link PrimitiveResourceClass#getCommonSuperclass}
 * chooses, and an error for a class without a single-column superclass.
 */
public class ExpressionClassTest
{
    /**
     * A user IRI class of two columns with the given superclasses, which does nothing the test does not ask of it.
     */
    private static final class PairClass extends UserIriClass
    {
        /**
         * Creates the class.
         *
         * @param name the name
         * @param superClasses the superclasses
         */
        PairClass(String name, Set<PrimitiveResourceClass> superClasses)
        {
            super(name, List.of(INT4, INT4), superClasses);
        }


        @Override
        public boolean match(Request request, Iri iri)
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public List<Column> toColumns(Request request, Iri iri)
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public String getPrefix(List<Column> columns)
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public int getCheckCost()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * Creates a single-column user IRI class of the given name.
     *
     * @param name the name
     * @return the class
     */
    private static PrimitiveResourceClass userClass(String name)
    {
        return new IntegerUserIriClass(name, INT4, "http://example.org/" + name + "/");
    }


    @Test
    void minimalSuperclassesAreChosenByName()
    {
        // the names spread over the buckets of a hash set, so an order of iteration would choose another one
        List<String> names = List.of("q", "c", "x", "a", "m", "z", "b");

        for(int i = 1; i < names.size(); i++)
        {
            PrimitiveResourceClass first = userClass(names.get(i));
            PrimitiveResourceClass second = userClass(names.get(i - 1));
            PrimitiveResourceClass expected = first.getResourceName().compareTo(second.getResourceName()) < 0 ? first :
                    second;

            assertEquals(expected, ResourceClass.getExpressionClass(new PairClass("pair", Set.of(first, second))));
            assertEquals(expected, ResourceClass.getExpressionClass(new PairClass("pair", Set.of(second, first))));
        }
    }


    @Test
    void classWithoutSingleColumnSuperclassIsRefused()
    {
        assertThrows(IllegalArgumentException.class,
                () -> ResourceClass.getExpressionClass(new PairClass("pair", Set.of())));
    }


    @Test
    void builtinClassesKeepTheirLeastSuperclass() throws IllegalAccessException
    {
        int multiColumn = 0;

        for(Field field : BuiltinClasses.class.getFields())
        {
            if(!Modifier.isStatic(field.getModifiers()) || !(field.get(null) instanceof PrimitiveResourceClass resClass)
                    || resClass.getColumnCount() == 1 || resClass instanceof InternalResourceClass)
                continue;

            ResourceClass expressionClass = ResourceClass.getExpressionClass(resClass);

            assertEquals(1, expressionClass.getColumnCount(), field.getName());

            for(PrimitiveResourceClass superClass : resClass.getSuperClasses())
                if(superClass.getColumnCount() == 1)
                    assertTrue(expressionClass.isSubclassOf(superClass), field.getName() + " " + superClass);

            multiColumn++;
        }

        assertTrue(multiColumn > 0);
    }
}

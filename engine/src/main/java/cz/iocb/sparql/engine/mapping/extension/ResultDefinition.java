package cz.iocb.sparql.engine.mapping.extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Definition of a procedure result: the IRI used as its predicate (null for the single result of a simple procedure)
 * and, for each resource class it may take, the fields of the SQL result row holding it (null when the function returns
 * the value itself).
 */
public class ResultDefinition
{
    /**
     * IRI naming the result; null for the single unnamed result.
     */
    private final String resultName;

    /**
     * Fields of the SQL result row holding the value, per resource class.
     */
    private final Map<ResourceClass, List<Column>> mappings;


    /**
     * Creates a result that may take several classes, each stored in its own fields.
     *
     * @param resultName IRI naming the result
     * @param mappings columns per resource class
     */
    public ResultDefinition(String resultName, Map<ResourceClass, List<Column>> mappings)
    {
        this.resultName = resultName;
        this.mappings = mappings;
    }


    /**
     * Creates a result of one class stored in the given fields.
     *
     * @param resultName IRI naming the result
     * @param resultClass class of the result
     * @param sqlTypeFields fields of the SQL result row holding the value
     */
    public ResultDefinition(String resultName, ResourceClass resultClass, List<Column> sqlTypeFields)
    {
        Map<ResourceClass, List<Column>> mappings = new HashMap<>();
        mappings.put(resultClass, sqlTypeFields);

        this.resultName = resultName;
        this.mappings = mappings;
    }


    /**
     * Creates a result of one class stored in a single field.
     *
     * @param resultName IRI naming the result
     * @param resultClass class of the result
     * @param sqlTypeField field of the SQL result row holding the value
     */
    public ResultDefinition(String resultName, ResourceClass resultClass, String sqlTypeField)
    {
        List<Column> sqlTypeFields = List.of(new TableColumn(sqlTypeField));
        Map<ResourceClass, List<Column>> mappings = new HashMap<>();
        mappings.put(resultClass, sqlTypeFields);

        this.resultName = resultName;
        this.mappings = mappings;
    }


    /**
     * Creates the unnamed result of a simple procedure, whose value is the function result itself.
     *
     * @param resultClass class of the result
     */
    public ResultDefinition(ResourceClass resultClass)
    {
        Map<ResourceClass, List<Column>> mappings = new HashMap<>();
        mappings.put(resultClass, null);

        this.resultName = null;
        this.mappings = mappings;
    }


    /**
     * IRI naming the result; null for the single unnamed result.
     *
     * @return IRI naming the result; null for the single unnamed result
     */
    public final String getResultName()
    {
        return this.resultName;
    }


    /**
     * Fields of the SQL result row holding the value, per resource class; null fields mean the function result itself.
     *
     * @return fields of the SQL result row holding the value, per resource class; null fields mean the function result
     *         itself
     */
    public final Map<ResourceClass, List<Column>> getMappings()
    {
        return mappings;
    }
}

package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Literal class keeping every valid lexical form of its datatype: the value columns are followed by a lexical column
 * holding the original form, or an empty string when the form is canonical.
 */
public abstract class BaseLiteralClass extends LiteralClass
{
    /**
     * Creates the class with its name, datatype, column types and superclasses.
     *
     * @param name the name
     * @param datatype the datatype
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected BaseLiteralClass(String name, Datatype datatype, List<SqlType> sqlTypes,
            Set<PrimitiveResourceClass> superClasses)
    {
        super(name, datatype, sqlTypes, superClasses);
    }


    @Override
    public boolean match(Statement statement, Literal literal)
    {
        if(!datatype.getTypeIri().equals(literal.getType()))
            return false;

        if(!datatype.isValidForm(literal.getValue()))
            return false;

        return true;
    }


    /**
     * The lexical column constant for the literal: its lexical form, or an empty string when it is canonical.
     *
     * @param literal the literal
     * @return the lexical column constant for the literal: its lexical form, or an empty string when it is canonical
     */
    protected Column getLexicalColumn(Literal literal)
    {
        if(datatype.isCanonicalForm(literal.getValue()))
            return constant("", VARCHAR);

        return constant(literal.getValue(), VARCHAR);
    }
}

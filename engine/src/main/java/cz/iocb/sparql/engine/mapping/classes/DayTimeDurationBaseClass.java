package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationType;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.DayTimeDurationDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Any valid {@code xsd:dayTimeDuration} literal stored as a {@code int8} value plus its lexical form.
 */
public final class DayTimeDurationBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public DayTimeDurationBaseClass()
    {
        super("daytimeduration", xsdDayTimeDurationType, "int8");
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(
                constant(DayTimeDurationDatatype.parseValue(literal.getValue()).toBigIntegerExact(), sqlTypes.get(0)),
                getLexicalColumn(literal));
    }
}

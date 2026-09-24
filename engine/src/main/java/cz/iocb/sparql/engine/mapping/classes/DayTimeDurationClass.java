package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationType;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.DayTimeDurationDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Canonical {@code xsd:dayTimeDuration} literals stored in one {@code int8} column.
 */
public final class DayTimeDurationClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public DayTimeDurationClass()
    {
        super("daytimeduration", xsdDayTimeDurationType, INT8, genDayTimeDuration);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(
                constant(DayTimeDurationDatatype.parseValue(literal.getValue()).toBigIntegerExact(), sqlTypes.get(0)));
    }
}

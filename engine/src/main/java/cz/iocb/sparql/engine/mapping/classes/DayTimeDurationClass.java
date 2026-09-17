package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationType;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.DayTimeDurationDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



public final class DayTimeDurationClass extends SimpleLiteralClass
{
    public DayTimeDurationClass()
    {
        super("daytimeduration", xsdDayTimeDurationType, "int8", genDayTimeDuration);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(
                constant(DayTimeDurationDatatype.parseValue(literal.getValue()).toBigIntegerExact(), sqlTypes.get(0)));
    }
}

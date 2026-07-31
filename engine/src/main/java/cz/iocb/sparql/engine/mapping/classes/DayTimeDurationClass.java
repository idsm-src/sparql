package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationType;



public final class DayTimeDurationClass extends SimpleLiteralClass
{
    public DayTimeDurationClass()
    {
        super("daytimeduration", xsdDayTimeDurationType, "int8");
    }
}

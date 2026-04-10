package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDayTimeDurationIri;



public final class DayTimeDurationClass extends SimpleLiteralClass
{
    public DayTimeDurationClass()
    {
        super("daytimeduration", "int8", xsdDayTimeDurationIri);
    }
}

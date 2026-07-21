package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdDayTimeDurationIri;



public final class DayTimeDurationClass extends SimpleLiteralClass
{
    public DayTimeDurationClass()
    {
        super("daytimeduration", xsdDayTimeDurationIri, "int8");
    }
}

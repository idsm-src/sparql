package cz.iocb.sparql.engine.mapping.classes;



public sealed interface DateInZone permits DateInZoneClass, DateInZoneBaseClass
{
    public int getZone();
}

package cz.iocb.sparql.engine.mapping.classes;



public sealed interface DateTimeInZone permits DateTimeInZoneClass, DateTimeInZoneBaseClass
{
    public int getZone();
}

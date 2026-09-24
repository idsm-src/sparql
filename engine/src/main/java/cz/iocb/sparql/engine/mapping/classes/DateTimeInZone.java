package cz.iocb.sparql.engine.mapping.classes;



/**
 * Classes of xsd:dateTime values that all carry the same timezone offset.
 */
public sealed interface DateTimeInZone permits DateTimeInZoneClass, DateTimeInZoneBaseClass
{
    /**
     * Timezone offset in seconds east of UTC; {@link Integer#MIN_VALUE} for values without a timezone.
     *
     * @return timezone offset in seconds east of UTC; {@link Integer#MIN_VALUE} for values without a timezone
     */
    public int getZone();
}

package cz.iocb.chemweb.server.sparql.translator.visitor;

import java.util.LinkedHashSet;



/**
 * Auxiliary class used for holding a translation of subqueries. All the segments are being merged during the main
 * translation process.
 */
public class TranslatedSegment
{
    LinkedHashSet<String> subqueryVars = new LinkedHashSet<String>(); //FIXME:
    LinkedHashSet<String> possibleUnboundVars = new LinkedHashSet<String>(); //FIXME:
    String str = "";


    /**
     * Constructs the translated segment.
     *
     * @param str String of translated subquery.
     */
    public TranslatedSegment(String str)
    {
        this.str = str;
    }

    /**
     * Constructs the translated segment.
     *
     * @param str String of translated subquery.
     * @param subqueryVars Variables used (or projected) in this segment.
     */
    public TranslatedSegment(String str, LinkedHashSet<String> subqueryVars)
    {
        this(str);
        setSubqueryVars(subqueryVars);
        possibleUnboundVars = new LinkedHashSet<>();
    }

    public TranslatedSegment(String str, LinkedHashSet<String> subqueryVars, LinkedHashSet<String> possibleUnboundVars)
    {
        this(str);
        setSubqueryVars(subqueryVars);
        setPossibleUnboundVars(possibleUnboundVars);
    }

    /**
     * Sets variables used (or projected) in this segment.
     *
     * @param subqueryVars List of variables.
     * @return This segment itself.
     */
    public final TranslatedSegment setSubqueryVars(LinkedHashSet<String> subqueryVars)
    {
        this.subqueryVars = new LinkedHashSet<>();
        this.subqueryVars.addAll(subqueryVars);
        return this;
    }

    /**
     * Sets possible unbound variables in this segment.
     *
     * @param possibleUnboundVars List of possible unbound variables.
     * @return This segment itself.
     */
    public final TranslatedSegment setPossibleUnboundVars(LinkedHashSet<String> possibleUnboundVars)
    {
        if(possibleUnboundVars != null && !possibleUnboundVars.isEmpty())
        {
            this.possibleUnboundVars = new LinkedHashSet<>();
            this.possibleUnboundVars.addAll(possibleUnboundVars);
        }
        return this;
    }
}

package cz.iocb.chemweb.server.sparql.mapping.classes;



public enum ResultTag
{
    /* generic tag */
    RDFTERM("rdfterm"),

    /* generic iri tag */
    IRI("iri"),

    /* blank node tag */
    BLANKNODEINT("bnint"),
    BLANKNODESTR("bnstr"),

    /* literal tags */
    BOOLEAN("boolean"),
    SHORT("short"),
    INT("int"),
    LONG("long"),
    FLOAT("float"),
    DOUBLE("double"),
    INTEGER("integer"),
    DECIMAL("decimal"),
    DATETIME("datetime"),
    DATE("date"),
    DAYTIMEDURATION("daytimeduration"),
    STRING("string"),
    LANGSTRING("langstring"),

    /* supplementary literal tags */
    LANG("lang");


    private final String tag;


    ResultTag(String tag)
    {
        this.tag = tag;
    }


    public final String getTag()
    {
        return tag;
    }


    public static ResultTag get(String name)
    {
        for(ResultTag tag : values())
            if(tag.getTag().equals(name))
                return tag;

        return RDFTERM;
    }
}

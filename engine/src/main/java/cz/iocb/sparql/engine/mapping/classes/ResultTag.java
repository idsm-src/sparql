package cz.iocb.sparql.engine.mapping.classes;



public enum ResultTag
{
    /* generic tag */
    NULL("null", "varchar"),

    /* generic iri tag */
    IRI("iri", "varchar"),

    /* blank node tag */
    BLANKNODEINT("bnint", "int8"),
    BLANKNODESTR("bnstr", "varchar"),

    /* literal tags */
    BOOLEAN("boolean", "boolean"),
    SHORT("short", "int2"),
    INT("int", "int4"),
    LONG("long", "int8"),
    FLOAT("float", "float4"),
    DOUBLE("double", "float8"),
    INTEGER("integer", "decimal"),
    DECIMAL("decimal", "decimal"),
    DATETIME("datetime", "sparql.zoneddatetime"),
    DATE("date", "sparql.zoneddate"),
    DAYTIMEDURATION("daytimeduration", "int8"),
    STRING("string", "varchar"),
    LANGSTRING("langstring", "varchar"),
    LITERAL("literal", "varchar"),

    /* supplementary literal tags */
    LANG("lang", "varchar"),
    TYPE("type", "varchar");


    private final String tag;
    private final String type;


    ResultTag(String tag, String type)
    {
        this.tag = tag;
        this.type = type;
    }


    public final String getTag()
    {
        return tag;
    }


    public final String getSqlType()
    {
        return type;
    }


    public static ResultTag get(String name)
    {
        for(ResultTag tag : values())
            if(tag.getTag().equals(name))
                return tag;

        return NULL;
    }
}

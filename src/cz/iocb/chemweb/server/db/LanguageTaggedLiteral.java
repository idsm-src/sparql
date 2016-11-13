package cz.iocb.chemweb.server.db;



public class LanguageTaggedLiteral extends Literal
{
    private final String language;


    public LanguageTaggedLiteral(String value, String language)
    {
        super(value);
        this.language = language;
    }


    public String getLanguage()
    {
        return language;
    }


    @Override
    public String toString()
    {
        return "\"" + value.replace("\"", "\\\"") + "\"@" + language;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof LanguageTaggedLiteral))
            return false;

        LanguageTaggedLiteral literal = (LanguageTaggedLiteral) obj;

        return value.equals(literal.value) && language.equals(literal.language);
    }
}

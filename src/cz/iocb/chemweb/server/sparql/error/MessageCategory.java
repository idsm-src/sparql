package cz.iocb.chemweb.server.sparql.error;



public enum MessageCategory
{
    ERROR("error"), WARNING("warning");

    private String text;

    MessageCategory(String text)
    {
        this.text = text;
    }


    public String getText()
    {
        return text;
    }
}

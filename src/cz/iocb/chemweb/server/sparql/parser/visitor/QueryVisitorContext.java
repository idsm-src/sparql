package cz.iocb.chemweb.server.sparql.parser.visitor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import cz.iocb.chemweb.server.sparql.parser.error.ParseException;
import cz.iocb.chemweb.server.sparql.parser.model.Prefix;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;



public class QueryVisitorContext
{
    private Prologue prologue;
    private LinkedHashMap<String, ProcedureDefinition> procedures;
    private List<Prefix> predefinedPrefixes;
    private Consumer<ParseException> exceptionConsumer;

    public final Prologue getPrologue()
    {
        return prologue;
    }

    public final void setPrologue(Prologue prologue)
    {
        this.prologue = prologue;
    }

    public final LinkedHashMap<String, ProcedureDefinition> getProcedures()
    {
        return procedures;
    }

    public final void setProcedures(LinkedHashMap<String, ProcedureDefinition> procedures)
    {
        this.procedures = procedures;
    }

    public final List<Prefix> getPredefinedPrefixes()
    {
        return predefinedPrefixes;
    }

    public final void setPredefinedPrefixes(List<Prefix> predefinedPrefixes)
    {
        this.predefinedPrefixes = predefinedPrefixes;
    }

    public final Consumer<ParseException> getExceptionConsumer()
    {
        return exceptionConsumer;
    }

    public final void setExceptionConsumer(Consumer<ParseException> exceptionConsumer)
    {
        this.exceptionConsumer = exceptionConsumer;
    }
}

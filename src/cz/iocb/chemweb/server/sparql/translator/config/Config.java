package cz.iocb.chemweb.server.sparql.translator.config;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;



/**
 * Auxiliary class storing details of inner procedures, that can be used in a SPARQL query. The instance of this object
 * is usually created by parsing the configuretion *.ini file with ConfigFileParser.
 */
public class Config
{
    private final List<ProcedureDefinition> procedureRecords = new ArrayList<>();

    /**
     * Stores an inner procedure details.
     * 
     * @param procedure Procedure detail to be stored.
     */
    public void addProcedure(ProcedureDefinition procedure)
    {
        this.procedureRecords.add(procedure);
    }

    /**
     * Gets a list of procedures details.
     * 
     * @return Collection of procedures details currently stored.
     */
    public List<ProcedureDefinition> getProcedures()
    {
        return this.procedureRecords;
    }

    /**
     * Finds a stored procedure detail by its name.
     * 
     * @param procName The name of a procedure to be find.
     * @return Inner procedure detail.
     */
    public ProcedureDefinition getProcedureByName(String procName)
    {
        for(ProcedureDefinition rec : this.procedureRecords)
        {
            if(rec.getProcName().equals(procName))
                return rec;
        }

        return null;
    }

    /**
     * Gets a list of procedure names in the form of IRI.
     * 
     * @return Collection of IRIs.
     */
    public List<IRI> getProceduresNames()
    {
        List<IRI> retProcedures = new ArrayList<>();

        for(ProcedureDefinition proc : procedureRecords)
        {
            retProcedures.add(new IRI(proc.getProcName()));
        }

        return retProcedures;
    }
}

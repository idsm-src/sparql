package cz.iocb.chemweb.server.sparql.translator.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;



/**
 * Auxiliary class for parsing configuration object from *.ini file.
 */
public class ConfigFileParser
{
    /**
     * Parses configuration file and checks the validity of it.
     * 
     * @param configFileName Filepath to the *.ini configuration file.
     * @return Configuration object storing the details of inner procedures.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ConfigFileParseException
     */
    public static Config parse(String configFileName)
            throws FileNotFoundException, IOException, ConfigFileParseException
    {
        /*
         * ---Example---
         * 
         * config-file:
         * 
         * @<http://example.org/sim> xsd:integer $sim(x, y) x =
         * <http://example.org/x> 5 xsd:integer y = <http://example.org/y> 6
         * xsd:integer
         * 
         * config-object:
         * 
         * Config config = new Config();
         * 
         * ProcedureDefinition proc = new
         * ProcedureDefinition("<http://example.org/sim>", "sim",
         * "<http://www.w3.org/2001/XMLSchema#integer>");
         * 
         * ParameterDefinition treshParam = new
         * ParameterDefinition("<http://example.org/tresh>",
         * "<http://www.w3.org/2001/XMLSchema#integer>", "5");
         * proc.addParameter(treshParam);
         * 
         * ParameterDefinition coParam = new
         * ParameterDefinition("<http://example.org/co>",
         * "<http://www.w3.org/2001/XMLSchema#integer>", "'SMILES'");
         * proc.addParameter(coParam);
         * 
         * config.addProcedure(proc);
         */

        Config config = new Config();
        ProcedureDefinition currentProc = null;

        BufferedReader br = new BufferedReader(new FileReader(configFileName));

        String line;
        int lineNo = 0;

        while((line = br.readLine()) != null)
        {
            line = line.trim();
            ++lineNo;

            if(line.length() == 0)
                continue;

            char firstChar = line.charAt(0);
            switch(firstChar)
            {
                case '%': // comment line
                    continue;

                case '@': // new SPARQL procedure
                {
                    line = line.replaceFirst("@", "").trim();
                    if(currentProc != null) // new procedure definition occured,
                    // while the previous is not stored
                    // yet
                    {
                        if(isProcedureDefinedOK(currentProc, lineNo))
                        {
                            config.addProcedure(currentProc);
                        }
                        currentProc = null;
                    }

                    int separatorIdx = line.indexOf(" ");
                    if(separatorIdx == -1)
                        separatorIdx = line.length();

                    String sparqlProcName = line.substring(0, separatorIdx).trim();
                    String procTypeIRI = line.substring(separatorIdx).trim();

                    if(procTypeIRI.isEmpty())
                    {
                        procTypeIRI = null;
                    }

                    currentProc = new ProcedureDefinition(sparqlProcName, getFullIriForXsd(procTypeIRI, lineNo));
                    break;
                }
                case '$': // mapped SQL procedure to the current SPARQL
                // procedure
                {
                    line = line.replaceFirst("\\$", "").trim();
                    if(currentProc == null)
                    {
                        throw new ConfigFileParseException(
                                "SPARQL procedure name has to be defined first, before assigning mapped SQL procedure.",
                                lineNo);
                    }

                    int bracketIdx = line.indexOf("(");
                    if(bracketIdx == -1)
                        bracketIdx = line.length();

                    String mappedProcName = line.substring(0, bracketIdx).trim();
                    currentProc.setMappedProcName(mappedProcName);

                    String paramStr = line.substring(bracketIdx).trim();
                    paramStr = paramStr.replace("(", "").replace(")", "");
                    String[] paramList = paramStr.split(",");

                    for(int i = 0; i < paramList.length; ++i)
                    {
                        String mappedParamName = paramList[i].trim();
                        if(!mappedParamName.isEmpty())
                        {
                            ParameterDefinition newParam = new ParameterDefinition(mappedParamName);
                            currentProc.addParameter(newParam);
                        }
                    }
                    break;
                }
                case '#': // result parameter
                {
                    line = line.replaceFirst("#", "").trim();
                    if(currentProc == null)
                    {
                        throw new ConfigFileParseException(
                                "SPARQL procedure name has to be defined first, before assigning result parameters.",
                                lineNo);
                    }

                    int sepIdx = line.indexOf(" ");
                    if(sepIdx == -1)
                        sepIdx = line.length();

                    String sparqlResultName = line.substring(0, sepIdx).trim();
                    line = line.substring(sepIdx).trim();

                    sepIdx = line.indexOf(" ");
                    if(sepIdx == -1)
                        sepIdx = line.length();

                    String typeIRI = line.substring(0, sepIdx).trim();
                    String strCanBeNull = line.substring(sepIdx).trim();

                    if(typeIRI.isEmpty() || typeIRI.equals("-"))
                        typeIRI = null;

                    boolean canBeNull = false;
                    if(strCanBeNull.equals("NULLABLE"))
                        canBeNull = true;

                    ResultDefinition resultDef = new ResultDefinition(sparqlResultName,
                            getFullIriForXsd(typeIRI, lineNo), canBeNull);

                    currentProc.addResult(resultDef);

                    break;
                }
                default: // parameter for the current SPARQL procedure
                {
                    if(currentProc == null || currentProc.getMappedProcName() == null)
                    {
                        throw new ConfigFileParseException(
                                "SPARQL procedure and mapped SQL procedure have to be defined first, before assigning parameters.",
                                lineNo);
                    }

                    String mappedParamName = line.substring(0, line.indexOf("=")).trim();

                    line = line.substring(line.indexOf("=") + 1).trim();

                    int sepIdx = line.indexOf(" ");
                    if(sepIdx == -1)
                        sepIdx = line.length();

                    String sparqlProcName = line.substring(0, sepIdx).trim();
                    line = line.substring(sepIdx).trim();

                    sepIdx = line.indexOf(" ");
                    if(sepIdx == -1)
                        sepIdx = line.length();

                    String defaultVal = line.substring(0, sepIdx).trim();
                    String typeIRI = line.substring(sepIdx).trim();
                    if(typeIRI.isEmpty())
                        typeIRI = null;

                    ParameterDefinition param = currentProc.getParameterByMappedParamName(mappedParamName);
                    if(param != null)
                    {
                        param.setParamName(sparqlProcName);
                        param.setTypeIRI(getFullIriForXsd(typeIRI, lineNo));
                        if(!defaultVal.isEmpty())
                        {
                            if(defaultVal.equals("-"))
                                defaultVal = "";
                            defaultVal = defaultVal.replace("\"", "'");
                            param.setDefaultValue(defaultVal);
                        }
                    }
                    else
                    {
                        throw new ConfigFileParseException(
                                "Mapped parameter has to be defined first inside the mapped SQL procedure parameter list.",
                                lineNo);
                    }

                    break;
                }
            }

        }

        // store the last procedure defined
        if(currentProc != null)
        {
            if(isProcedureDefinedOK(currentProc, lineNo))
            {
                config.addProcedure(currentProc);
            }
            currentProc = null;
        }

        br.close();

        return config;
    }

    /**
     * Checks the validity of last procedure definition.
     * 
     * @param proc Procedure definition object.
     * @param lineNo Last number of line of the procedure definition in the file.
     * @return true if procedure definition is correct.
     * @throws ConfigFileParseException
     */
    private static boolean isProcedureDefinedOK(ProcedureDefinition proc, int lineNo) throws ConfigFileParseException
    {
        if(proc.getMappedProcName() == null)
        {
            throw new ConfigFileParseException(
                    "Last procedure was not defined correctly. SPARQL procedure has no assigned SQL procedure.",
                    lineNo);
        }

        for(ParameterDefinition param : proc.getParameters())
        {
            String paramName = param.getParamName();
            String mappedParamName = param.getMappedParamName();

            if(paramName == null || paramName.isEmpty() || mappedParamName == null || mappedParamName.isEmpty())
            {
                throw new ConfigFileParseException(
                        "Last procedure was not defined correctly. Some of the parameters of the mapped SQL procedure are not defined.",
                        lineNo);
            }
        }

        return true;
    }

    /**
     * Checks whether the type IRI is of type XSD; also replaces XSD prefix with the full IRI.
     * 
     * @param typeIRI Parameter type or procedure return value type (IRI).
     * @param lineNo Line number of the currently processed line.
     * @return Correct XSD type IRI.
     */
    private static String getFullIriForXsd(String typeIRI, int lineNo)
    {
        if(typeIRI == null)
            return null;

        if(typeIRI.startsWith("xsd:"))
        {
            typeIRI = typeIRI.replaceFirst("xsd:", "<http://www.w3.org/2001/XMLSchema#") + ">";
        }
        // else if (!typeIRI.startsWith("<http://www.w3.org/2001/XMLSchema#"))
        // {
        // throw new
        // ConfigFileParseException("Invalid type used - you should use XSD datatype.",
        // lineNo);
        // }

        return typeIRI;
    }
}

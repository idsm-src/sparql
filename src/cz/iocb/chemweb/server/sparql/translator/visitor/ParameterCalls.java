package cz.iocb.chemweb.server.sparql.translator.visitor;

import java.util.ArrayList;
import java.util.List;



/**
 * Auxiliary class for holding the information of parameter calls during the translation of inner procedure calls.
 */
public class ParameterCalls
{
    private List<ParameterCall> arr = new ArrayList<>();

    /**
     * Finds the parameter call details by the parameter name.
     * 
     * @param parName Parameter name.
     * @return Parameter detail.
     */
    public ParameterCall find(String parName)
    {
        for(int i = 0; i < arr.size(); ++i)
        {
            ParameterCall parCall = arr.get(i);
            if(parCall.getParName().equals(parName))
                return parCall;
        }
        return null;
    }

    /**
     * Gets the list of parameter calls.
     * 
     * @return Collection of parameter calls.
     */
    public List<ParameterCall> getArr()
    {
        return this.arr;
    }
}

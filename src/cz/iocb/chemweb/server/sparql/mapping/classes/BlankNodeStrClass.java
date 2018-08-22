package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.BLANKNODEINT;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.BlankNodeBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class BlankNodeStrClass extends BlankNodeBaseClass
{
    BlankNodeStrClass()
    {
        super("str_blanknode", "varchar", BLANKNODEINT);
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return "sparql.cast_as_rdfbox_from_str_blanknode(" + variableAccessor.variableAccess(variable, this, 0) + ")";
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return this;
    }


    @Override
    public String getSqlPatternValue(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        return "sparql.rdfbox_extract_str_blanknode" + "(" + column + ")";
    }
}

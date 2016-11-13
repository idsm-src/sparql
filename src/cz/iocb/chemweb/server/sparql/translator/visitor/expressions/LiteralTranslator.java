package cz.iocb.chemweb.server.sparql.translator.visitor.expressions;

import java.text.SimpleDateFormat;



/**
 * Auxiliary translator class for handling the literal values.
 *
 */
public class LiteralTranslator
{
    /**
     * Status of type-checking of literal values.
     */
    public enum Status
    {
        /**
         * Type of the literal value is OK, no need for casting.
         */
        TYPE_OK,

        /**
         * The literal value needs to be casted.
         */
        CAST_NEEDED,

        /**
         * The literal value cannot be properly casted.
         */
        TYPE_ERROR
    };

    /**
     * IDs of suppoerted xsd types.
     */
    private enum TypeID
    {
        INTEGER, DOUBLE, DECIMAL, FLOAT, LONG, INT, SHORT, BYTE, NONPOSINTEGER, NONNEGINTEGER, POSINTEGER, NEGINTEGER, ULONG, UINT, USHORT, UBYTE, STRING, BOOLEAN, DATE, DATETIME, USER_DEF, NOT_TYPED_STRING
    };

    /**
     * Maps the literal type (in the form of IRI) to the corresponding type ID.
     *
     * @param literalTypeIRI Literal type in the form of IRI.
     * @return Literal type ID.
     */
    private static TypeID getMappedXsdTypeID(String literalTypeIRI)
    {
        String literalType = literalTypeIRI.replace("http://www.w3.org/2001/XMLSchema#", "xsd:");
        switch(literalType)
        {
            case "xsd:integer":
                return TypeID.INTEGER;
            case "xsd:double":
                return TypeID.DOUBLE;
            case "xsd:decimal":
                return TypeID.DECIMAL;

            case "xsd:float":
                return TypeID.FLOAT;
            case "xsd:long":
                return TypeID.LONG;
            case "xsd:int":
                return TypeID.INT;
            case "xsd:short":
                return TypeID.SHORT;
            case "xsd:byte":
                return TypeID.BYTE;

            case "xsd:unsignedLong":
                return TypeID.ULONG;
            case "xsd:unsignedInt":
                return TypeID.UINT;
            case "xsd:unsignedShort":
                return TypeID.USHORT;
            case "xsd:unsignedByte":
                return TypeID.UBYTE;

            case "xsd:nonPositiveInteger":
                return TypeID.NONPOSINTEGER;
            case "xsd:nonNegativeInteger":
                return TypeID.NONNEGINTEGER;
            case "xsd:positiveInteger":
                return TypeID.POSINTEGER;
            case "xsd:negativeInteger":
                return TypeID.NEGINTEGER;

            case "xsd:boolean":
                return TypeID.BOOLEAN;
            case "xsd:date":
                return TypeID.DATE;
            case "xsd:dateTime":
                return TypeID.DATETIME;

            case "xsd:string":
                return TypeID.STRING;
            case "NOT_TYPED_STRING":
                return TypeID.NOT_TYPED_STRING;
            default:
                return TypeID.USER_DEF; // xsd:user-def-type
        }
    }

    /**
     * Checks whether the literal value matches to its type.
     *
     * @param literalTypeIRI Literal type in the form of IRI.
     * @param literalVal Literal value.
     * @param literalLangTag Literal language tag (null if none present).
     * @param suppressLiteralConversion Is ExpressionTranslateFlag.SUPPRESS_LITERAL_CONVERSION active?
     * @return Status.TYPE_OK if the literal value matches its type, or Status.CAST_NEEDED if some literal casting is
     *         needed; Status.TYPE_ERROR otherwise.
     */
    public static Status checkTypeMatch(String literalTypeIRI, String literalVal, String literalLangTag,
            boolean suppressLiteralConversion)
    {
        // if (suppressLiteralConversion)
        // return Status.TYPE_OK;

        TypeID literalType = getMappedXsdTypeID(literalTypeIRI);

        switch(literalType)
        {
            case INTEGER:
            case DOUBLE:
            case DECIMAL:

            case FLOAT:
            case LONG:
            case INT:
            case SHORT:
            case BYTE:

            case ULONG:
            case UINT:
            case USHORT:
            case UBYTE:

            case NONPOSINTEGER:
            case NONNEGINTEGER:
            case POSINTEGER:
            case NEGINTEGER:
                return checkNumber(literalVal, literalType);

            case BOOLEAN:
                return literalVal.equals("true") || literalVal.equals("false") ? Status.TYPE_OK : Status.TYPE_ERROR;

            case DATE:
            case DATETIME:
                return checkDateTime(literalVal, literalType);

            case NOT_TYPED_STRING:
                return literalLangTag == null ? Status.TYPE_OK : Status.CAST_NEEDED;

            case STRING:
            default: // xsd:user-def-type
                return Status.CAST_NEEDED;
        }
    }

    /**
     * Checks whether the literal value corresponds to the given numeric type.
     *
     * @param val Literal value.
     * @param typeID ID of type to check.
     * @return Status.TYPE_OK if the literal value matches its type, or Status.CAST_NEEDED if some literal casting is
     *         needed; Status.TYPE_ERROR otherwise.
     */
    private static Status checkNumber(String val, TypeID typeID)
    {
        if(checkNumeric(val) == Status.TYPE_OK)
        {
            // check (un)signed validity
            switch(typeID)
            {
                case ULONG:
                case UINT:
                case USHORT:
                case UBYTE:
                    if(checkNonNegInteger(val) != Status.TYPE_OK)
                        return Status.CAST_NEEDED;
                case NONPOSINTEGER:
                    return checkNonPosInteger(val);
                case NONNEGINTEGER:
                    return checkNonNegInteger(val);
                case POSINTEGER:
                    return checkPosInteger(val);
                case NEGINTEGER:
                    return checkNegInteger(val);
                default:
                    break;
            }

            // check types ranges
            try
            {
                switch(typeID)
                {
                    case INTEGER:
                    case DECIMAL:
                    case DOUBLE:
                    case ULONG:
                    case NONPOSINTEGER:
                    case NONNEGINTEGER:
                    case POSINTEGER:
                    case NEGINTEGER:
                        break; // --> no further limitations
                    case FLOAT:
                        Float.parseFloat(val);
                        break;
                    case LONG:
                    case UINT: // fall-through
                        Long.parseLong(val);
                        break;
                    case INT:
                    case USHORT: // fall-through
                        Integer.parseInt(val);
                        break;
                    case SHORT:
                    case UBYTE: // fall-through
                        Short.parseShort(val);
                        break;
                    case BYTE:
                        Byte.parseByte(val);
                        break;
                    default:
                        break;
                }

            }
            catch (NumberFormatException e)
            {
                return Status.CAST_NEEDED;
            }
        }
        else
        {
            return Status.TYPE_ERROR;
        }

        return Status.TYPE_OK;
    }

    /**
     * Checks whether the literal corresponds to xsd:dateTime or xsd:date format.
     *
     * @param val Literal value.
     * @param typeID TypeID.DATE or TypeID.DATETIME.
     * @return Status.TYPE_OK if the literal value is of type xsd:dateTime; Status.TYPE_ERROR otherwise.
     */
    private static Status checkDateTime(String val, TypeID typeID)
    {
        SimpleDateFormat formatter;
        if(typeID == TypeID.DATE)
        {
            // supported date format: YYYY-MM-DD
            formatter = new SimpleDateFormat("YYYY-MM-DD");
        }
        else
        // TypeID.DATETIME
        {
            // supported dateTime format: YYYY-MM-DDThh:mm:ss
            formatter = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss");
        }

        try
        {
            formatter.parse(val);
        }
        catch (java.text.ParseException ex)
        {
            return Status.TYPE_ERROR;
        }

        return Status.TYPE_OK;
    }

    /**
     * Checks whether the literal value is numeric.
     *
     * @param val Literal value.
     * @return Status.TYPE_OK if the literal value is of numeric type; Status.TYPE_ERROR otherwise.
     */
    private static Status checkNumeric(String val)
    {
        return val.matches("^[-+]?\\d+(\\.\\d+)?$") ? Status.TYPE_OK : Status.TYPE_ERROR;
    }

    /**
     * Checks whether the numeric literal value is a non-positive integer.
     *
     * @param num Numeric literal value.
     * @return Status.TYPE_OK if the literal value is a non-positive integer; Status.CAST_NEEDED otherwise.
     */
    private static Status checkNonPosInteger(String num)
    {
        return num.matches("^[-]\\d+$") ? Status.TYPE_OK : Status.CAST_NEEDED;
    }

    /**
     * Checks whether the numeric literal value is a non-negative integer.
     *
     * @param num Numeric literal value.
     * @return Status.TYPE_OK if the literal value is a non-negative integer; Status.CAST_NEEDED otherwise.
     */
    private static Status checkNonNegInteger(String num)
    {
        return num.matches("^[+]?\\d+$") ? Status.TYPE_OK : Status.CAST_NEEDED;
    }

    /**
     * Checks whether the numeric literal value is a positive integer.
     *
     * @param num Numeric literal value.
     * @return Status.TYPE_OK if the literal value is a positive integer; Status.CAST_NEEDED otherwise.
     */
    private static Status checkPosInteger(String num)
    {
        return checkNonNegInteger(num) == Status.TYPE_OK && !isZero(num) ? Status.TYPE_OK : Status.CAST_NEEDED;
    }

    /**
     * Checks whether the numeric literal value is a negative integer.
     *
     * @param num Numeric literal value.
     * @return Status.TYPE_OK if the literal value is a negative integer; Status.CAST_NEEDED otherwise.
     */
    private static Status checkNegInteger(String num)
    {
        return checkNonPosInteger(num) == Status.TYPE_OK && !isZero(num) ? Status.TYPE_OK : Status.CAST_NEEDED;
    }

    /**
     * Checks whether the numeric literal value is zero.
     *
     * @param num Numeric literal value.
     * @return true if literal value is zero; false otherwise
     */
    private static boolean isZero(String num)
    {
        return num.matches("^[-+]?\\[0]+(\\.\\[0]+)?$");
    }

    /**
     * Returns the correct form of literal value (in the context of SQL query).
     *
     * @param literalTypeIRI Literal type in the form of IRI.
     * @param literalVal Literal value.
     * @param castNeeded Is cast required for the literal? (based on checkTypeMatch() method)
     * @param literalLangTag Literal language tag (null if none present).
     * @param suppressLiteralConversion Is ExpressionTranslateFlag.SUPPRESS_LITERAL_CONVERSION active?
     * @param longCast Is ExpressionTranslateFlag.LONG_CAST active?
     * @return String of a (possibly casted) literal value.
     */
    public static String makeLiteralStr(String literalTypeIRI, String literalVal, boolean castNeeded,
            String literalLangTag, boolean suppressLiteralConversion, boolean longCast)
    {
        if(longCast)
            castNeeded = true;

        TypeID literalType = getMappedXsdTypeID(literalTypeIRI);

        String prefixStr = suppressLiteralConversion || longCast ? "" : "DB.DBA.RDF_OBJ_OF_SQLVAL(";
        String litVal = literalVal;
        String suffixStr = suppressLiteralConversion || longCast ? "" : ")";

        if(castNeeded && !suppressLiteralConversion)
        {
            String typeIRIStr = literalType != TypeID.NOT_TYPED_STRING ? "UNAME'" + literalTypeIRI + "'" : "NULL";
            String langTagStr = literalLangTag != null ? "'" + literalLangTag + "'" : "NULL";

            if(longCast)
            {
                if(literalType == TypeID.NOT_TYPED_STRING && literalLangTag == null)
                    return "'" + litVal + "'";

                prefixStr = "DB.DBA.RDF_MAKE_LONG_OF_TYPEDSQLVAL_STRINGS('";
                suffixStr = "', " + typeIRIStr + ", " + langTagStr + ")";
            }
            else
            {
                prefixStr = "DB.DBA.RDF_MAKE_OBJ_OF_TYPEDSQLVAL('";
                suffixStr = "', __i2id(" + typeIRIStr + "), " + langTagStr + ")";
            }
        }

        switch(literalType)
        {
            case FLOAT:
                litVal = "CAST (" + litVal + " as float)";
                break;

            case LONG:
            case ULONG:
                // litVal = litVal + ".0"; // some internal thing for Virtuoso
                break;

            case INT:
            case SHORT:
            case BYTE:
            case UINT:
            case USHORT:
            case UBYTE:
            case NONPOSINTEGER:
            case NONNEGINTEGER:
            case POSINTEGER:
            case NEGINTEGER:
                break;

            case INTEGER:
                // if (!castNeeded)
                litVal = getIntegerPartOfNumber(litVal);
                prefixStr = suffixStr = "";
                break;
            case DOUBLE:
            case DECIMAL:
                // if (!castNeeded)
                litVal = convertNumberToDecimal(litVal);
                prefixStr = suffixStr = "";
                break;

            case BOOLEAN:
                litVal = litVal.equals("true") ? "1" : "0";
                prefixStr = suffixStr = "";
                break;

            case DATE:
                prefixStr = suffixStr = "";
                litVal = "CAST ('" + litVal + "' AS DATE)";
                break;

            case DATETIME:
                prefixStr = suffixStr = "";
                litVal = "CAST ('" + litVal + "' AS DATETIME)";
                break;

            case NOT_TYPED_STRING:
            case STRING:
            default: // xsd:user-def-type
                String apost = "'";
                if(castNeeded && !suppressLiteralConversion)
                    apost = "";
                litVal = apost + literalVal + apost;
        }

        return prefixStr + litVal + suffixStr;
    }

    /**
     * Try to get the integer part of a numeric value.
     *
     * @param litVal Numeric literal value.
     * @return String of an integer part of a numeric value.
     */
    private static String getIntegerPartOfNumber(String litVal)
    {
        long num;
        try
        {
            num = Long.parseLong(litVal);
        }
        catch (NumberFormatException e)
        {
            try
            {
                double doubleNum = Double.parseDouble(litVal);
                num = (long) doubleNum;
            }
            catch (NumberFormatException e2)
            {
                return litVal;
            }

        }

        return Long.toString(num);
    }

    /**
     * Try to convert a numeric literal value to the decimal form.
     *
     * @param litVal Numeric literal value.
     * @return String of a decimal form of a numeric value.
     */
    private static String convertNumberToDecimal(String litVal)
    {
        try
        {
            double num = Double.parseDouble(litVal);
            return Double.toString(num);
        }
        catch (NumberFormatException e)
        {
            return litVal;
        }
    }

    /**
     * Wrap a result value of an inner procedure call (SQL values returned by a procedure need to be typed for correct
     * table joins).
     *
     * @param resultVar Procedure result variable.
     * @return String of a typed result variable.
     */
    public static String translateProcedureRetVar(String resultVar, String typeIRI)
    {
        return "DB.DBA.RDF_OBJ_OF_SQLVAL(" + resultVar + ")";
    }
}

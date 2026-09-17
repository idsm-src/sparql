package cz.iocb.sparql.engine.mapping.classes;

import java.util.Locale;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.UserType;
import cz.iocb.sparql.engine.mapping.datatypes.UserDatatype;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * User datatypes used by the tests of {@link UserLiteralClass} and {@link UserLiteralBaseClass}.
 *
 * The three datatypes differ in the way the boxed sql value is stored: int4 is passed by value, uuid is a fixed-length
 * type passed by reference and varchar is a varlena type.
 */
public class UserDatatypes
{
    public static final UserDatatype intDatatype = new IntDatatype();
    public static final UserDatatype uuidDatatype = new UuidDatatype();
    public static final UserDatatype tokenDatatype = new TokenDatatype();


    /**
     * An int4 value; the canonical form has no leading sign, no leading zeros and no white spaces.
     */
    private static class IntDatatype extends UserDatatype
    {
        private static final Pattern pattern = Pattern.compile(WS + "[+-]?[0-9]+" + WS);


        IntDatatype()
        {
            super(new Iri("http://example.org/datatype#int"), "int", new UserType("int4"));
        }


        @Override
        public boolean isValidForm(String value)
        {
            if(!pattern.matcher(value).matches())
                return false;

            try
            {
                Integer.parseInt(getCanonicalLexicalForm(value));
            }
            catch(NumberFormatException e)
            {
                return false;
            }

            return true;
        }


        @Override
        public String getCanonicalLexicalForm(String value)
        {
            String form = getCollapsedForm(value);

            boolean negative = form.startsWith("-");

            if(negative || form.startsWith("+"))
                form = form.substring(1);

            form = form.replaceFirst("^0+(?=[0-9])", "");

            return negative && !form.equals("0") ? "-" + form : form;
        }
    }


    /**
     * A uuid value; the canonical form uses lower case hexadecimal digits only.
     */
    private static class UuidDatatype extends UserDatatype
    {
        private static final Pattern pattern = Pattern
                .compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");


        UuidDatatype()
        {
            super(new Iri("http://example.org/datatype#uuid"), "uuid", new UserType("uuid"));
        }


        @Override
        public boolean isValidForm(String value)
        {
            return pattern.matcher(value).matches();
        }


        @Override
        public String getCanonicalLexicalForm(String value)
        {
            return value.toLowerCase(Locale.US);
        }
    }


    /**
     * A varchar value; the canonical form has its white spaces collapsed and must not be empty.
     */
    private static class TokenDatatype extends UserDatatype
    {
        TokenDatatype()
        {
            super(new Iri("http://example.org/datatype#token"), "token", new UserType("varchar"));
        }


        @Override
        public boolean isValidForm(String value)
        {
            return !getCollapsedForm(value).isEmpty();
        }


        @Override
        public String getCanonicalLexicalForm(String value)
        {
            return getCollapsedForm(value);
        }
    }
}

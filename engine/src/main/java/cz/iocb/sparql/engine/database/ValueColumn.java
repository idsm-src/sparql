package cz.iocb.sparql.engine.database;

import java.util.Objects;



/**
 * The typed constant {@code 'literal'::type} of a non-null value.
 */
public final class ValueColumn extends ConstantColumn
{
    /**
     * Unquoted literal value.
     */
    private final String literal;


    /**
     * Creates the constant.
     *
     * @param literal the literal value
     * @param type the SQL type
     */
    public ValueColumn(String literal, SqlType type)
    {
        super(quote(Objects.requireNonNull(literal)) + "::" + type, type);
        this.literal = literal;
    }


    /**
     * The SQL string constant of the text, which stands for the text whatever the setting
     * {@code standard_conforming_strings} of the server is: the quotes are doubled, and a text holding a backslash is
     * written as an escape string constant ({@code E'...'}) with its backslashes doubled, as a backslash in a plain
     * constant escapes the next character when the setting is off.
     *
     * @param text the text
     * @return the SQL string constant of the text
     */
    public static String quote(String text)
    {
        String quoted = text.replace("'", "''");

        return quoted.indexOf('\\') < 0 ? "'" + quoted + "'" : "E'" + quoted.replace("\\", "\\\\") + "'";
    }


    /**
     * The unquoted literal value.
     *
     * @return the unquoted literal value
     */
    public String getValue()
    {
        return literal;
    }


    @Override
    public boolean canBeNull()
    {
        return false;
    }
}

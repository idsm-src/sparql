package cz.iocb.sparql.engine.request;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;



/**
 * The column names of a request: a name is shortened when its size, reckoned as PostgreSQL may need it in any encoding
 * of a server (a byte for a character of ASCII, four for any other one), reaches 63 bytes, at the boundary of a
 * character and with a counter keeping the shortened names unique; and two unequal classes of one name get different
 * labels, also when a class is named like the label of another one.
 */
public class ColumnMapTest
{
    @Test
    @DisplayName("names of ASCII are shortened from 63 characters on")
    void ascii()
    {
        ColumnMap map = new ColumnMap();

        assertThat(map.getSafeName("x".repeat(62)), is("x".repeat(62)));
        assertThat(map.getSafeName("x".repeat(63)), is("x".repeat(55) + "00000000"));
        assertThat(map.getSafeName("x".repeat(64)), is("x".repeat(55) + "00000001"));
        assertThat(map.getSafeName("x".repeat(63)), is("x".repeat(55) + "00000000"));
    }


    @Test
    @DisplayName("a character beyond ASCII counts as four bytes and is never cut")
    void otherCharacters()
    {
        ColumnMap map = new ColumnMap();
        String c = Character.toString(0x10D);
        String d = Character.toString(0x1D400);

        assertThat(map.getSafeName(c.repeat(15)), is(c.repeat(15)));
        assertThat(map.getSafeName(c.repeat(16)), is(c.repeat(13) + "___00000000"));
        assertThat(map.getSafeName(c.repeat(17)), is(c.repeat(13) + "___00000001"));
        assertThat(map.getSafeName("x" + c.repeat(16)), is("x" + c.repeat(13) + "__00000000"));

        // a surrogate pair stays whole
        assertThat(map.getSafeName(d.repeat(15)), is(d.repeat(15)));
        assertThat(map.getSafeName(d.repeat(16)), is(d.repeat(13) + "___00000000"));
        assertThat(map.getSafeName("xx" + d.repeat(16)), is("xx" + d.repeat(13) + "_00000000"));
        assertThat(map.getSafeName("xxx" + d.repeat(16)), is("xxx" + d.repeat(13) + "00000000"));

        // a name kept as it is never equals a shortened one, which is padded to the limit
        String kept = c.repeat(13) + "00000000";

        assertThat(map.getSafeName(kept), is(kept));
        assertThat(map.getSafeName(c.repeat(16)), not(kept));
    }


    @Test
    @DisplayName("an unequal class of the name of another one gets another label")
    void labels()
    {
        ColumnMap map = new ColumnMap();
        String name = xsdString.getResourceName();
        StringUserIriClass string = new StringUserIriClass(name, "http://example.org/s/");
        StringUserIriClass numbered = new StringUserIriClass(name + "~1", "http://example.org/t/");

        assertThat(map.getLabel(xsdString), is(name));
        assertThat(map.getLabel(numbered), is(name + "~1"));
        assertThat(map.getLabel(string), is(name + "~2"));

        // an equal class gets the same label
        assertThat(map.getLabel(new StringUserIriClass(name, "http://example.org/s/")), is(name + "~2"));
        assertThat(map.getLabel(xsdString), is(name));
        assertThat(map.getLabel(numbered), is(name + "~1"));
    }
}

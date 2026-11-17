package cz.iocb.sparql.engine.request;

import static java.util.Locale.ROOT;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Names of the generated columns of one request: shortens a name that exceeds the identifier limit of PostgreSQL,
 * keeping the mapping stable within the request so the same long name always gets the same short one, and labels the
 * classes the names are made of, so that two unequal classes of one name get different labels.
 */
public class ColumnMap
{
    /**
     * Size of a name, in bytes of the encoding of the server, from which on it is shortened: PostgreSQL truncates an
     * identifier to 63 bytes.
     */
    private static final int limit = 63;

    /**
     * Number of hexadecimal digits of the uniqueness suffix.
     */
    private static final int numpart = 8;

    /**
     * The most bytes a character other than one of ASCII takes in an encoding PostgreSQL allows for a server (four in
     * UTF-8, in EUC_JP, EUC_TW and MULE_INTERNAL), as the size of a name is reckoned by it whatever the encoding is.
     */
    private static final int maxCharacterSize = 4;

    /**
     * Shortened names by original name.
     */
    private Map<String, String> map = new HashMap<>();

    /**
     * Labels of the classes labelled so far.
     */
    private Map<ResourceClass, String> labels = new HashMap<>();

    /**
     * The labels given so far.
     */
    private Set<String> usedLabels = new HashSet<>();


    /**
     * Creates an empty map.
     */
    public ColumnMap()
    {
    }


    /**
     * The name itself if short enough, otherwise its unique shortened form: its longest beginning of at most
     * {@code limit - numpart} bytes that ends at the boundary of a character, followed by a hexadecimal counter.
     *
     * @param name the name
     * @return the name itself if short enough, otherwise its unique shortened form
     */
    public String getSafeName(String name)
    {
        if(getSize(name) < limit)
            return name;

        if(map.containsKey(name))
            return map.get(name);

        String shortName = getBeginning(name, limit - numpart);

        for(int i = 0;; i++)
        {
            String safe = shortName + String.format(ROOT, "%0" + numpart + "X", i);

            if(!map.values().contains(safe))
            {
                map.put(name, safe);
                return safe;
            }
        }
    }


    /**
     * The label of the class in the names of the columns of the request, unique among the labels of unequal classes:
     * its name, unless the label is given to another class already (the names of the classes are unique by convention
     * only, and a user class may take the name of a built-in one), in which case the name followed by {@code ~} and the
     * least number giving a label that is not given yet.
     *
     * @param resClass the class
     * @return the label of the class
     */
    public String getLabel(ResourceClass resClass)
    {
        String label = labels.get(resClass);

        if(label == null)
        {
            String name = resClass.getResourceName();
            label = name;

            for(int i = 1; usedLabels.contains(label); i++)
                label = name + "~" + i;

            labels.put(resClass, label);
            usedLabels.add(label);
        }

        return label;
    }


    /**
     * The size of the text in bytes as it is reckoned against the limit: one byte for a character of ASCII and
     * {@link #maxCharacterSize} for any other one.
     *
     * @param text the text
     * @return the size of the text
     */
    private static int getSize(String text)
    {
        return text.codePoints().map(c -> c < 0x80 ? 1 : maxCharacterSize).sum();
    }


    /**
     * The longest beginning of the text of at most the given size (see {@link #getSize}) that ends at the boundary of a
     * character, never inside a surrogate pair.
     *
     * @param text the text
     * @param size the size
     * @return the longest beginning of the text of at most the given size
     */
    private static String getBeginning(String text, int size)
    {
        int end = 0;
        int used = 0;

        while(end < text.length())
        {
            int character = text.codePointAt(end);
            int characterSize = character < 0x80 ? 1 : maxCharacterSize;

            if(used + characterSize > size)
                break;

            used += characterSize;
            end += Character.charCount(character);
        }

        return text.substring(0, end);
    }
}

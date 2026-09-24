package cz.iocb.sparql.engine.translator;



/**
 * Immutable key-value pair.
 *
 * @param <KeyType> the key type
 * @param <ValueType> the value type
 */
public class Pair<KeyType, ValueType>
{
    /**
     * The key.
     */
    private final KeyType key;

    /**
     * The value.
     */
    private final ValueType value;


    /**
     * Creates the pair.
     *
     * @param key the key
     * @param value the value
     */
    public Pair(KeyType key, ValueType value)
    {
        this.key = key;
        this.value = value;
    }


    /**
     * The key.
     *
     * @return the key
     */
    public KeyType getKey()
    {
        return key;
    }


    /**
     * The value.
     *
     * @return the value
     */
    public ValueType getValue()
    {
        return value;
    }
}

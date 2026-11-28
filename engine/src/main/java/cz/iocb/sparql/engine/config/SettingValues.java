package cz.iocb.sparql.engine.config;

import static java.util.Locale.ROOT;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import cz.iocb.sparql.engine.database.Function;



/**
 * Values of settings: the checks of their ranges and their text forms, by which the attributes of a resource in
 * {@code context.xml} or the initial parameters of a servlet give them: a decimal number, possibly with an exponent, an
 * integer, {@code true} or {@code false}, the name of a constant of an enumeration in lower case with its words joined
 * by hyphens, a function as its schema and its name joined by a dot, and a time as an integer followed by its unit
 * ({@code us}, {@code ms}, {@code s}, {@code min}, {@code h} or {@code d}, as PostgreSQL writes them for its settings),
 * or without a unit in the unit of the setting. A value out of the range of its setting and a text that gives no value
 * are refused by an error naming the setting; the spaces around a text and between a number and its unit are left out.
 */
public final class SettingValues
{
    /**
     * Units of a time, by the names PostgreSQL gives them.
     */
    private static final Map<String, ChronoUnit> units = Map.of("us", ChronoUnit.MICROS, "ms", ChronoUnit.MILLIS, "s",
            ChronoUnit.SECONDS, "min", ChronoUnit.MINUTES, "h", ChronoUnit.HOURS, "d", ChronoUnit.DAYS);

    /**
     * A decimal number: digits with a decimal point, if any, followed by an exponent, if any; not the other texts Java
     * reads as numbers, like {@code NaN}, {@code Infinity}, hexadecimal numbers or numbers followed by a type suffix.
     */
    private static final Pattern number = Pattern
            .compile("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");

    /**
     * A time: an integer followed by its unit, if any.
     */
    private static final Pattern time = Pattern.compile("([0-9]+)\\s*([a-z]*)");


    /**
     * Creates nothing; the class has static members only.
     */
    private SettingValues()
    {
    }


    /**
     * Checks that the value of a setting is a positive finite number.
     *
     * @param name name of the setting
     * @param value the value
     * @return the value
     * @throws IllegalArgumentException if the value is not a positive finite number
     */
    public static double checkPositive(String name, double value)
    {
        if(!(value > 0) || Double.isInfinite(value))
            throw new IllegalArgumentException(name + " has to be a positive finite number: " + value);

        return value;
    }


    /**
     * Checks that the value of a setting is a number greater than 0 and at most 1.
     *
     * @param name name of the setting
     * @param value the value
     * @return the value
     * @throws IllegalArgumentException if the value is not a number greater than 0 and at most 1
     */
    public static double checkFraction(String name, double value)
    {
        if(!(value > 0 && value <= 1))
            throw new IllegalArgumentException(name + " has to be a number greater than 0 and at most 1: " + value);

        return value;
    }


    /**
     * Checks that the value of a setting is a time that is not less than the given one and fits into a {@code long}
     * number of nanoseconds.
     *
     * @param name name of the setting
     * @param value the value
     * @param least the least time allowed
     * @return the value
     * @throws NullPointerException if the value is null
     * @throws IllegalArgumentException if the value is less than the least time or too long
     */
    public static Duration checkDuration(String name, Duration value, Duration least)
    {
        Objects.requireNonNull(value, name);

        if(value.compareTo(least) < 0 || value.compareTo(Duration.ofNanos(Long.MAX_VALUE)) > 0)
            throw new IllegalArgumentException(
                    name + " has to be a time from " + least.toNanos() + " to " + Long.MAX_VALUE + " ns: " + value);

        return value;
    }


    /**
     * Checks that the value of a setting lies in the given range.
     *
     * @param name name of the setting
     * @param value the value
     * @param least the least value allowed
     * @param greatest the greatest value allowed
     * @return the value
     * @throws IllegalArgumentException if the value lies out of the range
     */
    public static int checkRange(String name, int value, int least, int greatest)
    {
        if(value < least || value > greatest)
        {
            if(greatest == Integer.MAX_VALUE)
                throw new IllegalArgumentException(name + " has to be at least " + least + ": " + value);

            throw new IllegalArgumentException(
                    name + " has to be between " + least + " and " + greatest + ": " + value);
        }

        return value;
    }


    /**
     * The value of a setting given by {@code true} or {@code false}.
     *
     * @param name name of the setting
     * @param value the text
     * @return the value of the setting
     * @throws IllegalArgumentException if the text is neither {@code true} nor {@code false}
     */
    public static boolean parseBoolean(String name, String value)
    {
        return switch(value.strip())
        {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(name + " has to be true or false: " + value);
        };
    }


    /**
     * The value of a setting given by an integer.
     *
     * @param name name of the setting
     * @param value the text
     * @return the value of the setting
     * @throws IllegalArgumentException if the text is no integer an {@code int} holds
     */
    public static int parseInt(String name, String value)
    {
        try
        {
            return Integer.parseInt(value.strip());
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException(name + " has to be an integer: " + value, e);
        }
    }


    /**
     * The value of a setting given by a decimal number, possibly with an exponent.
     *
     * @param name name of the setting
     * @param value the text
     * @return the value of the setting, infinite for a number beyond the range of a {@code double}
     * @throws IllegalArgumentException if the text is no decimal number
     */
    public static double parseDouble(String name, String value)
    {
        String text = value.strip();

        if(!number.matcher(text).matches())
            throw new IllegalArgumentException(name + " has to be a number: " + value);

        return Double.parseDouble(text);
    }


    /**
     * The value of a setting given by a time: an integer followed by its unit, or without a unit in the unit of the
     * setting.
     *
     * @param name name of the setting
     * @param value the text
     * @param unit the unit of an integer without a unit
     * @return the value of the setting
     * @throws IllegalArgumentException if the text is no time
     */
    public static Duration parseDuration(String name, String value, ChronoUnit unit)
    {
        Matcher matcher = time.matcher(value.strip());

        if(!matcher.matches() || !matcher.group(2).isEmpty() && !units.containsKey(matcher.group(2)))
            throw new IllegalArgumentException(name + " has to be an integer followed by one of the units "
                    + "us, ms, s, min, h and d, or an integer of the unit " + units.entrySet().stream()
                            .filter(e -> e.getValue() == unit).map(Map.Entry::getKey).findFirst().orElse("?")
                    + ": " + value);

        try
        {
            return Duration.of(Long.parseLong(matcher.group(1)),
                    matcher.group(2).isEmpty() ? unit : units.get(matcher.group(2)));
        }
        catch(NumberFormatException | ArithmeticException e)
        {
            throw new IllegalArgumentException(name + " is too long a time: " + value, e);
        }
    }


    /**
     * The value of a setting given by the name of a constant of an enumeration in lower case with its words joined by
     * hyphens.
     *
     * @param <E> type of the enumeration
     * @param name name of the setting
     * @param value the text
     * @param type the enumeration
     * @return the value of the setting
     * @throws IllegalArgumentException if the text names no constant of the enumeration
     */
    public static <E extends Enum<E>> E parseEnum(String name, String value, Class<E> type)
    {
        for(E constant : type.getEnumConstants())
            if(toText(constant).equals(value.strip()))
                return constant;

        throw new IllegalArgumentException(name + " has to be one of "
                + Arrays.stream(type.getEnumConstants()).map(SettingValues::toText).collect(Collectors.joining(", "))
                + ": " + value);
    }


    /**
     * The value of a setting given by a function: its schema and its name joined by a dot, the first one of the text.
     *
     * @param name name of the setting
     * @param value the text
     * @return the value of the setting
     * @throws IllegalArgumentException if the text is no schema and name joined by a dot
     */
    public static Function parseFunction(String name, String value)
    {
        String text = value.strip();
        int dot = text.indexOf('.');

        if(dot <= 0 || dot == text.length() - 1)
            throw new IllegalArgumentException(name + " has to be a schema and a name joined by a dot: " + value);

        return new Function(text.substring(0, dot), text.substring(dot + 1));
    }


    /**
     * The text of a constant of an enumeration: its name in lower case with its words joined by hyphens.
     *
     * @param constant the constant
     * @return the text of the constant
     */
    public static String toText(Enum<?> constant)
    {
        return constant.name().toLowerCase(ROOT).replace('_', '-');
    }
}

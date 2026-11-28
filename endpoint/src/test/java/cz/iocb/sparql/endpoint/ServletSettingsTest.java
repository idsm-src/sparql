package cz.iocb.sparql.endpoint;

import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import jakarta.servlet.ServletConfig;
import org.junit.jupiter.api.Test;



/**
 * The settings of the endpoint servlet: their defaults are the values the servlet used before they could be set, a copy
 * made by a {@code with} method differs from the original in its setting alone and leaves the original as it is, a
 * value out of the range of a setting is refused by a message naming the setting, a setting is set by its name to the
 * value a text gives, and the initial parameters of the servlet set the settings they name.
 */
public class ServletSettingsTest
{
    /**
     * A setting: its name, a text giving the value of a change of it, the change, its value, the value the change gives
     * it and a change it refuses.
     */
    private record Setting(String name, String text, UnaryOperator<ServletSettings> change,
            Function<ServletSettings, Object> value, Object changed, UnaryOperator<ServletSettings> invalid)
    {
    }


    /**
     * The settings.
     */
    private static final List<Setting> settings = List.of(
            new Setting("fetch-size", "0", s -> s.withFetchSize(0), ServletSettings::fetchSize, 0,
                    s -> s.withFetchSize(-1)),
            new Setting("timeout", "10", s -> s.withTimeout(Duration.ofSeconds(10)), ServletSettings::timeout,
                    Duration.ofSeconds(10), s -> s.withTimeout(Duration.ofSeconds(-1))),
            new Setting("max-timeout", "2h", s -> s.withMaxTimeout(Duration.ofHours(2)), ServletSettings::maxTimeout,
                    Duration.ofHours(2), s -> s.withMaxTimeout(Duration.ZERO)),
            new Setting("sql-query-size-limit", "134217728", s -> s.withSqlQuerySizeLimit(134217728),
                    ServletSettings::sqlQuerySizeLimit, 134217728, s -> s.withSqlQuerySizeLimit(-1)),
            new Setting("client-watch-interval", "100", s -> s.withClientWatchInterval(Duration.ofMillis(100)),
                    ServletSettings::clientWatchInterval, Duration.ofMillis(100),
                    s -> s.withClientWatchInterval(Duration.ofNanos(999_999))),
            new Setting("client-check-rows", "10", s -> s.withClientCheckRows(10), ServletSettings::clientCheckRows, 10,
                    s -> s.withClientCheckRows(0)),
            new Setting("client-check-interval", "0", s -> s.withClientCheckInterval(Duration.ZERO),
                    ServletSettings::clientCheckInterval, Duration.ZERO,
                    s -> s.withClientCheckInterval(Duration.ofMillis(-1))));


    /**
     * The accessors of the settings.
     *
     * @return the public methods of the settings that take no parameter and give a setting
     */
    private static List<Method> getAccessors()
    {
        return Arrays.stream(ServletSettings.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getParameterCount() == 0 && m.getReturnType() != ServletSettings.class).toList();
    }


    /**
     * Creates the configuration of a servlet with the given initial parameters.
     *
     * @param parameters the names and the values of the parameters, in turn
     * @return the configuration of the servlet
     */
    private static ServletConfig config(String... parameters)
    {
        Map<String, String> values = new LinkedHashMap<>();

        for(int i = 0; i < parameters.length; i += 2)
            values.put(parameters[i], parameters[i + 1]);

        return (ServletConfig) Proxy.newProxyInstance(ServletConfig.class.getClassLoader(),
                new Class<?>[] { ServletConfig.class }, (_, method, args) -> switch(method.getName())
                {
                    case "getInitParameterNames" -> Collections.enumeration(values.keySet());
                    case "getInitParameter" -> values.get(args[0]);
                    case "getServletName" -> "sparql";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }


    @Test
    public void defaults()
    {
        ServletSettings settings = ServletSettings.DEFAULT;

        assertEquals(1000, settings.fetchSize());
        assertEquals(Duration.ofSeconds(1000), settings.timeout());
        assertEquals(Duration.ofSeconds(6000), settings.maxTimeout());
        assertEquals(0, settings.sqlQuerySizeLimit());
        assertEquals(Duration.ofMillis(500), settings.clientWatchInterval());
        assertEquals(1000, settings.clientCheckRows());
        assertEquals(Duration.ofSeconds(1), settings.clientCheckInterval());
    }


    @Test
    public void copies() throws ReflectiveOperationException
    {
        List<Method> accessors = getAccessors();

        // every setting is tested
        assertEquals(accessors.size(), settings.size());

        for(Setting setting : settings)
        {
            ServletSettings changed = setting.change().apply(ServletSettings.DEFAULT);

            assertEquals(setting.changed(), setting.value().apply(changed), setting.name());

            int differing = 0;

            for(Method accessor : accessors)
                if(!Objects.equals(accessor.invoke(changed), accessor.invoke(ServletSettings.DEFAULT)))
                    differing++;

            assertEquals(1, differing, setting.name());
        }

        // the defaults stay as they are
        defaults();
    }


    @Test
    public void ranges()
    {
        for(Setting setting : settings)
        {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> setting.invalid().apply(ServletSettings.DEFAULT), setting.name());

            assertTrue(e.getMessage().startsWith(setting.name() + " "), e.getMessage());
        }

        assertThrows(NullPointerException.class, () -> ServletSettings.DEFAULT.withTimeout(null));
        assertThrows(NullPointerException.class, () -> ServletSettings.DEFAULT.withMaxTimeout(null));
        assertThrows(NullPointerException.class, () -> ServletSettings.DEFAULT.withClientWatchInterval(null));
        assertThrows(NullPointerException.class, () -> ServletSettings.DEFAULT.withClientCheckInterval(null));

        // no time limit, and the shortest time between two reads of the socket
        assertEquals(Duration.ZERO, ServletSettings.DEFAULT.withTimeout(Duration.ZERO).timeout());
        assertEquals(Duration.ofMillis(1),
                ServletSettings.DEFAULT.withClientWatchInterval(Duration.ofMillis(1)).clientWatchInterval());
    }


    @Test
    public void texts()
    {
        // every setting is named by its accessor
        assertEquals(ServletSettings.NAMES, settings.stream().map(Setting::name).toList());
        assertEquals(Set.copyOf(ServletSettings.NAMES), getAccessors().stream()
                .map(m -> m.getName().replaceAll("([A-Z])", "-$1").toLowerCase(ROOT)).collect(Collectors.toSet()));

        for(Setting setting : settings)
        {
            ServletSettings changed = ServletSettings.DEFAULT.with(setting.name(), setting.text());

            assertEquals(setting.changed(), setting.value().apply(changed), setting.name());

            // a text giving no value of the setting is refused by a message naming the setting
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> ServletSettings.DEFAULT.with(setting.name(), "x"), setting.name());

            assertTrue(e.getMessage().startsWith(setting.name() + " "), e.getMessage());
        }

        // the time limits without a unit are in seconds, the other times in milliseconds
        assertEquals(Duration.ofSeconds(90), ServletSettings.DEFAULT.with("max-timeout", "90").maxTimeout());
        assertEquals(Duration.ofMillis(90),
                ServletSettings.DEFAULT.with("client-check-interval", "90").clientCheckInterval());
        assertEquals(Duration.ofMinutes(10), ServletSettings.DEFAULT.with("timeout", "10min").timeout());

        // an unknown name is refused too
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ServletSettings.DEFAULT.with("time-out", "10"));

        assertEquals("unknown setting: time-out", e.getMessage());
    }


    @Test
    public void parameters()
    {
        // the parameters naming settings set them, the others set nothing
        ServletSettings settings = EndpointServlet.getSettings(config("resource", "sparql:example", "fetch-size", "0",
                "timeout", "10min", "time-out", "10", "sql-query-size-limit", "134217728"));

        assertEquals(0, settings.fetchSize());
        assertEquals(Duration.ofMinutes(10), settings.timeout());
        assertEquals(134217728, settings.sqlQuerySizeLimit());
        assertEquals(ServletSettings.DEFAULT.maxTimeout(), settings.maxTimeout());
        assertEquals(ServletSettings.DEFAULT.clientCheckRows(), settings.clientCheckRows());

        // and a servlet without parameters has the default settings
        assertSame(ServletSettings.DEFAULT, EndpointServlet.getSettings(config()));

        // a parameter giving no value of its setting is refused by a message naming the setting
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> EndpointServlet.getSettings(config("resource", "sparql:example", "fetch-size", "-1")));

        assertTrue(e.getMessage().startsWith("fetch-size "), e.getMessage());
    }
}

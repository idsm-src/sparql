package cz.iocb.sparql.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
import cz.iocb.sparql.engine.request.Request;



/**
 * The settings of the engine: their defaults are the values the engine used before they could be set, a copy made by a
 * {@code with} method differs from the original in its setting alone and leaves the original as it is, a value out of
 * the range of a setting is refused by a message naming the setting, and a request keeps the settings the configuration
 * has when the request is created.
 */
public class EngineSettingsTest
{
    /**
     * A setting: its name, a change of it, its value, the value the change gives it and a change it refuses, if there
     * is one.
     */
    private record Setting(String name, UnaryOperator<EngineSettings> change, Function<EngineSettings, Object> value,
            Object changed, UnaryOperator<EngineSettings> invalid)
    {
    }


    /**
     * The settings.
     */
    private static final List<Setting> settings = List.of(
            new Setting("join-optimizer", s -> s.withJoinOptimizer(JoinOptimizerMode.ADVICE),
                    EngineSettings::joinOptimizer, JoinOptimizerMode.ADVICE, null),
            new Setting("lookup-cost", s -> s.withLookupCost(4), EngineSettings::lookupCost, 4.0,
                    s -> s.withLookupCost(0)),
            new Setting("lookup-row-cost", s -> s.withLookupRowCost(1), EngineSettings::lookupRowCost, 1.0,
                    s -> s.withLookupRowCost(-1)),
            new Setting("hash-cost", s -> s.withHashCost(3), EngineSettings::hashCost, 3.0,
                    s -> s.withHashCost(Double.NaN)),
            new Setting("comparison-cost", s -> s.withComparisonCost(0.5), EngineSettings::comparisonCost, 0.5,
                    s -> s.withComparisonCost(Double.POSITIVE_INFINITY)),
            new Setting("pairwise-margin", s -> s.withPairwiseMargin(8), EngineSettings::pairwiseMargin, 8.0,
                    s -> s.withPairwiseMargin(0)),
            new Setting("second-join-margin", s -> s.withSecondJoinMargin(3), EngineSettings::secondJoinMargin, 3.0,
                    s -> s.withSecondJoinMargin(0)),
            new Setting("memoize-ratio", s -> s.withMemoizeRatio(4), EngineSettings::memoizeRatio, 4.0,
                    s -> s.withMemoizeRatio(0)),
            new Setting("exhaustive-join-limit", s -> s.withExhaustiveJoinLimit(10),
                    EngineSettings::exhaustiveJoinLimit, 10, s -> s.withExhaustiveJoinLimit(31)),
            new Setting("counted-join-ratio", s -> s.withCountedJoinRatio(2), EngineSettings::countedJoinRatio, 2.0,
                    s -> s.withCountedJoinRatio(0)),
            new Setting("default-rows", s -> s.withDefaultRows(100), EngineSettings::defaultRows, 100.0,
                    s -> s.withDefaultRows(0)),
            new Setting("equality-selectivity", s -> s.withEqualitySelectivity(0.01),
                    EngineSettings::equalitySelectivity, 0.01, s -> s.withEqualitySelectivity(0)),
            new Setting("inequality-selectivity", s -> s.withInequalitySelectivity(0.25),
                    EngineSettings::inequalitySelectivity, 0.25, s -> s.withInequalitySelectivity(1.5)),
            new Setting("default-selectivity", s -> s.withDefaultSelectivity(1), EngineSettings::defaultSelectivity,
                    1.0, s -> s.withDefaultSelectivity(-0.5)),
            new Setting("disjunct-limit", s -> s.withDisjunctLimit(10), EngineSettings::disjunctLimit, 10,
                    s -> s.withDisjunctLimit(0)),
            new Setting("box-comparison-limit", s -> s.withBoxComparisonLimit(16), EngineSettings::boxComparisonLimit,
                    16, s -> s.withBoxComparisonLimit(0)),
            new Setting("triple-term-class-limit", s -> s.withTripleTermClassLimit(1),
                    EngineSettings::tripleTermClassLimit, 1, s -> s.withTripleTermClassLimit(0)),
            new Setting("exists-round-limit", s -> s.withExistsRoundLimit(2), EngineSettings::existsRoundLimit, 2,
                    s -> s.withExistsRoundLimit(0)),
            new Setting("statistics-function",
                    s -> s.withStatisticsFunction(new cz.iocb.sparql.engine.database.Function("s", "f")),
                    EngineSettings::statisticsFunction, new cz.iocb.sparql.engine.database.Function("s", "f"), null),
            new Setting("statistics-check-interval", s -> s.withStatisticsCheckInterval(Duration.ofSeconds(5)),
                    EngineSettings::statisticsCheckInterval, Duration.ofSeconds(5),
                    s -> s.withStatisticsCheckInterval(Duration.ofSeconds(-1))),
            new Setting("statistics-cache-limit", s -> s.withStatisticsCacheLimit(10),
                    EngineSettings::statisticsCacheLimit, 10, s -> s.withStatisticsCacheLimit(0)),
            new Setting("service-reorder", s -> s.withServiceReorder(true), EngineSettings::serviceReorder, true, null),
            new Setting("service-table-rows", s -> s.withServiceTableRows(0), EngineSettings::serviceTableRows, 0,
                    s -> s.withServiceTableRows(-1)),
            new Setting("service-batch-size", s -> s.withServiceBatchSize(10), EngineSettings::serviceBatchSize, 10,
                    s -> s.withServiceBatchSize(0)));


    /**
     * The accessors of the settings.
     *
     * @return the public methods of the settings that take no parameter and give a setting, not one derived from them
     */
    private static List<Method> getAccessors()
    {
        return Arrays.stream(EngineSettings.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getParameterCount() == 0 && m.getReturnType() != EngineSettings.class)
                .filter(m -> !m.getName().startsWith("is")).toList();
    }


    @Test
    public void defaults()
    {
        EngineSettings settings = EngineSettings.DEFAULT;

        assertEquals(JoinOptimizerMode.OFF, settings.joinOptimizer());
        assertEquals(8, settings.lookupCost());
        assertEquals(2, settings.lookupRowCost());
        assertEquals(2.5, settings.hashCost());
        assertEquals(0.3, settings.comparisonCost());
        assertEquals(16, settings.pairwiseMargin());
        assertEquals(2, settings.secondJoinMargin());
        assertEquals(2, settings.memoizeRatio());
        assertEquals(15, settings.exhaustiveJoinLimit());
        assertEquals(4, settings.countedJoinRatio());
        assertEquals(1000, settings.defaultRows());
        assertEquals(0.005, settings.equalitySelectivity());
        assertEquals(1.0 / 3, settings.inequalitySelectivity());
        assertEquals(0.5, settings.defaultSelectivity());
        assertEquals(30, settings.disjunctLimit());
        assertEquals(256, settings.boxComparisonLimit());
        assertEquals(256, settings.tripleTermClassLimit());
        assertEquals(8, settings.existsRoundLimit());
        assertNull(settings.statisticsFunction());
        assertEquals(Duration.ofMinutes(1), settings.statisticsCheckInterval());
        assertEquals(100_000, settings.statisticsCacheLimit());
        assertFalse(settings.serviceReorder());
        assertEquals(1000, settings.serviceTableRows());
        assertEquals(1000, settings.serviceBatchSize());
    }


    @Test
    public void copies() throws ReflectiveOperationException
    {
        List<Method> accessors = getAccessors();

        // every setting is tested
        assertEquals(accessors.size(), settings.size());

        for(Setting setting : settings)
        {
            EngineSettings changed = setting.change().apply(EngineSettings.DEFAULT);

            assertEquals(setting.changed(), setting.value().apply(changed), setting.name());

            int differing = 0;

            for(Method accessor : accessors)
                if(!Objects.equals(accessor.invoke(changed), accessor.invoke(EngineSettings.DEFAULT)))
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
            if(setting.invalid() == null)
                continue;

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> setting.invalid().apply(EngineSettings.DEFAULT), setting.name());

            assertTrue(e.getMessage().startsWith(setting.name() + " "), e.getMessage());
        }

        assertThrows(NullPointerException.class, () -> EngineSettings.DEFAULT.withJoinOptimizer(null));
        assertThrows(NullPointerException.class, () -> EngineSettings.DEFAULT.withStatisticsCheckInterval(null));

        // the times of the checks of the statistics
        assertEquals(Duration.ZERO,
                EngineSettings.DEFAULT.withStatisticsCheckInterval(Duration.ZERO).statisticsCheckInterval());
        assertThrows(IllegalArgumentException.class,
                () -> EngineSettings.DEFAULT.withStatisticsCheckInterval(Duration.ofDays(365 * 300)));

        // the limits of the join trees tried
        assertEquals(1, EngineSettings.DEFAULT.withExhaustiveJoinLimit(1).exhaustiveJoinLimit());
        assertEquals(30, EngineSettings.DEFAULT.withExhaustiveJoinLimit(30).exhaustiveJoinLimit());
        assertThrows(IllegalArgumentException.class, () -> EngineSettings.DEFAULT.withExhaustiveJoinLimit(0));
    }


    @Test
    public void joinOptimizer()
    {
        assertFalse(EngineSettings.DEFAULT.isJoinOptimizerEnabled());
        assertFalse(EngineSettings.DEFAULT.isPlanAdviceEnabled());

        EngineSettings settings = EngineSettings.DEFAULT.withJoinOptimizer(JoinOptimizerMode.SETTINGS);
        assertTrue(settings.isJoinOptimizerEnabled());
        assertFalse(settings.isPlanAdviceEnabled());

        EngineSettings advice = EngineSettings.DEFAULT.withJoinOptimizer(JoinOptimizerMode.ADVICE);
        assertTrue(advice.isJoinOptimizerEnabled());
        assertTrue(advice.isPlanAdviceEnabled());
    }


    @Test
    public void configuration() throws Exception
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, null, null, false);

        assertSame(EngineSettings.DEFAULT, config.getEngineSettings());

        EngineSettings changed = EngineSettings.DEFAULT.withLookupCost(4);
        config.setEngineSettings(changed);

        assertSame(changed, config.getEngineSettings());
        assertThrows(NullPointerException.class, () -> config.setEngineSettings(null));

        // a request keeps the settings it has begun with
        try(Request request = new Request(config))
        {
            config.setEngineSettings(EngineSettings.DEFAULT);

            assertSame(changed, request.getEngineSettings());
        }

        // and evaluates the SERVICE calls independently of their context as the settings say, unless told otherwise
        config.setEngineSettings(EngineSettings.DEFAULT.withServiceReorder(true));

        try(Request request = new Request(config); Request told = new Request(config, false))
        {
            assertTrue(request.isServiceReorderEnabled());
            assertFalse(told.isServiceReorderEnabled());
        }
    }
}

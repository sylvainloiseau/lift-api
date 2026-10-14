package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Measures what loading the corpus costs: time and retained heap.
 *
 * Not a pass/fail check, which would be flaky: it prints figures to compare before and
 * after a change of the model (the plan asks for one before the observable state and one
 * after the change events). Excluded from the default build by its tag; run it with
 * {@code mvn test -Dgroups=perf -DexcludedTestGroups=none -Dtest=LoadingCostTest}.
 */
@Tag("perf")
class LoadingCostTest {

    private static final File CORPUS = new File("src/test/resources/lift/20240828Lift.lift");
    private static final int WARM_UP = 10;
    private static final int RUNS = 25;

    @Test
    void loadingTheCorpus() throws Exception {
        Logger.getLogger(LiftDictionary.class.getName()).setLevel(Level.WARNING);
        for (int i = 0; i < WARM_UP; i++) {
            LiftDictionary.loadDictionaryFromFile(CORPUS);
        }
        long[] times = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            LiftDictionary.loadDictionaryFromFile(CORPUS);
            times[i] = System.nanoTime() - start;
        }
        Arrays.sort(times);

        long before = usedHeapAfterGc();
        LiftDictionary kept = LiftDictionary.loadDictionaryFromFile(CORPUS);
        long after = usedHeapAfterGc();

        System.out.printf(
            "[perf] loading %s: median %d ms (min %d, max %d) over %d runs; retained heap %.1f MB for %d entries%n",
            CORPUS.getName(),
            times[RUNS / 2] / 1_000_000,
            times[0] / 1_000_000,
            times[RUNS - 1] / 1_000_000,
            RUNS,
            (after - before) / (1024.0 * 1024.0),
            kept.getLiftDictionaryRegistry().getEntries().size()
        );
        assertTrue(kept.getLiftDictionaryRegistry().getEntries().size() > 0);
    }

    private static long usedHeapAfterGc() throws InterruptedException {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 4; i++) {
            System.gc();
            Thread.sleep(50);
        }
        return rt.totalMemory() - rt.freeMemory();
    }
}

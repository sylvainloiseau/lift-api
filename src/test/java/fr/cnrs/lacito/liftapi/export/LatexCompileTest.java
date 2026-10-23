package fr.cnrs.lacito.liftapi.export;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.Utils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Compiling to PDF: only when XeLaTeX or LuaLaTeX is installed. */
class LatexCompileTest {

    private static LatexEngine engine(LatexEngineRequest request) {
        Optional<LatexEngine> e = LatexEngine.find(request);
        assumeTrue(e.isPresent(), "no " + request + " engine on this machine");
        return e.get();
    }

    private static void compileTheSample(Path dir, LatexEngine engine) throws Exception {
        Path pictures = Files.createDirectories(dir.resolve("pictures"));
        Files.copy(Utils.resourceFile("lift/pictures/P1230164.JPG").toPath(), pictures.resolve("P1230164.JPG"));
        LiftDictionary d = Utils.loadDictionaryForTest(LatexExportTest.SAMPLE);
        Path tex = dir.resolve("sample.tex");
        LatexExport.export(d, tex, LatexExportTest.sampleOptions().withIllustrationDirectory(pictures)
            .withListOfFigures(true), EntrySelection.all());
        LatexCompilation c = LatexExport.compile(tex, engine, Duration.ofMinutes(5));
        assertTrue(c.success(), c.firstError().orElse("?"));
        assertTrue(Files.size(c.pdf()) > 1000);
        assertTrue(c.runs() >= 2);
        assertFalse(Files.readString(c.log()).contains("undefined references"), "the references resolve");
    }

    @Test
    void theSampleCompilesWithXelatex(@TempDir Path dir) throws Exception {
        compileTheSample(dir, engine(LatexEngineRequest.of(LatexEngineKind.XELATEX)));
    }

    @Test
    void theSampleCompilesWithLualatex(@TempDir Path dir) throws Exception {
        compileTheSample(dir, engine(LatexEngineRequest.of(LatexEngineKind.LUALATEX)));
    }

    @Test
    void anErrorGivesItsLine(@TempDir Path dir) throws Exception {
        LatexEngine e = engine(LatexEngineRequest.auto());
        Path tex = dir.resolve("bad.tex");
        Files.writeString(tex, "\\documentclass{article}\n\\begin{document}\nok\n\\undefinedcommand\n\\end{document}\n");
        LatexCompilation c = LatexExport.compile(tex, e, Duration.ofMinutes(2));
        assertFalse(c.success());
        assertEquals(1, c.runs());
        assertEquals(4, c.firstErrorLine().orElseThrow());
        assertTrue(c.firstError().orElseThrow().contains("Undefined control sequence"), c.firstError().orElseThrow());
    }

    @Test
    void theTimeoutStopsTheEngine(@TempDir Path dir) throws Exception {
        LatexEngine e = engine(LatexEngineRequest.auto());
        Path tex = dir.resolve("loop.tex");
        Files.writeString(tex, "\\documentclass{article}\n\\begin{document}\n\\def\\x{\\x}\\x\n\\end{document}\n");
        LatexExportException ex = assertThrows(LatexExportException.class,
            () -> LatexExport.compile(tex, e, Duration.ofSeconds(3)));
        assertTrue(ex.getMessage().contains("did not finish within 3 s"), ex.getMessage());
    }
}

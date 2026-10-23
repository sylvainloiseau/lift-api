package fr.cnrs.lacito.liftapi.export;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/** Finding an engine, over folders holding fake xelatex and lualatex scripts: no TeX needed. */
@DisabledOnOs(OS.WINDOWS)
class LatexEngineTest {

    private static Path fake(Path dir, String name, String version) throws Exception {
        Files.createDirectories(dir);
        Path p = dir.resolve(name);
        Files.writeString(p, "#!/bin/sh\necho '" + version + "'\n");
        Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwxr-xr-x"));
        return p;
    }

    @Test
    void autoPrefersXelatex(@TempDir Path dir) throws Exception {
        Path lua = fake(dir.resolve("a"), "lualatex", "LuaHBTeX 1.24");
        Path xe = fake(dir.resolve("b"), "xelatex", "XeTeX 3.14");
        List<Path> dirs = List.of(dir.resolve("a"), dir.resolve("b"));
        LatexEngine e = LatexEngine.require(LatexEngineRequest.auto(), dirs);
        assertEquals(LatexEngineKind.XELATEX, e.kind(), "XeLaTeX first, even found later on the path");
        assertEquals(xe, e.path());
        assertEquals("XeTeX 3.14", e.version());
        assertEquals(List.of(LatexEngineKind.XELATEX, LatexEngineKind.LUALATEX),
            LatexEngine.findAll(dirs).stream().map(LatexEngine::kind).toList());
        assertEquals(lua, LatexEngine.require(LatexEngineRequest.of(LatexEngineKind.LUALATEX), dirs).path());
    }

    @Test
    void autoTakesLualatexWhenItIsTheOnlyOne(@TempDir Path dir) throws Exception {
        fake(dir, "lualatex", "LuaHBTeX 1.24");
        assertEquals(LatexEngineKind.LUALATEX, LatexEngine.require(LatexEngineRequest.auto(), List.of(dir)).kind());
    }

    @Test
    void aMissingKindIsAnErrorThatNamesTheOtherOne(@TempDir Path dir) throws Exception {
        Path xe = fake(dir, "xelatex", "XeTeX 3.14");
        LatexExportException e = assertThrows(LatexExportException.class,
            () -> LatexEngine.require(LatexEngineRequest.of(LatexEngineKind.LUALATEX), List.of(dir)));
        assertTrue(e.getMessage().startsWith("LuaLaTeX not found; XeLaTeX is installed at " + xe), e.getMessage());
        assertTrue(LatexEngine.find(LatexEngineRequest.of(LatexEngineKind.LUALATEX), List.of(dir)).isEmpty());

        LatexExportException none = assertThrows(LatexExportException.class,
            () -> LatexEngine.require(LatexEngineRequest.auto(), List.of(dir.resolve("empty"))));
        assertTrue(none.getMessage().startsWith("Neither XeLaTeX nor LuaLaTeX was found"));
    }

    @Test
    void aProgramsKindIsGuessedFromItsNameOrGiven(@TempDir Path dir) throws Exception {
        Path xe = fake(dir, "xelatex-dev", "XeTeX");
        Path other = fake(dir, "mytex", "XeTeX");
        assertEquals(LatexEngineKind.XELATEX, LatexEngine.require(LatexEngineRequest.at(xe, null), List.of()).kind());
        assertThrows(LatexExportException.class, () -> LatexEngine.require(LatexEngineRequest.at(other, null), List.of()));
        assertEquals(LatexEngineKind.LUALATEX,
            LatexEngine.require(LatexEngineRequest.at(other, LatexEngineKind.LUALATEX), List.of()).kind());
        assertThrows(LatexExportException.class,
            () -> LatexEngine.require(LatexEngineRequest.at(dir.resolve("none"), LatexEngineKind.XELATEX), List.of()));
    }

    @Test
    void requestsAreParsedAndSaved() {
        assertTrue(LatexEngineRequest.parse("AUTO").isAuto());
        assertTrue(LatexEngineRequest.parse(null).isAuto());
        assertEquals(LatexEngineRequest.of(LatexEngineKind.LUALATEX), LatexEngineRequest.parse("LuaLaTeX"));
        assertEquals("xelatex", LatexEngineRequest.parse("xelatex").settingsValue().orElseThrow());
        assertTrue(LatexEngineRequest.parse("/opt/tex/xelatex").path().isPresent());
        assertTrue(LatexEngineRequest.parse("/opt/tex/xelatex").settingsValue().isEmpty(), "a path is not saved");
    }

    @Test
    void theOpenCommandOfEachPlatform() {
        Path pdf = Path.of("/tmp/my dict.pdf");
        assertEquals(List.of("open", pdf.toString()), LatexExport.openCommand("Mac OS X", pdf));
        assertEquals(List.of("xdg-open", pdf.toString()), LatexExport.openCommand("Linux", pdf));
        assertEquals(List.of("xdg-open", pdf.toString()), LatexExport.openCommand("FreeBSD", pdf));
        assertEquals(List.of("rundll32", "url.dll,FileProtocolHandler", pdf.toString()),
            LatexExport.openCommand("Windows 11", pdf));
    }

    @Test
    void theFirstErrorOfALog() {
        String log = """
            This is XeTeX
            (./d.tex
            ./d.tex:42: Undefined control sequence.
            l.42 \\foo
                      bar
            more
            """;
        String[] e = LatexExport.firstError(log);
        assertEquals("./d.tex:42: Undefined control sequence.\nl.42 \\foo\n          bar", e[0]);
        assertEquals("42", e[1]);
        String[] bang = LatexExport.firstError("x\n! Missing $ inserted.\n<inserted text>\nl.7 a_b\n");
        assertEquals("7", bang[1]);
        assertNull(LatexExport.firstError("no error here\n"));
    }
}

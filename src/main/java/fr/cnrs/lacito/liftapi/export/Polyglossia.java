package fr.cnrs.lacito.liftapi.export;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * The languages of polyglossia, and the polyglossia name of a language code.
 *
 * <p>The table ({@code polyglossia-languages.txt}) was made from the {@code gloss-*.ldf}
 * files of polyglossia v2.9 (TeX Live 2026): each language, the BCP 47 language subtags it
 * declares, and the other names polyglossia accepts ({@code british}, {@code american}...).
 */
public final class Polyglossia {

    private static final List<String> NAMES;
    private static final Map<String, String> BY_CODE;

    static {
        TreeSet<String> names = new TreeSet<>();
        Map<String, String> byCode = new HashMap<>();
        try (InputStream in = Polyglossia.class.getResourceAsStream("polyglossia-languages.txt")) {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] f = line.split("\t", -1);
                names.add(f[1]);
                if (f[0].equals("language") && f.length > 2 && !f[2].isEmpty()) {
                    for (String code : f[2].split(",")) byCode.putIfAbsent(code, f[1]);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        NAMES = List.copyOf(names);
        BY_CODE = Map.copyOf(byCode);
    }

    private Polyglossia() {}

    /** The names polyglossia accepts for \setotherlanguage, sorted. */
    public static List<String> names() {
        return NAMES;
    }

    public static boolean isKnown(String name) {
        return name != null && NAMES.contains(name.trim());
    }

    /**
     * The polyglossia name of a language code ({@code fr}, {@code fr-FR}, {@code fra},
     * {@code oc-FR-fonipa}...), from its primary subtag; empty when polyglossia has none
     * for it. A three-letter code is also matched to its two-letter form ({@code fra} →
     * {@code fr}).
     */
    public static Optional<String> nameFor(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        String primary = code.trim().split("[-_]", 2)[0].toLowerCase(Locale.ROOT);
        String name = BY_CODE.get(primary);
        if (name == null && primary.length() == 3) {
            for (Map.Entry<String, String> e : BY_CODE.entrySet()) {
                if (e.getKey().length() == 2 && iso3(e.getKey()).equals(primary)) {
                    name = e.getValue();
                    break;
                }
            }
        }
        return Optional.ofNullable(name);
    }

    private static String iso3(String twoLetters) {
        try {
            return Locale.of(twoLetters).getISO3Language();
        } catch (java.util.MissingResourceException e) {
            return "";
        }
    }

    /** The codes the table maps, for tests. */
    static List<String> codes() {
        return new ArrayList<>(BY_CODE.keySet());
    }
}

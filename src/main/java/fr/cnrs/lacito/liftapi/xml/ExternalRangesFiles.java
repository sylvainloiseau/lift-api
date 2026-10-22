package fr.cnrs.lacito.liftapi.xml;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The {@code .lift-ranges} files the header of a {@code .lift} file refers to: how an
 * {@code href} is resolved to a file, and the files already read.
 *
 * Each file is read once per load, whatever the number of ranges referring to it and
 * however their {@code href} spells it: the cache is keyed by the resolved real path.
 * A failure is kept too, so that it is reported once. The reader clears the cache at the
 * end of the header ({@link #clear()}).
 *
 * <h2>Resolving an {@code href}</h2>
 *
 * The {@code href} comes from a file written on another machine. In order:
 * <ol>
 * <li>if it starts with a URI scheme (two letters or more: a single letter followed by
 * {@code :} is a Windows drive), it must be {@code file:} - no other scheme is read, there
 * is no network access. {@code file:///abs/path} is that path; {@code file://name}, whose
 * file name is the URI's authority, is resolved against the directory of the {@code .lift}
 * file. Otherwise the {@code href} is a path, relative to that directory or absolute;</li>
 * <li>if that file does not exist, the file of the same name in the directory of the
 * {@code .lift} file: FLEx writes the absolute path of the exporting machine, while the
 * ranges file travels next to the {@code .lift} file;</li>
 * <li>otherwise the file is missing.</li>
 * </ol>
 */
final class ExternalRangesFiles {

    /** A URI scheme: two characters or more, so that a Windows drive is not one. */
    private static final Pattern URI_SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]+:");

    /**
     * What a {@code <range href>} found.
     *
     * @param file the resolved ranges file, or {@code null} if none was found
     * @param range the range of that id in the file, or {@code null} if it has none
     * @param problem why {@code range} is {@code null}, or {@code null}
     */
    record Lookup(Path file, RecordedElement range, String problem) {}

    private final Path baseDirectory;

    /** Real path -&gt; its ranges by id, or the {@code IOException} its reading threw. */
    private final Map<Path, Object> filesRead = new HashMap<>();

    /**
     * @param baseDirectory the directory of the {@code .lift} file, against which relative
     *        references are resolved
     */
    ExternalRangesFiles(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    /**
     * Finds the range {@code rangeId} in the ranges file {@code href} names, reading the
     * file if it was not read yet.
     *
     * @throws IllegalArgumentException if the {@code href} has a scheme other than
     *         {@code file:}, or the file is not a valid ranges file
     */
    Lookup find(String href, String rangeId) {
        Optional<Path> file = resolve(href, baseDirectory);
        if (file.isEmpty()) {
            return new Lookup(null, null, "the ranges file " + href + " was not found");
        }
        Object ranges = filesRead.computeIfAbsent(file.get(), f -> {
            try {
                return LiftRangesFileReader.read(f);
            } catch (IOException e) {
                return e;
            }
        });
        if (ranges instanceof IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        @SuppressWarnings("unchecked")
        RecordedElement range = ((Map<String, RecordedElement>) ranges).get(rangeId);
        return range == null
            ? new Lookup(file.get(), null, "the ranges file " + href + " has no range " + rangeId)
            : new Lookup(file.get(), range, null);
    }

    /** Forgets the files read: at the end of the header. */
    void clear() {
        filesRead.clear();
    }

    /**
     * Resolves an {@code href} to an existing file (see the class documentation).
     *
     * @return the real path of the file, or empty if none exists
     * @throws IllegalArgumentException if the {@code href} has a scheme other than {@code file:}
     */
    static Optional<Path> resolve(String href, Path baseDirectory) {
        return resolve(href, baseDirectory, true);
    }

    /**
     * @param byNameNextToTheLiftFile whether to fall back on the file of the same name in
     *        {@code baseDirectory}
     */
    private static Optional<Path> resolve(String href, Path baseDirectory, boolean byNameNextToTheLiftFile) {
        String h = href.trim();
        boolean isUri = URI_SCHEME.matcher(h).lookingAt();
        if (isUri && !h.regionMatches(true, 0, "file:", 0, 5)) {
            throw new IllegalArgumentException(
                "Only file references are read; the ranges file " + href + " is not one.");
        }
        List<Path> candidates = new ArrayList<>(2);
        if (isUri) {
            candidates.add(pathOfFileUri(h, baseDirectory));
        } else {
            candidates.add(resolveOrNull(baseDirectory, h));
        }
        String name = byNameNextToTheLiftFile ? lastSegment(h, isUri) : "";
        if (!name.isEmpty()) {
            candidates.add(resolveOrNull(baseDirectory, name));
        }
        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                try {
                    return Optional.of(candidate.toRealPath());
                } catch (IOException e) {
                    // Unreadable: try the next one.
                }
            }
        }
        return Optional.empty();
    }

    /**
     * The {@code href} to write for a ranges file when the dictionary is saved in
     * {@code newBaseDirectory}: the one read, if it still names the same file from there;
     * otherwise a relative path to the file, or its {@code file:} URI if there is none (two
     * Windows drives). Nothing is copied: the reference follows the file.
     *
     * @param href the {@code href} read
     * @param file the file it was resolved to on load, or {@code null} if none was found
     *        (then {@code href} is written as it was)
     */
    static String hrefForSaving(String href, Path file, Path newBaseDirectory) {
        if (file == null) {
            return href;
        }
        try {
            // Without the fallback on the name: the reference must name the file by itself.
            if (resolve(href, newBaseDirectory, false).filter(file::equals).isPresent()) {
                return href;
            }
        } catch (IllegalArgumentException e) {
            // Not a file reference: replaced below.
        }
        Path base;
        try {
            base = newBaseDirectory.toRealPath();
        } catch (IOException e) {
            base = newBaseDirectory.toAbsolutePath().normalize();
        }
        try {
            return base.relativize(file).toString().replace(File.separatorChar, '/');
        } catch (IllegalArgumentException e) {
            return file.toUri().toString();
        }
    }

    private static Path pathOfFileUri(String href, Path baseDirectory) {
        try {
            URI uri = new URI(href.replace('\\', '/').replace(" ", "%20"));
            String authority = uri.getAuthority();
            String path = uri.getPath();
            if (authority != null && !authority.isEmpty() && !authority.equalsIgnoreCase("localhost")) {
                // file://name: the name parsed as the host.
                return baseDirectory.resolve(authority + (path == null ? "" : path));
            }
            if (path == null) {
                // file:name, an opaque URI.
                return resolveOrNull(baseDirectory, uri.getSchemeSpecificPart());
            }
            return resolveOrNull(baseDirectory, path);
        } catch (URISyntaxException | InvalidPathException e) {
            return null;
        }
    }

    private static Path resolveOrNull(Path baseDirectory, String path) {
        try {
            return baseDirectory.resolve(path);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /** The file name an {@code href} ends with, decoded if it is a URI. */
    private static String lastSegment(String href, boolean isUri) {
        String h = href.replace('\\', '/');
        int query = isUri ? indexOfAny(h, '?', '#') : -1;
        if (query >= 0) {
            h = h.substring(0, query);
        }
        while (h.endsWith("/")) {
            h = h.substring(0, h.length() - 1);
        }
        String segment = h.substring(h.lastIndexOf('/') + 1);
        if (isUri) {
            if (segment.regionMatches(true, 0, "file:", 0, 5)) {
                segment = segment.substring(5);
            }
            segment = URLDecoder.decode(segment.replace("+", "%2B"), StandardCharsets.UTF_8);
        }
        return segment;
    }

    private static int indexOfAny(String s, char a, char b) {
        int i = s.indexOf(a);
        int j = s.indexOf(b);
        return i < 0 ? j : j < 0 ? i : Math.min(i, j);
    }
}

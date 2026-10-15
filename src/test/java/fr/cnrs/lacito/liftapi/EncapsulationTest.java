package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fr.cnrs.lacito.liftapi.builder.AbstractLiftElementBuilder;
import fr.cnrs.lacito.liftapi.builder.DictionaryComponentBuilderFactory;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SetProperty;

/**
 * One way to change each thing: a consumer cannot change the model behind the registry's
 * back through a collection a getter hands out.
 *
 * By reflection, on one populated object of each kind: every public, parameterless
 * method returning a {@code Collection} or a {@code Map} returns one that refuses to be
 * modified, and no public method returns a writable list, map or set property (except
 * the typed values of a trait, which are data a UI edits, and are reported).
 */
class EncapsulationTest {

    /** Methods that return a fresh copy, which the caller may do what it likes with. */
    private static final Set<String> COPIES = Set.of("walkTextSpanTree");

    @Test
    void collectionsCannotBeModifiedFromOutside() throws Exception {
        List<String> writable = new ArrayList<>();
        for (Object o : samples()) {
            for (Method m : o.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 0
                    || m.getDeclaringClass() == Object.class || COPIES.contains(m.getName())
                    || m.getName().endsWith("ValueProperty")) {
                    continue;
                }
                Class<?> type = m.getReturnType();
                if (!Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type)) {
                    continue;
                }
                Object result = m.invoke(o);
                if (result == null || isUnmodifiable(result)) {
                    continue;
                }
                writable.add(o.getClass().getSimpleName() + "." + m.getName() + "()");
            }
        }
        assertTrue(writable.isEmpty(), "Getters returning a modifiable collection: " + writable);
    }

    /**
     * The XML reader reaches the element under construction through
     * {@code internal.BuilderInternals}; an application must not, or it could wire the
     * element by hand behind the builder's back.
     */
    @Test
    void noBuilderHandsOutItsElementBeforeBuild() {
        List<String> leaks = new ArrayList<>();
        for (Method factoryMethod : DictionaryComponentBuilderFactory.class.getMethods()) {
            Class<?> builder = factoryMethod.getReturnType();
            if (!AbstractLiftElementBuilder.class.isAssignableFrom(builder)) {
                continue;
            }
            for (Method m : builder.getMethods()) {
                if (!m.getName().equals("build") && AbstractLiftRoot.class.isAssignableFrom(m.getReturnType())) {
                    leaks.add(builder.getSimpleName() + "." + m.getName() + "()");
                }
            }
        }
        assertTrue(leaks.isEmpty(), "Builder methods handing out the element: " + leaks);
    }

    @Test
    void noWritableCollectionPropertyIsHandedOut() {
        List<String> writable = new ArrayList<>();
        for (Object o : samples()) {
            for (Method m : o.getClass().getMethods()) {
                Class<?> type = m.getReturnType();
                boolean collectionProperty = ListProperty.class.isAssignableFrom(type)
                    || MapProperty.class.isAssignableFrom(type)
                    || SetProperty.class.isAssignableFrom(type);
                if (collectionProperty && !m.getName().endsWith("ValueProperty")) {
                    writable.add(o.getClass().getSimpleName() + "." + m.getName() + "()");
                }
            }
        }
        assertTrue(writable.isEmpty(), "Methods returning a writable collection property: " + writable);
    }

    /**
     * Whether adding to it is refused. Not {@code clear()}: on an empty unmodifiable
     * JavaFX list it does nothing, without throwing.
     */
    @SuppressWarnings("unchecked")
    private static boolean isUnmodifiable(Object result) {
        try {
            if (result instanceof Collection<?> c) {
                ((Collection<Object>) c).add(null);
            } else {
                ((Map<Object, Object>) result).put(null, null);
            }
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        } catch (RuntimeException e) {
            // Refused null, but would have taken something else.
            return false;
        }
    }

    /** One populated object of each kind, from the corpus. */
    private static List<Object> samples() {
        LiftDictionary d;
        try {
            d = LiftDictionary.loadDictionaryFromFile(new File("src/test/resources/lift/20240828Lift.lift"));
        } catch (LiftDocumentLoadingException e) {
            throw new AssertionError(e);
        }
        LiftDictionaryRegistry reg = d.getLiftDictionaryRegistry();
        Map<Class<?>, Object> byClass = new LinkedHashMap<>();
        List<List<?>> all = List.of(reg.getEntries(), reg.getSenses(), reg.getExamples(),
            reg.getVariants(), reg.getTraits(), reg.getReversals(), reg.getRelations(),
            reg.getPronunciations(), reg.getNotes(), reg.getTranslations(), reg.getMedias(),
            reg.getIllustrations(), reg.getFields(), reg.getEtymologies(),
            reg.getGrammaticalInfos(), reg.getAnnotations());
        for (List<?> l : all) {
            for (Object o : l) {
                byClass.putIfAbsent(o.getClass(), o);
            }
        }
        LiftEntry e = reg.getEntries().stream().filter(x -> !x.getSenses().isEmpty()).findFirst().orElseThrow();
        LiftSense s = e.getSenses().getFirst();
        List<Object> samples = new ArrayList<>(byClass.values());
        samples.add(d.getHeader());
        samples.add(d.getHeader().getNoteTypeManager());
        samples.add(d.getHeader().getFeatureSets().stream()
            .flatMap(fs -> fs.getFeatures().values().stream()).findFirst().orElseThrow());
        samples.add(d.getHeader().getFieldsAndTraitsDefinitions().getFirst());
        samples.add(s.getGlosses());
        samples.add(s.getGlosses().getForms().iterator().next());
        samples.add(d.getObjectLanguageManager());
        samples.add(reg);
        return samples;
    }
}

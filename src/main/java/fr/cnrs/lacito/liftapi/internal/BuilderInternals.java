package fr.cnrs.lacito.liftapi.internal;

import fr.cnrs.lacito.liftapi.builder.AbstractLiftElementBuilder;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;

/**
 * What the XML reader needs from the builders and applications must not see.
 *
 * The reader creates every component through the builders, but it drives them from
 * SAX events rather than from nested lambdas: it keeps the builders of the open
 * elements on a stack, creates the builder of a child on the element of its parent's
 * builder before that one is built, and fills the texts of the element as the forms
 * arrive. That needs the element under construction, which a builder keeps to itself
 * so that an application cannot wire it by hand behind the builder's back.
 *
 * The same pattern as {@link ModelInternals}: the {@code builder} package implements
 * {@link Access} and installs it from a static initializer; this package is not
 * exported, so consumers cannot reach it.
 *
 * <b>Not public API.</b>
 */
public final class BuilderInternals {

    /**
     * The builders' operations reserved to the XML reader, implemented in
     * {@code builder}.
     */
    public interface Access {

        /**
         * The element under construction, before {@code build()} - for creating the
         * builders of its children on it, and filling its texts.
         */
        <T extends AbstractLiftRoot> T element(AbstractLiftElementBuilder<T, ?> builder);
    }

    private static volatile Access access;

    private BuilderInternals() {}

    /**
     * Called once, by the builders, when their classes are initialized.
     */
    public static void install(Access a) {
        if (access != null) {
            throw new IllegalStateException("The builder access is already installed.");
        }
        access = a;
    }

    /**
     * The access to the builders' reserved operations.
     */
    public static Access get() {
        Access a = access;
        if (a == null) {
            // The builders install it from a static initializer: make sure it has run.
            try {
                Class.forName(
                    AbstractLiftElementBuilder.class.getName(),
                    true,
                    AbstractLiftElementBuilder.class.getClassLoader()
                );
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
            a = access;
        }
        return a;
    }
}

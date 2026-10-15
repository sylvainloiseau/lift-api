/**
 * The fluent API that creates the components of a dictionary, reached through
 * {@code dictionary.getComponentBuilder()} ({@link
 * fr.cnrs.lacito.liftapi.builder.DictionaryComponentBuilderFactory}).
 *
 * The builders are the only code that creates components: the XML reader goes through
 * them too. So they accept what the LIFT schema accepts, and enforce what it requires
 * (the type of a relation, the source of an etymology, the {@code href} of a media)
 * and nothing more. A sense without gloss, an example without text or an entry without
 * lexical unit can be built, since files contain them. Editorial rules of that kind
 * belong to the application.
 *
 * Likewise, the builders accept what a file may contain before the declaration it
 * refers to: a type or a feature not yet in the header is added to it, and a reference
 * to an entry or a sense that does not exist yet is recorded and resolved later (see
 * {@link fr.cnrs.lacito.liftapi.LiftDictionary#resolveReferences()}).
 *
 * Building into an attached parent registers the component at once; building into a
 * component that is itself still being built registers nothing until the outermost
 * component is attached (see the {@code model} package documentation).
 */
package fr.cnrs.lacito.liftapi.builder;

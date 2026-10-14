package fr.cnrs.lacito.liftapi.change;

import java.util.List;
import java.util.OptionalInt;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasField;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.HasReversal;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftTranslation;
import fr.cnrs.lacito.liftapi.model.LiftVariant;

/// Puts a component into its parent, or takes it out, through the public
/// {@code addX}/{@code deleteX} method that fits both.
///
/// The one place that knows which method holds which kind of component; the component
/// events use it to revert and reapply. It only calls the public API, so the
/// registration rules apply as for any other caller.
final class Slots {

    private Slots() {}

    /// Add {@code component} to {@code parent}, at {@code position} for a list-held one.
    static void add(Object parent, AbstractLiftRoot component, OptionalInt position) {
        switch (component) {
            case LiftEntry e -> {
                LiftDictionary d = (LiftDictionary) parent;
                d.addEntry(e, at(position, d.getLiftDictionaryRegistry().getEntries()));
            }
            case LiftSense s -> {
                HasSense p = (HasSense) parent;
                p.addSense(at(position, p.getSenses()), s);
            }
            case LiftExample x -> {
                LiftSense p = (LiftSense) parent;
                p.addExample(at(position, p.getExamples()), x);
            }
            case LiftVariant v -> {
                LiftEntry p = (LiftEntry) parent;
                p.addVariant(at(position, p.getVariants()), v);
            }
            case LiftPronunciation x -> {
                HasPronunciation p = (HasPronunciation) parent;
                p.addPronunciation(at(position, p.getPronunciations()), x);
            }
            case LiftRelation r -> {
                HasRelations p = (HasRelations) parent;
                p.addRelation(at(position, p.getRelations()), r);
            }
            case LiftEtymology y -> {
                LiftEntry p = (LiftEntry) parent;
                p.addEtymology(at(position, p.getEtymologies()), y);
            }
            case LiftReversal r -> {
                if (position.isEmpty() && parent instanceof LiftReversal main) {
                    main.setMain(r);
                } else {
                    HasReversal p = (HasReversal) parent;
                    p.addReversal(at(position, p.getReversals()), r);
                }
            }
            case LiftTrait t -> {
                HasTrait p = (HasTrait) parent;
                p.addTrait(at(position, p.getTraits()), t);
            }
            case LiftAnnotation a -> {
                HasAnnotation p = (HasAnnotation) parent;
                p.addAnnotation(at(position, p.getAnnotations()), a);
            }
            case LiftMedia m -> {
                LiftPronunciation p = (LiftPronunciation) parent;
                p.addMedia(at(position, p.getMedias()), m);
            }
            case LiftIllustration i -> {
                LiftSense p = (LiftSense) parent;
                p.addIllustration(at(position, p.getIllustrations()), i);
            }
            case LiftNote n -> ((HasNote) parent).addNote(n);
            case LiftField f -> ((HasField) parent).addField(f);
            case LiftTranslation t -> ((LiftExample) parent).addTranslation(t);
            case GrammaticalInfo g -> ((LiftSense) parent).setGrammaticalInfo(g);
            default -> throw new IllegalArgumentException(
                "Cannot add a " + component.getClass().getSimpleName() + " back to its parent.");
        }
    }

    /// Delete {@code component} from {@code parent}, unregistering it.
    static void delete(Object parent, AbstractLiftRoot component) {
        switch (component) {
            case LiftEntry e -> ((LiftDictionary) parent).removeEntry(e);
            case LiftSense s -> ((HasSense) parent).deleteSense(s);
            case LiftExample x -> ((LiftSense) parent).deleteExample(x);
            case LiftVariant v -> ((LiftEntry) parent).deleteVariant(v);
            case LiftPronunciation x -> ((HasPronunciation) parent).deletePronunciation(x);
            case LiftRelation r -> ((HasRelations) parent).deleteRelation(r);
            case LiftEtymology y -> ((LiftEntry) parent).deleteEtymology(y);
            case LiftReversal r -> {
                if (parent instanceof LiftReversal main && main.getMain() == r) {
                    main.deleteMain();
                } else {
                    ((HasReversal) parent).deleteReversal(r);
                }
            }
            case LiftTrait t -> ((HasTrait) parent).deleteTrait(t);
            case LiftAnnotation a -> ((HasAnnotation) parent).deleteAnnotation(a);
            case LiftMedia m -> ((LiftPronunciation) parent).deleteMedia(m);
            case LiftIllustration i -> ((LiftSense) parent).deleteIllustration(i);
            case LiftNote n -> ((HasNote) parent).deleteNote(n);
            case LiftField f -> ((HasField) parent).deleteField(f);
            case LiftTranslation t -> ((LiftExample) parent).deleteTranslation(t);
            case GrammaticalInfo g -> ((LiftSense) parent).deleteGrammaticalInfo();
            default -> throw new IllegalArgumentException(
                "Cannot delete a " + component.getClass().getSimpleName() + " from its parent.");
        }
    }

    private static int at(OptionalInt position, List<?> list) {
        return position.orElse(list.size());
    }
}

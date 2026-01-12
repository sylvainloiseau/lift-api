package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

/**
 * An annotation. 
 * 
 * Annotations can appear on most lift objects, including
 * {@link LiftTrait}, {@link LiftField} or in the {@link Form}s of a {@link MultiText} object.
 * 
 * Since an annotation can itself contains a MultiText object,
 * there is an possibility of unlimited recursive hierarchy of {@link Form} and {@link LiftAnnotation}.
 * 
 * {@see HasAnnotation}.
 */
public final class LiftAnnotation
    extends AbstractLiftRoot {

    @Getter protected final String name;
    @Getter protected Optional<String> value = Optional.empty();
    @Getter protected Optional<String> who = Optional.empty();
    @Getter protected Optional<String> when = Optional.empty();
    @Setter @Getter protected HasAnnotation parent;

    /**
     * Create an annotation. The name is the only mandatory component of an annotation.
     * 
     * @param name the name of the annotation.
     * @param value the value of the annotation. Can be {@code null}.
     * @param who the name giving the author of the annotation. can be {@code null}.
     * @param when the date of the annotation. can be {@code null}.
     */
    protected LiftAnnotation(String name) {
        this.name = name;
    }

    public MultiText getText() {
        return getMainMultiText();
    }

    protected void setValue(String value) {
        this.value = Optional.of(value);
    }

    protected void setWho(String who) {
        this.who = Optional.of(who);
    }

    protected void setWhen(String when) {
        this.when = Optional.of(when);
    }

}

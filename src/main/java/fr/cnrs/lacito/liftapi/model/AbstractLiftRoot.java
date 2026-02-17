package fr.cnrs.lacito.liftapi.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Root of the hierarchy for Lift objects.
 * All the concret subclasses of this class have a name starting with "Lift": LiftSense, LiftEntry, etc.
 * 
 * This abstract class provide a Multitext field used by subclasses.
 * Subclasses are responsible for exposing this field
 * with the correct accessor. For instance {@code LiftEntry#getForms()}
 * refers to this field, while renamming it.
 */
public sealed abstract class AbstractLiftRoot
    permits AbstractExtensibleWithoutField, LiftAnnotation,
    LiftIllustration, LiftMedia, LiftTrait, LiftHeader, LiftHeaderFieldDefinition,
    LiftReversal {
        
    private final MultiText mainMultiText = new MultiText();
    protected final Map<String,String> otherXmlAttributes = new HashMap<>();

    /**
     * The semantic of this multitext depends on the subclass.
     * @return
     */
    public MultiText getMainMultiText() {
        return mainMultiText;
    }

    public Map<String,String> getOtherXmlAttributes() {
        return otherXmlAttributes;
    }

    protected void addToMainMultiText(Form t) {
        mainMultiText.add(t);
    }
}

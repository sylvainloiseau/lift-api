module fr.cnrs.lacito.liftapi {
    requires transitive java.xml;
    requires java.logging;
    requires java.net.http;
    requires lombok;
    requires transitive javafx.base;
    exports fr.cnrs.lacito.liftapi.model;
    exports fr.cnrs.lacito.liftapi.xml; // TODO : not exported
    exports fr.cnrs.lacito.liftapi;
    exports fr.cnrs.lacito.liftapi.builder; // TODO : not exported
    opens fr.cnrs.lacito.liftapi;
    opens fr.cnrs.lacito.liftapi.model;
    opens fr.cnrs.lacito.liftapi.xml; // TODO : not exported
    opens fr.cnrs.lacito.liftapi.builder; // TODO : not exported
}

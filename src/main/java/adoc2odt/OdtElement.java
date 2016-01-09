package adoc2odt;

import org.jdom2.Attribute;
import org.jdom2.Element;

public abstract class OdtElement {

    protected final Element rootElement;

    protected OdtElement(Element rootElement) {
        this.rootElement = rootElement;
    }

    private Element createOdtElement(String name, String namespace) {
        return new Element(name, rootElement.getNamespace(namespace));
    }

    protected Attribute createOdtAttribute(String name, String value, String namespace) {
        return new Attribute(name, value, rootElement.getNamespace(namespace));
    }

}

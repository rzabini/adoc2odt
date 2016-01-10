package adoc2odt;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.io.File;

public class OdtDocument {

    private final Document xmlContents;
    private Element currentElement = null;


    public OdtDocument(Document document) {
        xmlContents = document;

        Namespace officeNS = getRootElement().getNamespace("office");
        currentElement = getRootElement().getChild("body", officeNS).getChild("text", officeNS);
        currentElement.removeContent();

    }

    public Element getRootElement() {
        return xmlContents.getRootElement();
    }

    public void addContent(Element element) {
        currentElement.addContent(element);
    }

    public Document getXmlDocument() {
        return xmlContents;
    }

    public void openNode(Element element) {
        currentElement.addContent(element);
        currentElement = element;

    }

    public void closeNode() {
        currentElement = currentElement.getParentElement();
    }

    public Attribute createOdtAttribute(String name, String value, String namespace) {
        return new Attribute(name, value, getRootElement().getNamespace(namespace));
    }

    Element createOdtElement(String name, String namespace) {
        return new Element(name, getRootElement().getNamespace(namespace));
    }

    Element createOdtTableElement(String name) {
        return new Element(name, getRootElement().getNamespace("table"));
    }

    Namespace getOdtTextNamespace() {
        return getRootElement().getNamespace("text");
    }

    public OdtImage createOdtImage(Element htmlElement, File basePath) {
        return new OdtImage(getRootElement(), htmlElement, basePath);
    }
}

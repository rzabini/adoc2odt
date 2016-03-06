package adoc2odt;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

public class Metadata {

    private final Document document;

    public Metadata(Document document) {
        this.document = document;
    }


    public void setMetaProperty(String attributeNamespace, String attributeName, String attributeValue) {
        Element root = document.getRootElement();
        Element meta = root.getChild("meta", root.getNamespace());
        Element attributeElement = meta.getChild(attributeName, root.getNamespace(attributeNamespace));
        if (attributeElement == null) {
            attributeElement = new Element(attributeName, root.getNamespace(attributeNamespace));
            meta.addContent(attributeElement);
        }
        attributeElement.setText(attributeValue);
    }


    String write(XMLOutputter xo) {
        return xo.outputString(document);
    }
}

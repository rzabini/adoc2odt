package adoc2odt;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.io.File;

public class OdtDocument {

    private final Document xmlContents;
    private Element currentElement = null;
    private final OdtTables odtTables = new OdtTables();


    public OdtDocument(Document template) {
        xmlContents = template;

        Namespace officeNS = getRootElement().getNamespace("office");
        currentElement = getRootElement().getChild("body", officeNS).getChild("text", officeNS);
        //currentElement.removeContent();

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

    Element createXmlElement(String name, String namespace) {
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

    private Element createXmlElementInTextNamespace(String name) {
        return new Element(name, getOdtTextNamespace());
    }

    Attribute createOdtAttribute(String name, String value) {
        return new Attribute(name, value, getOdtTextNamespace());
    }

    Element createStyledOdtElement(String name, Object style) {
        Element element = createXmlElementInTextNamespace(name);
        if (style != null)
            element.setAttribute(createOdtAttribute("style-name", style.toString()));
        return element;
    }

    private Element createOdtTableElement(String name, String style) {
        Element element = createOdtTableElement(name);
        element.setAttribute(createOdtAttribute("style-name", style));
        return element;
    }

    void openTableRow() {
        Element element= createOdtTableElement("table-row");
        openNode(element);
    }

    int getTableCount() {
        return odtTables.getTableCount();
    }

    void incrementTableCount() {
        odtTables.incrementTableCount();
    }

    void createTableColumn(long tableColumnIndex) {
        Element columnElement = createXmlElement("table-column", "table");
        columnElement.setAttribute(createOdtAttribute("style-name", String.format("Table%d.%s", getTableCount(), 'A' + tableColumnIndex), "table"));
        addContent(columnElement);
    }

    void openTable() {
        incrementTableCount();
        Element element= createOdtTableElement("table");
        element.setAttribute(createOdtAttribute("style-name", String.format("Table%d", getTableCount()), "table"));

        openNode(element);
    }


    public Element createSpanElement() {
        return createXmlElementInTextNamespace("span");
    }

    public Element createSpanElement(String styleName) {
        Element spanElement = createXmlElementInTextNamespace("span");
        spanElement.setAttribute("style-name", styleName, getOdtTextNamespace());
        //spanElement.setText(text);
        return  spanElement;
    }

    public Element createParagraphElement() {
        return createXmlElementInTextNamespace("p");
    }

    public Element createSpaceElement() {
        return createXmlElementInTextNamespace("s");
    }


    public Element createLineBreakElement() {
        return createXmlElementInTextNamespace("line-break");
    }

    public Element createAElement(String href) {
        Element aElement = createXmlElementInTextNamespace("a");
        aElement.setAttribute(createOdtAttribute("href", href, "xlink"));
        return aElement;
    }
}

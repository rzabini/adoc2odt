package adoc2odt;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class OdtDocument {

    private final Document xmlContents;
    private final OutputStream outputStream;
    private final Metadata metadata;
    private final Manifest manifest;
    private Element currentElement = null;
    private final OdtTables odtTables = new OdtTables();
    private final Map<String, ZipEntrySource> entries = new HashMap<String, ZipEntrySource>();


    public OdtDocument(ZipFile template, OutputStream outputStream) throws IOException, JDOMException {
        initEntries(template);
        SAXBuilder saxBuilder = new SAXBuilder();
        xmlContents = saxBuilder.build(entries.get("content.xml").getInputStream());
        Namespace officeNS = getRootElement().getNamespace("office");
        currentElement = getRootElement().getChild("body", officeNS).getChild("text", officeNS);
        this.outputStream = outputStream;

        this.metadata = new Metadata(saxBuilder.build(entries.get("meta.xml").getInputStream()));
        this.manifest = new Manifest(saxBuilder.build(entries.get("META-INF/manifest.xml").getInputStream()));
    }

    private void initEntries(ZipFile source) {
        Enumeration<? extends ZipEntry> sourceEntries = source.entries();
        while (sourceEntries.hasMoreElements()) {
            ZipEntry zipEntry = sourceEntries.nextElement();
            byte[] content = ZipUtil.unpackEntry(source, zipEntry.getName());
            entries.put(zipEntry.getName(), new ByteSource(zipEntry.getName(), content));
        }
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

    public void write() {
        XMLOutputter xo = new XMLOutputter();

        entries.put("content.xml", new ByteSource("content.xml", xo.outputString(xmlContents).getBytes()));
        entries.put("meta.xml", new ByteSource("meta.xml", metadata.write(xo).getBytes()));
        entries.put("META-INF/manifest.xml", new ByteSource("META-INF/manifest.xml", manifest.write(xo).getBytes()));

        ZipUtil.pack(entries.values().toArray(new ZipEntrySource[entries.size()]), outputStream);
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("cannot close output stream", e);
        }

    }

    public void storeFile(File imageFile, String fileRelativePath) {
        manifest.addFileEntry(fileRelativePath);
        entries.put(fileRelativePath, new FileSource(fileRelativePath, imageFile));
    }

    public void setGenerator(String generator) {
        metadata.setMetaProperty("meta", "generator", generator);
    }

    public void setTitle(String title) {
        metadata.setMetaProperty( "dc", "title", title);
    }
}

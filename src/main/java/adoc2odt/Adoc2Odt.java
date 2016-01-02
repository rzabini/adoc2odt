package adoc2odt;

import com.gc.iotools.stream.os.OutputStreamToInputStream;
import org.asciidoctor.ast.*;
import org.asciidoctor.ast.Document;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Adoc2Odt implements AdocListener {

    public static final String MAIN_NAMESPACE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";

    private final File styleFile;
    private final ZipOutputStream zos;

    private final org.jdom2.Document odtDocument;
    private Element currentElement = null;

    private int tableCount = 0;

    public Adoc2Odt(File outputFile, File styleFile) throws Exception {
        this.styleFile = styleFile;

        FileOutputStream fos = new FileOutputStream(outputFile);
        zos = new ZipOutputStream(fos);

        odtDocument = extractXMLFromZip(styleFile, "content.xml");

        Namespace officeNS = odtDocument.getRootElement().getNamespace("office");
        currentElement = odtDocument.getRootElement().getChild("body", officeNS).getChild("text", officeNS);
        currentElement.removeContent();

        //odtDocument.getRootElement().addContent(createOdtElement("automatic-styles", "office"));
    }

    @Override
    public void visitDocument(Document document) {
        try {
            writeMetadata(styleFile, "meta.xml", document);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void departDocument(Document adocDocument) {

        try {
            writeContent();
            copyZipEntry(styleFile, zos, "settings.xml");
            copyZipEntry(styleFile, zos, "styles.xml");

            copyZipEntry(styleFile, zos, "mimetype");
            copyZipEntry(styleFile, zos, "META-INF/manifest.xml");


            zos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    Element fromString(String xml)  {
        SAXBuilder saxBuilder = new SAXBuilder();
        InputStream xmlStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try {
            return saxBuilder.build(xmlStream).getRootElement();
        } catch (Exception e) {
           throw new RuntimeException(e);
        }
    }

    @Override
    public void visitParagraph(Block paragraph)  {
        Element htmlElement= fromString(paragraph.convert()).getChild("p");
        currentElement.addContent(translateHtml(htmlElement));
    }

    private Element translateHtml(Element htmlElement) {
        Element newParagraph = createOdtElement("p");
        for (Content child : htmlElement.getContent()) {
            if (child instanceof Text)
                newParagraph.addContent(htmlElementToOdtElement((Text)child));
            else if (child instanceof Element)
                newParagraph.addContent(htmlElementToOdtElement((Element) child));
        }
        return newParagraph;
    }

    private Content htmlElementToOdtElement(Element child) {
        Element odtElement = child.clone();
        if (odtElement.getName().equalsIgnoreCase("a")) {
            odtElement.setNamespace(getOdtTextNamespace());
            odtElement.setAttribute(createOdtAttribute("href", child.getAttribute("href").getValue(), "xlink"));
        } else if (odtElement.getName().equalsIgnoreCase("em")) {
            addTextRun(odtElement, "adoc-emphasis");
        } else if (odtElement.getName().equalsIgnoreCase("strong")) {
            addTextRun(odtElement, "adoc-strong");
        }
        return odtElement;
    }

    private void addTextRun(Element odtElement, String styleName) {
        odtElement.setNamespace(getOdtTextNamespace());
        odtElement.setName("span");
        odtElement.setAttribute("style-name", styleName, getOdtTextNamespace());
    }

    private Content htmlElementToOdtElement(Text child) {
        return child.clone();
    }

    @Override
    public void departParagraph(Block block) {

    }

    @Override
    public void visitSection(Section section) {
        Element element=new Element("h", getOdtTextNamespace());
        element.setAttribute("style-name", String.format("adoc-heading-%d", section.getLevel()), getOdtTextNamespace());
        element.setText(section.getTitle());
        currentElement.addContent(element);
        //currentElement = element;
    }

    @Override
    public void departSection(Section section) {
        //currentElement = currentElement.getParentElement();
    }

    @Override
    public void visitList(ListNode block) {
        Element element=new Element("list", getOdtTextNamespace());
        currentElement.addContent(element);
        currentElement = element;
    }

    @Override
    public void departList(ListNode block) {
        currentElement = currentElement.getParentElement();
    }

    @Override
    public void visitListItem(ListItem block) {
        Element element=new Element("list-item", getOdtTextNamespace());
        Element htmlElement= fromString(String.format("<p>%s</p>", block.getText()));
        element.addContent(translateHtml(htmlElement));
        currentElement.addContent(element);
    }

    @Override
    public void departListItem(ListItem block) {

    }

    @Override
    public void visitListing(Block block) {
        Element element = createStyledOdtElement("p", "adoc-listing");

        for (String line : block.lines()){
            int leadingSpacesCount = line.indexOf(line.trim());
            Element spaces = createOdtElement("s");
            spaces.setAttribute(createOdtAttribute("c", Integer.toString(leadingSpacesCount)));
            element.addContent(spaces);
            element.addContent(new Text(line.trim()));
            element.addContent(createOdtElement("line-break"));
        }
        currentElement.addContent(element);
    }


    private Attribute createOdtAttribute(String name, String value) {
        return new Attribute(name, value, getOdtTextNamespace());
    }

    private Attribute createOdtAttribute(String name, String value, String namespace) {
        return new Attribute(name, value, odtDocument.getRootElement().getNamespace(namespace));
    }

    @Override
    public void departListing(Block block) {

    }

    @Override
    public void visitTable(TableNode table) {
        ++tableCount;
        Element element= createOdtTableElement("table");
        element.setAttribute(createOdtAttribute("style-name", String.format("Table%d", tableCount), "table"));

        currentElement.addContent(element);
        currentElement = element;

    }

    @Override
    public void departTable(TableNode table) {
        currentElement = currentElement.getParentElement();

    }

    @Override
    public void visitBodyCell(TableCell cell) {
        Element element= createOdtTableElement("table-cell");

        for (Object line : cell.lines()) {
            Element htmlElement = fromString(String.format("<p>%s</p>", line));
            element.addContent(translateHtml(htmlElement));
        }

        currentElement.addContent(element);
    }

    @Override
    public void departTableRow() {
        currentElement = currentElement.getParentElement();
    }

    @Override
    public void visitHeaderRows() {
        Element element= createOdtTableElement("table-header-rows");
        currentElement.addContent(element);
        currentElement = element;
    }

    @Override
    public void departHeaderRows() {
        currentElement = currentElement.getParentElement();
    }

    @Override
    public void visitFooterRows() {
        Element element= createOdtTableElement("table-footer-rows");
        currentElement.addContent(element);
        currentElement = element;
    }

    @Override
    public void departFooterRows() {
        currentElement = currentElement.getParentElement();

    }

    @Override
    public void visitTableColumn(TableColumn tableColumn) {
        Element columnElement = createOdtElement("table-column", "table");
        columnElement.setAttribute(createOdtAttribute("style-name", String.format("Table%d.%s", tableCount, 'A' + tableColumn.getIndex() ), "table"));
        currentElement.addContent(columnElement);

    }

    @Override
    public void visitTableRow() {
        Element element= createOdtTableElement("table-row");
        currentElement.addContent(element);
        currentElement = element;
    }

    private Element createOdtElement(String name) {
        return new Element(name, getOdtTextNamespace());
    }

    private Element createOdtElement(String name, String namespace) {
        return new Element(name, odtDocument.getRootElement().getNamespace(namespace));
    }

    private Element createOdtTableElement(String name) {
        return new Element(name, odtDocument.getRootElement().getNamespace("table"));
    }

    private Element createStyledOdtElement(String name, String style) {
        Element element = createOdtElement(name);
        element.setAttribute(createOdtAttribute("style-name", style));
        return element;
    }

    private Element createOdtTableElement(String name, String style) {
        Element element = createOdtTableElement(name);
        element.setAttribute(createOdtAttribute("style-name", style));
        return element;
    }

    private Namespace getOdtTextNamespace(String name) {
        return odtDocument.getRootElement().getNamespace("text");
    }

    private Namespace getOdtTextNamespace() {
        return getOdtTextNamespace("text");
    }


    private void writeMetadata(File styleFile, String fileName, Document adocDocument) throws Exception {
        org.jdom2.Document document = extractXMLFromZip(styleFile, fileName);

        Element root = document.getRootElement();
        List<Namespace> ns = root.getNamespacesIntroduced();
        Element meta = root.getChild("meta", root.getNamespace());
        Element generator = meta.getChild("generator", root.getNamespace("meta"));
        generator.setText("adoc2odt");

        Element title = meta.getChild("title", root.getNamespace("dc"));
        if (title == null) {
            title = new Element("title", root.getNamespace("dc"));
            meta.addContent(title );
        }

        title.setText(adocDocument.doctitle());

        ZipEntry ze= new ZipEntry(fileName);
        zos.putNextEntry(ze);
        XMLOutputter xo = new XMLOutputter();
        xo.output(document, zos);
        zos.closeEntry();
    }

    private void copyZipEntry(File styleFile, ZipOutputStream zos, String fileName) throws IOException {
        ZipEntry ze= new ZipEntry(fileName);
        zos.putNextEntry(ze);
        extractFileFromZip(styleFile, fileName, zos);
        zos.closeEntry();
    }

    private void writeContent() throws IOException {
        ZipEntry ze= new ZipEntry("content.xml");
        zos.putNextEntry(ze);
        XMLOutputter xo = new XMLOutputter();
        xo.output(odtDocument, zos);
        zos.closeEntry();
    }

    org.jdom2.Document extractXMLFromZip(File zipFile, String fileName) throws Exception {

        final OutputStreamToInputStream<org.jdom2.Document> out = new OutputStreamToInputStream<org.jdom2.Document>() {
            @Override
            protected org.jdom2.Document doRead(final InputStream istream) throws Exception {
          /*
           * Read the data from the InputStream "istream" passed as parameter.
           */
                SAXBuilder saxBuilder = new SAXBuilder();
                return saxBuilder.build(istream);
            }
        };
        try {
            extractFileFromZip(zipFile, fileName, out);
        } finally {
            // don't miss the close (or a thread would not terminate correctly).
            out.close();
        }


        return out.getResult();


    }

    void extractFileFromZip(File zipFile, String fileName, OutputStream outputStream) throws IOException {
        FileInputStream fin = new FileInputStream(zipFile);
        BufferedInputStream bin = new BufferedInputStream(fin);
        ZipInputStream zin = new ZipInputStream(bin);
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.getName().equals(fileName)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zin.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                break;
            }
        }
    }


}

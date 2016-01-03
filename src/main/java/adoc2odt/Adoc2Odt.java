package adoc2odt;

import com.gc.iotools.stream.os.OutputStreamToInputStream;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.asciidoctor.ast.*;
import org.asciidoctor.ast.Document;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Adoc2Odt implements AdocListener {

    public static final String MAIN_NAMESPACE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
    public static final double DFAULT_72_DPI = 28.34;

    private final File styleFile;
    private final ZipOutputStream zos;

    private final org.jdom2.Document odtDocument;
    private Element currentElement = null;

    private int tableCount = 0;
    private File basePath;

    private List<String> imageList = new ArrayList<String>();

    private final Stack<String> styleStack = new Stack<String>();

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
    public void visitDocument(Document document, String absolutePath) {
        this.basePath = new File(absolutePath).getParentFile();
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
            extractFolderFromZip(styleFile, "Pictures", zos);
            writeManifest();

            zos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void writeManifest() throws IOException {
        ZipEntry ze= new ZipEntry("META-INF/manifest.xml");
        zos.putNextEntry(ze);
        org.jdom2.Document document = getManifestXMLDocument();

        XMLOutputter xo = new XMLOutputter();
        xo.setFormat(Format.getPrettyFormat());
        xo.output(document, zos);
        zos.closeEntry();

    }

    private org.jdom2.Document getManifestXMLDocument() {
        org.jdom2.Document document = new org.jdom2.Document();

        Namespace manifestNamespace = Namespace.getNamespace("manifest", "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\"");
        Element root = new Element("manifest", manifestNamespace);
        document.addContent(root);

        root.addContent(createFileEntryManifestElement("/", "application/vnd.oasis.opendocument.text"));
        root.addContent(createFileEntryManifestElement("styles.xml"));
        root.addContent(createFileEntryManifestElement("content.xml"));
        root.addContent(createFileEntryManifestElement("meta.xml"));
        root.addContent(createFileEntryManifestElement("settings.xml"));

        for (String imageFile : imageList)
            root.addContent(createFileEntryManifestElement(String.format("%s", imageFile), "image/png"));
        return document;
    }

    private Element createFileEntryManifestElement(String fileEntryFullPath) {
        return createFileEntryManifestElement(fileEntryFullPath, "text/xml");
    }

    private Element createFileEntryManifestElement(String fileEntryFullPath, String mediaType) {
        Namespace manifestNamespace = Namespace.getNamespace("manifest", "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\"");

        Element fileEntry = new Element("file-entry", manifestNamespace);
        fileEntry.setAttribute("full-path", fileEntryFullPath, manifestNamespace);
        fileEntry.setAttribute("media-type", mediaType, manifestNamespace);
        return fileEntry;
    }

    Element fromString(String xml)  {
        xml = xml.replaceAll ("(<img[^>]*)>", "$1 />");
        xml = xml.replaceAll ("(<br[^>]*)>", "$1 />");

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
        Object adocStyle = getAdocStyle(paragraph);
        if (adocStyle != null)
            pushCurrentParagraphStyle(adocStyle.toString());

        String appliedStyle = styleStack.empty()? null : styleStack.peek();
        currentElement.addContent(translateHtml(htmlElement, appliedStyle));
    }

    @Override
    public void departParagraph(Block paragraph) {
        Object adocStyle = getAdocStyle(paragraph);
        if (adocStyle != null)
            styleStack.pop();

    }

    private Object getAdocStyle(Block paragraph) {
        if (paragraph.getAttributes().get("role") != null)
            return String.format("adoc-%s", paragraph.getAttributes().get("role") );
        if (paragraph.getAttributes().get("style") != null)
            return String.format("adoc-%s", paragraph.getAttributes().get("style") );
        return null;
    }

    private Element translateHtml(Element htmlElement, Object style) {
        Element newParagraph = createStyledOdtElement("p", style);
        fill(htmlElement, newParagraph);
        return newParagraph;
    }

    private Element translateHtml(Element htmlElement) {
        Element newParagraph = createOdtElement("p");
        fill(htmlElement, newParagraph);
        return newParagraph;
    }

    private void fill(Element htmlElement, Element parent) {
        if(htmlElement == null)
            return;
        for (Content child : htmlElement.getContent()) {
            if (child instanceof Text)
                parent.addContent(htmlElementToOdtElement((Text)child));
            else if (child instanceof Element) {
                Element subElement = htmlElementToOdtElement((Element) child);
                parent.addContent(subElement);
                fill ((Element)child, subElement);
            }
        }
    }

    private Element htmlElementToOdtElement(Element htmlElement) {
        Element odtElement = createOdtElement(htmlElement.getName()); //child.clone();
        if (odtElement.getName().equalsIgnoreCase("a")) {
            odtElement.setNamespace(getOdtTextNamespace());
            odtElement.setAttribute(createOdtAttribute("href", htmlElement.getAttribute("href").getValue(), "xlink"));
        } else if (odtElement.getName().equalsIgnoreCase("em")) {
            addTextRun(odtElement, "adoc-emphasis");
        } else if (odtElement.getName().equalsIgnoreCase("strong")) {
            addTextRun(odtElement, "adoc-strong");
        }  else if (odtElement.getName().equalsIgnoreCase("img")) {
            //odtElement.setName("image");
            return createOdtImage(htmlElement);
        }

        return odtElement;
    }

    private Element createOdtImage(Element htmlElement)  {

        String imageFilePath = htmlElement.getAttributeValue("src");
        File imageFile = new File( basePath, imageFilePath);
        ImageInfo imageInfo = getImageInfo(imageFile);

        Element frame = createOdtElement("frame", "draw");
        frame.setAttribute(createOdtAttribute("name", "Image1", "draw"));
        frame.setAttribute(createOdtAttribute("z-index", "0", "draw"));

        frame.setAttribute(createOdtAttribute("width", getImageSizeInCm(imageInfo.getWidth()), "svg"));
        frame.setAttribute(createOdtAttribute("height",  getImageSizeInCm(imageInfo.getHeight()), "svg"));
        frame.setAttribute(createOdtAttribute("anchor-type", "paragraph", "text"));


        Element image = createOdtElement("image", "draw");
        image.setAttribute(createOdtAttribute("type", "simple", "xlink"));
        image.setAttribute(createOdtAttribute("show", "embed", "xlink"));
        image.setAttribute(createOdtAttribute("actuate", "onLoad", "xlink"));

        frame.addContent(image);


        storeImage(imageFile);


        image.setAttribute(createOdtAttribute("href", String.format("Pictures/%s",
                imageFile.getName()) ,"xlink"));
        return frame;
    }

    private String getImageSizeInCm(int size) {
        return String.format("%scm", Double.toString (size/ DFAULT_72_DPI)).replace(',', '.');
    }

    private ImageInfo getImageInfo(File imageFile)  {
        try {
            ImageInfo info = Sanselan.getImageInfo(imageFile);
            return info;
        } catch (ImageReadException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    public void visitSection(Section section) {
        Element heading=new Element("h", getOdtTextNamespace());
        heading.setAttribute("style-name", String.format("adoc-heading-%d", section.getLevel()), getOdtTextNamespace());
        appendTranslatedHtmlFragment(heading, section.getTitle());

        currentElement.addContent(heading);
    }

    private void appendTranslatedHtmlFragment(Element parentNode, String htmlFragment) {
        Element htmlElement= fromString(String.format("<p>%s</p>", htmlFragment));
        for (Content child : translateHtml(htmlElement).getContent())
            parentNode.addContent(child.clone());
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
    public void visitSidebar(Block block) {
        Element title = createStyledOdtElement("p", "adoc-sidebar-title");
        title.setText(getAttribute(block, "title"));
        currentElement.addContent(title);
        pushCurrentParagraphStyle("adoc-sidebar");
    }

    private void pushCurrentParagraphStyle(String styleName) {
        styleStack.push(styleName);
    }

    @Override
    public void departSidebar(Block block) {
        styleStack.pop();
    }

    @Override
    public void visitAdmonition(Block block) {
        Element element= createStyledOdtElement("p", String.format("adoc-%s", getAttribute(block, "style").toLowerCase()));

        for (String line : block.lines()) {
            appendTranslatedHtmlFragment(element, line);
        }
        currentElement.addContent(element);
    }

    @Override
    public void departAdmonition(Block block) {

    }


    private String getAttribute(Block block, String attributeName) {
        Object attr = block.getAttributes().get(attributeName);
        if (attr != null)
            return attr.toString();
        return "";
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

    private Element createStyledOdtElement(String name, Object style) {
        Element element = createOdtElement(name);
        if (style != null)
            element.setAttribute(createOdtAttribute("style-name", style.toString()));
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

    private void storeImage(File imageFile) {
        try {
            String fileRelativePath = String.format("Pictures/%s", imageFile.getName());
            imageList.add(fileRelativePath);
            ZipEntry ze = new ZipEntry(fileRelativePath);
            zos.putNextEntry(ze);

            Path path = imageFile.toPath();
            Files.copy(path, zos);
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


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

    void extractFolderFromZip(File zipFile, String folder, OutputStream outputStream) throws IOException {
        FileInputStream fin = new FileInputStream(zipFile);
        BufferedInputStream bin = new BufferedInputStream(fin);
        ZipInputStream zin = new ZipInputStream(bin);
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.getName().startsWith(folder)) {

                ZipEntry destZipEntry = new ZipEntry(ze.getName());
                zos.putNextEntry(ze);

                byte[] buffer = new byte[8192];
                int len;
                while ((len = zin.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }

                zos.closeEntry();
                imageList.add(ze.getName());

            }
        }
    }


}

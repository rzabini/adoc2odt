package adoc2odt;

import org.asciidoctor.ast.*;
import org.asciidoctor.ast.Document;
import org.jdom2.*;

import java.io.*;
import java.util.Stack;

public class Adoc2Odt implements AdocListener {

    public static final String MAIN_NAMESPACE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";

    private final OdtDocument odtDocument;

    private int tableCount = 0;
    private File basePath;

    private final OdtFile odtFile;

    private final Manifest manifest=new Manifest();

    private final Stack<OdtStyle> styleStack = new Stack<OdtStyle>();

    public Adoc2Odt(File outputFile, File styleFile) throws Exception {

        odtFile = new OdtFile(outputFile, styleFile);
        odtDocument = new OdtDocument(odtFile.extractXMLFromZip(styleFile, "content.xml"));
    }

    @Override
    public void visitDocument(Document document, String absolutePath) {
       writeMetadata(document);
    }

    public void departDocument(Document adocDocument) {
        odtFile.writeContent(odtDocument);

        odtFile.copyCommonElementsFromStyle();
        odtFile.copyPicturesFolderFromStyle(manifest);

        odtFile.writeManifest(manifest.toDocument());
        odtFile.close();
    }


    @Override
    public void visitParagraph(Block paragraph)  {
        HtmlFragment htmlFragment = new HtmlFragment(paragraph.convert());
        Element element=   htmlFragment.toValidXmlElement().getChild("p");
        pushParagraphStyle(paragraph);

        applyCurrentStyle(element);
    }

    private void applyCurrentStyle(Element element) {
        OdtStyle appliedStyle = styleStack.empty()? null : styleStack.peek();
        if(appliedStyle != null)
            odtDocument.addContent(translateHtml(element, appliedStyle));
    }

    private void pushParagraphStyle(Block paragraph) {
        OdtStyle adocStyle = getAdocStyle(paragraph);
        if (adocStyle.isValid())
            pushCurrentParagraphStyle(adocStyle.toString());
    }

    @Override
    public void departParagraph(Block paragraph) {
        popParagraphStyle(paragraph);
    }

    private void popParagraphStyle(Block paragraph) {
        OdtStyle adocStyle = getAdocStyle(paragraph);
        if (adocStyle.isValid())
            styleStack.pop();
    }

    private OdtStyle getAdocStyle(Block paragraph) {
        return new OdtStyle(paragraph.getAttributes());
    }

    private Element translateHtml(Element htmlElement, OdtStyle style) {
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
            odtElement.setNamespace(odtDocument.getOdtTextNamespace());
            odtElement.setAttribute(odtDocument.createOdtAttribute("href", htmlElement.getAttribute("href").getValue(), "xlink"));
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
        OdtImage odtImage = odtDocument.createOdtImage(htmlElement, basePath);
        String imageFilePath = htmlElement.getAttributeValue("src");
        File imageFile = new File( basePath, imageFilePath);
        storeImage(imageFile);
        return odtImage.toOdtElement();
    }

    private void addTextRun(Element odtElement, String styleName) {
        odtElement.setNamespace(odtDocument.getOdtTextNamespace());
        odtElement.setName("span");
        odtElement.setAttribute("style-name", styleName, odtDocument.getOdtTextNamespace());
    }

    private Content htmlElementToOdtElement(Text child) {
        return child.clone();
    }

    @Override
    public void visitSection(Section section) {
        Element heading=new Element("h", odtDocument.getOdtTextNamespace());
        heading.setAttribute("style-name", String.format("adoc-heading-%d", section.getLevel()), odtDocument.getOdtTextNamespace());
        appendTranslatedHtmlFragment(heading, section.getTitle());

        odtDocument.addContent(heading);
    }

    private void appendTranslatedHtmlFragment(Element parentNode, String htmlFragment) {
        Element htmlElement = toValidXmlElement(String.format("<p>%s</p>", htmlFragment));
        for (Content child : translateHtml(htmlElement).getContent())
            parentNode.addContent(child.clone());
    }

    private Element toValidXmlElement(String htmlFragment) {
        return new HtmlFragment(htmlFragment).toValidXmlElement();
    }

    @Override
    public void departSection(Section section) {
        //currentElement = currentElement.getParentElement();
    }

    @Override
    public void visitList(ListNode block) {
        Element element=new Element("list", odtDocument.getOdtTextNamespace());

        odtDocument.openNode(element);
    }

    @Override
    public void departList(ListNode block) {
        odtDocument.closeNode();
    }

    @Override
    public void visitListItem(ListItem block) {
        Element element=new Element("list-item", odtDocument.getOdtTextNamespace());
        Element htmlElement= toValidXmlElement(String.format("<p>%s</p>", block.getText()));
        element.addContent(translateHtml(htmlElement));
        odtDocument.addContent(element);
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
        odtDocument.addContent(element);
    }


    private Attribute createOdtAttribute(String name, String value) {
        return new Attribute(name, value, odtDocument.getOdtTextNamespace());
    }

    @Override
    public void departListing(Block block) {

    }

    @Override
    public void visitTable(TableNode table) {
        ++tableCount;
        Element element= odtDocument.createOdtTableElement("table");
        element.setAttribute(odtDocument.createOdtAttribute("style-name", String.format("Table%d", tableCount), "table"));

        odtDocument.openNode(element);
    }

    @Override
    public void departTable(TableNode table) {
        odtDocument.closeNode();

    }

    @Override
    public void visitBodyCell(TableCell cell) {
        Element element= odtDocument.createOdtTableElement("table-cell");

        for (Object line : cell.lines()) {
            Element htmlElement = toValidXmlElement(String.format("<p>%s</p>", line));
            element.addContent(translateHtml(htmlElement));
        }

        odtDocument.addContent(element);
    }

    @Override
    public void departTableRow() {
        odtDocument.closeNode();
    }

    @Override
    public void visitHeaderRows() {
        Element element= odtDocument.createOdtTableElement("table-header-rows");
        odtDocument.openNode(element);
    }

    @Override
    public void departHeaderRows() {
        odtDocument.closeNode();
    }

    @Override
    public void visitFooterRows() {
        Element element= odtDocument.createOdtTableElement("table-footer-rows");
        odtDocument.openNode(element);
    }

    @Override
    public void departFooterRows() {
        odtDocument.closeNode();
    }

    @Override
    public void visitSidebar(Block block) {
        Element title = createStyledOdtElement("p", "adoc-sidebar-title");
        title.setText(getAttribute(block, "title"));
        odtDocument.addContent(title);
        pushCurrentParagraphStyle("adoc-sidebar");
    }

    private void pushCurrentParagraphStyle(String styleName) {
        styleStack.push(new OdtStyle(styleName));
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
        odtDocument.addContent(element);
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
        Element columnElement = odtDocument.createOdtElement("table-column", "table");
        columnElement.setAttribute(odtDocument.createOdtAttribute("style-name", String.format("Table%d.%s", tableCount, 'A' + tableColumn.getIndex() ), "table"));
        odtDocument.addContent(columnElement);

    }

    @Override
    public void visitTableRow() {
        Element element= odtDocument.createOdtTableElement("table-row");
        odtDocument.openNode(element);
    }

    private Element createOdtElement(String name) {
        return new Element(name, odtDocument.getOdtTextNamespace());
    }

    private Element createStyledOdtElement(String name, Object style) {
        Element element = createOdtElement(name);
        if (style != null)
            element.setAttribute(createOdtAttribute("style-name", style.toString()));
        return element;
    }

    private Element createOdtTableElement(String name, String style) {
        Element element = odtDocument.createOdtTableElement(name);
        element.setAttribute(createOdtAttribute("style-name", style));
        return element;
    }


    private void writeMetadata(Document adocDocument) {
        odtFile.setMetaProperty("meta", "generator", "adoc2odt");
        odtFile.setMetaProperty( "dc", "title", adocDocument.doctitle());
        odtFile.writeMetadata();
    }

    private void storeImage(File imageFile) {
        String fileRelativePath = String.format("Pictures/%s", imageFile.getName());

        if (!manifest.exists(fileRelativePath)) {
            manifest.addFileEntry(fileRelativePath);
            odtFile.storeFile(imageFile, fileRelativePath);
        }
    }

}

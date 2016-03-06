package adoc2odt;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.asciidoctor.ast.*;
import org.asciidoctor.ast.Document;
import org.asciidoctor.internal.IOUtils;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Adoc2Odt implements AdocListener {

    public static final String MAIN_NAMESPACE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";

    private  OdtDocument odtDocument;
    //private final Manifest manifest=new Manifest();

    OdtStyles odtStyles = new OdtStyles();

    private File basePath;

    public Adoc2Odt(OdtDocument odtDocument) {
        this.odtDocument = odtDocument;
    }


    @Override
    public void visitDocument(Document document) {
        odtDocument.setGenerator("adoc2odt");
        odtDocument.setTitle(document.doctitle());
    }

    public void departDocument(Document adocDocument) {
/*        odtFile.writeContent(odtDocument);

        odtFile.copyCommonElementsFromStyle();
        odtFile.copyPicturesFolderFromStyle(manifest);

        odtFile.writeManifest(manifest.toDocument());
        odtFile.close();*/
        odtDocument.write();
    }


    @Override
    public void visitParagraph(Block paragraph)  {

        if (paragraph.getAttr("title") != null) {
            Element title = odtDocument.createParagraphElement();
            title.setText(paragraph.getAttr("title").toString());
            odtDocument.addContent(title);
        }

        HtmlLiteral htmlFragment = new HtmlLiteral(paragraph.convert());
        Element element = htmlFragment.toValidXmlElement().getChild("p");
        pushParagraphStyle(paragraph);

        applyCurrentStyle(element);
    }

    private void applyCurrentStyle(Element element) {
        OdtStyle appliedStyle = odtStyles.getCurrentStyle();
        if(appliedStyle != null)
            odtDocument.addContent(translateHtml(element, appliedStyle));
        else
            odtDocument.addContent(translateHtml(element));
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
            odtStyles.pop();
    }

    private OdtStyle getAdocStyle(Block paragraph) {
        return new OdtStyle(paragraph.getAttributes());
    }

    private Element translateHtml(Element htmlElement, OdtStyle style) {
        Element newParagraph = odtDocument.createStyledOdtElement("p", style);
        fill(htmlElement, newParagraph);
        return newParagraph;
    }

    private Element translateHtml(Element htmlElement) {
        Element newParagraph = odtDocument.createParagraphElement();
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
        if (htmlElement.getName().equalsIgnoreCase("a")) {
            return odtDocument.createAElement(htmlElement.getAttribute("href").getValue());
        }
        else if (htmlElement.getName().equalsIgnoreCase("span")) {
            return odtDocument.createSpanElement();
        }
        else if (htmlElement.getName().equalsIgnoreCase("em")) {
            return odtDocument.createSpanElement("adoc-emphasis");
        }
        else if (htmlElement.getName().equalsIgnoreCase("strong")) {
            return odtDocument.createSpanElement("adoc-strong");
        }
        else if (htmlElement.getName().equalsIgnoreCase("sup")) {
            return odtDocument.createSpanElement( "adoc-sup");
        }
        else if (htmlElement.getName().equalsIgnoreCase("sub")) {
            return odtDocument.createSpanElement("adoc-sub");
        }
        else if (htmlElement.getName().equalsIgnoreCase("code")) {
            return odtDocument.createSpanElement("adoc-code");
        }
        else if (htmlElement.getName().equalsIgnoreCase("u")) {
            return odtDocument.createSpanElement("adoc-u");
        }
        else if (htmlElement.getName().equalsIgnoreCase("br")) {
            return odtDocument.createLineBreakElement();
        }

        else if (htmlElement.getName().equalsIgnoreCase("img")) {
            return createOdtImage(htmlElement);
        }

        throw new RuntimeException("cannot convert htmlElement: " + htmlElement.getName());
    }

    private Element createOdtImage(Element htmlElement)  {
        OdtImage odtImage = odtDocument.createOdtImage(htmlElement, basePath);
        String imageFilePath = htmlElement.getAttributeValue("src");
        File imageFile = new File( basePath, imageFilePath);
        storeImage(imageFile);
        return odtImage.toOdtElement();
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
        return new HtmlLiteral(htmlFragment).toValidXmlElement();
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
        Element element = odtDocument.createStyledOdtElement("p", "adoc-listing");

        for (String line : block.lines()){
            int leadingSpacesCount = line.indexOf(line.trim());
            Element spaces = odtDocument.createSpaceElement();
            spaces.setAttribute(odtDocument.createOdtAttribute("c", Integer.toString(leadingSpacesCount)));
            element.addContent(spaces);
            element.addContent(new Text(line.trim()));
            element.addContent(odtDocument.createLineBreakElement());
        }
        odtDocument.addContent(element);
    }


    @Override
    public void departListing(Block block) {

    }

    @Override
    public void visitTable(TableNode table) {
        odtDocument.openTable();
    }

    @Override
    public void departTable(TableNode table) {
        odtDocument.closeNode();
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
        Element title = odtDocument.createStyledOdtElement("p", "adoc-sidebar-title");
        title.setText(getAttribute(block, "title"));
        odtDocument.addContent(title);
        pushCurrentParagraphStyle("adoc-sidebar");
    }

    private void pushCurrentParagraphStyle(String styleName) {
        odtStyles.push(new OdtStyle(styleName));
    }

    @Override
    public void departSidebar(Block block) {
        odtStyles.pop();
    }

    @Override
    public void visitAdmonition(Block block) {
        Element element= odtDocument.createStyledOdtElement("p", String.format("adoc-%s", getAttribute(block, "style").toLowerCase()));

        for (String line : block.lines()) {
            appendTranslatedHtmlFragment(element, line);
        }
        odtDocument.openNode(element);
    }

    @Override
    public void departAdmonition(Block block) {
        odtDocument.closeNode();
    }

    @Override
    public void visitPreamble(Block block) {
        Element element= odtDocument.createStyledOdtElement("p", String.format("adoc-%s", getAttribute(block, "style").toLowerCase()));

        for (String line : block.lines()) {
            appendTranslatedHtmlFragment(element, line);
        }
        odtDocument.addContent(element);
    }

    @Override
    public void departPreamble(Block block) {

    }

    @Override
    public void departLiteral(Block block) {
        departParagraph(block);
    }

    @Override
    public void visitLiteral(Block block) {
        visitParagraph(block);
    }


    @Override
    public void visitThematicBreak(Block block) {
        Element element= odtDocument.createStyledOdtElement("p", "adoc-thematic-break");
        odtDocument.addContent(element);
    }

    @Override
    public void visitSimpleBodyCell(TableCell tableCell) {
        Element element = openCellNode(tableCell);

        Element htmlElement = new HtmlLiteral(String.format("<p>%s</p>", tableCell.text())).toValidXmlElement();
        element.addContent(translateHtml(htmlElement));

        closeCellNode(tableCell);
    }

    @Override
    public void visitComplexBodyCell(TableCell tableCell) {
        Element element= openCellNode(tableCell);
    }

    private Element createTableCellNode(TableCell tableCell) {
        Element element= odtDocument.createOdtTableElement("table-cell");
        if (tableCell.rowspan() > 0)
            element.setAttribute(odtDocument.createOdtAttribute("number-rows-spanned", Integer.toString(tableCell.rowspan()), "table"));
        if (tableCell.colspan() > 0)
            element.setAttribute(odtDocument.createOdtAttribute("number-columns-spanned", Integer.toString(tableCell.colspan()), "table"));
        return element;
    }

    @Override
    public void departComplexTableCell(TableCell tableCell) {
        closeCellNode(tableCell);
    }

    @Override
    public void basePath(File basePath) {
        this.basePath = basePath;
    }

    private Element openCellNode(TableCell tableCell) {
        Element element = createTableCellNode(tableCell);
        odtDocument.openNode(element);
        return element;
    }

    private void closeCellNode(TableCell tableCell) {
        odtDocument.closeNode();
        for (int i =0; i < tableCell.colspan() - 1; ++i)
            odtDocument.addContent(odtDocument.createOdtTableElement("covered-table-cell"));

    }


    @Override
    public void departThematicBreak(Block block) {

    }

    @Override
    public void visitPageBreak(Block block) {
        Element element= odtDocument.createStyledOdtElement("p", "adoc-page-break");
        odtDocument.addContent(element);
    }

    @Override
    public void departPageBreak(Block block) {

    }


    private String getAttribute(Block block, String attributeName) {
        Object attr = block.getAttributes().get(attributeName);
        if (attr != null)
            return attr.toString();
        return "";
    }

    @Override
    public void visitTableColumn(TableColumn tableColumn) {
        odtDocument.createTableColumn(tableColumn.getIndex());
    }

    private int getTableCount() {
        return odtDocument.getTableCount();
    }

    @Override
    public void visitTableRow() {
        odtDocument.openTableRow();
    }


    private void writeMetadata(Document adocDocument) {
/*        odtFile.setMetaProperty("meta", "generator", "adoc2odt");
        odtFile.setMetaProperty( "dc", "title", adocDocument.doctitle());
        odtFile.writeMetadata();*/
    }

    private void storeImage(File imageFile) {
        String fileRelativePath = String.format("Pictures/%s", imageFile.getName());

        /*if (!manifest.exists(fileRelativePath)) {
            manifest.addFileEntry(fileRelativePath);
            odtDocument.storeFile(imageFile, fileRelativePath);
        }*/
        odtDocument.storeFile(imageFile, fileRelativePath);
    }

}

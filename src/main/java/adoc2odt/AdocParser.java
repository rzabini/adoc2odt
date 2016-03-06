package adoc2odt;

import org.apache.commons.io.IOUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.*;
import org.jruby.RubyArray;
import org.jruby.RubyObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class AdocParser {
    private final File basePath;

    //private final File adocFile;
    //private final Map<String, Object> options;

    Announcer<AdocListener> announcer = Announcer.to(AdocListener.class);
    private final SafeNodeConverter nodeConverter;
    private final InputStream inputStream;

    public AdocParser(InputStream inputStream, File basePath, AdocListener listener) {
        this.inputStream = inputStream;
        this.basePath = basePath;
        nodeConverter = new SafeNodeConverter();
        addListener(listener);
    }

    static String readInputStream(InputStream inputStream, Charset encoding) throws IOException {
        byte[] encoded = IOUtils.toByteArray(inputStream);
        return new String(encoded, encoding);
    }
    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public void addListener(AdocListener adocListener) {
        announcer.addListener(adocListener);
        announcer.announce().basePath(basePath);

    }

    public void parse() throws IOException {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        Document document=asciidoctor.load(readInputStream(this.inputStream, Charset.defaultCharset()), new HashMap<String, Object>());

        announcer.announce().visitDocument(document /*, adocFile.getAbsolutePath()*/);

        ListIterator<AbstractBlock> iterator = nodeConverter.getBlocks(document).listIterator();
        while (iterator.hasNext())
            visit(iterator.next());

        announcer.announce().departDocument(document);
    }

    private void visit(AbstractBlock block) {



        if (block.getContext().equalsIgnoreCase("paragraph"))
            announcer.announce().visitParagraph((Block)block);
        else if (block.getContext().equalsIgnoreCase("section"))
            announcer.announce().visitSection((Section)block);
        else if (block.getContext().equalsIgnoreCase("ulist"))
            announcer.announce().visitList((ListNode)block);
        else if (block.getContext().equalsIgnoreCase("olist"))
            announcer.announce().visitList((ListNode)block);
        else if (block.getContext().equalsIgnoreCase("list_item"))
            announcer.announce().visitListItem((ListItem)block);
        else if (block.getContext().equalsIgnoreCase("list_item"))
            announcer.announce().visitListItem((ListItem)block);
        else if (block.getContext().equalsIgnoreCase("listing"))
            announcer.announce().visitListing((Block)block);
        else if (block.getContext().equalsIgnoreCase("table"))
            visitTable((TableNode)block);
        else if (block.getContext().equalsIgnoreCase("sidebar"))
            visitSidebar((Block)block);
        else if (block.getContext().equalsIgnoreCase("admonition"))
            visitAdmonition((Block)block);
        else if (block.getContext().equalsIgnoreCase("preamble"))
            visitPreamble((Block)block);
        else if (block.getContext().equalsIgnoreCase("literal"))
            visitLiteral((Block)block);
        else if (block.getContext().equalsIgnoreCase("thematic_break"))
            visitThematicBreak((Block)block);
        else if (block.getContext().equalsIgnoreCase("page_break"))
            visitPageBreak((Block)block);
        else if (block.getContext().equalsIgnoreCase("quote"))
            visitLiteral((Block)block);
        else if (block.getContext().equalsIgnoreCase("verse"))
            visitLiteral((Block)block);
        else
            throw new RuntimeException("cannot visit node: " + block.getContext());

        //ListIterator<AbstractBlock> iterator = block.getBlocks().listIterator();
        List<AbstractBlock> abstractBlockList = nodeConverter.getBlocks(block);
        for (AbstractBlock abstractBlock : abstractBlockList) {
            System.out.println(abstractBlock.getTitle());
            visit(abstractBlock);
        }

        if (block.getContext().equalsIgnoreCase("paragraph"))
            announcer.announce().departParagraph((Block)block);
        else if (block.getContext().equalsIgnoreCase("section"))
            announcer.announce().departSection((Section)block);
        else if (block.getContext().equalsIgnoreCase("ulist"))
            announcer.announce().departList((ListNode)block);
        else if (block.getContext().equalsIgnoreCase("olist"))
            announcer.announce().departList((ListNode)block);
        else if (block.getContext().equalsIgnoreCase("list_item"))
            announcer.announce().departListItem((ListItem)block);
        else if (block.getContext().equalsIgnoreCase("listing"))
            announcer.announce().departListing((Block)block);
        else if (block.getContext().equalsIgnoreCase("sidebar"))
            announcer.announce().departSidebar((Block)block);
        else if (block.getContext().equalsIgnoreCase("admonition"))
            announcer.announce().departAdmonition((Block)block);
        else if (block.getContext().equalsIgnoreCase("preamble"))
            announcer.announce().departPreamble((Block)block);
        else if (block.getContext().equalsIgnoreCase("literal"))
            announcer.announce().departLiteral((Block)block);
        else if (block.getContext().equalsIgnoreCase("thematic_break"))
            announcer.announce().departThematicBreak((Block)block);
        else if (block.getContext().equalsIgnoreCase("page_break"))
            announcer.announce().departPageBreak((Block)block);
        else if (block.getContext().equalsIgnoreCase("quote"))
            announcer.announce().departLiteral((Block)block);
        else if (block.getContext().equalsIgnoreCase("verse"))
            announcer.announce().departLiteral((Block)block);


    }

    private void visitPageBreak(Block block) {
        announcer.announce().visitPageBreak(block);
    }

    private void visitThematicBreak(Block block) {
        announcer.announce().visitThematicBreak(block);
    }

    private void visitLiteral(Block block) {
        announcer.announce().visitLiteral(block);
    }

    private void visitPreamble(Block block) {
        announcer.announce().visitPreamble(block);
    }

    private void visitAdmonition(Block block) {
        announcer.announce().visitAdmonition(block);
    }

    private void visitSidebar(Block block) {
        announcer.announce().visitSidebar(block);
    }

    private void visitTable(TableNode tableNode) {
        announcer.announce().visitTable(tableNode);

        List<RubyObject> columnList = tableNode.getColumnList();
        for (RubyObject column : columnList) {
            TableColumn tableColumn = (TableColumn) new SafeNodeConverter().createASTNode(column);
            announcer.announce().visitTableColumn(tableColumn);
        }

        announcer.announce().visitHeaderRows();
        visitRows(tableNode.headerRows());
        announcer.announce().departHeaderRows();

        visitRows(tableNode.bodyRows());

        announcer.announce().visitFooterRows();
        visitRows(tableNode.footerRows());
        announcer.announce().departFooterRows();


        announcer.announce().departTable(tableNode);
    }

    private void visitRows(List<RubyArray> rows) {
        for (List<RubyObject> row : rows) {
            announcer.announce().visitTableRow();
            for (RubyObject cell : row) {
                TableCell tableCell = new SafeNodeConverter().createTableCellASTNode(cell);
                visitTableCell(tableCell);
            }
            announcer.announce().departTableRow();
        }
    }

    private void visitTableCell(TableCell tableCell) {
        if (tableCell.inner_document() == null) {
            announcer.announce().visitSimpleBodyCell(tableCell);
        }
        else {
            Document document = new SafeNodeConverter().createDocumentASTNode(tableCell.inner_document());
            announcer.announce().visitComplexBodyCell(tableCell);
            for (AbstractBlock block : document.getBlocks()) {
                visit(block);
            }
            announcer.announce().departComplexTableCell(tableCell);
        }
    }
}

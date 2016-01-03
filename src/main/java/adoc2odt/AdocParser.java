package adoc2odt;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.*;
import org.jruby.RubyArray;
import org.jruby.RubyObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class AdocParser {

    private final File adocFile;
    private final Map<String, Object> options;

    Announcer<AdocListener> announcer = Announcer.to(AdocListener.class);

    public AdocParser(File adocFile, Map<String, Object> options) {
        this.adocFile = adocFile;
        this.options = options;
    }

    public void addListener(AdocListener adocListener) {
        announcer.addListener(adocListener);
    }

    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public void parse() throws IOException {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        Document document=asciidoctor.load(readFile(adocFile.getAbsolutePath(), Charset.defaultCharset()), options);

        announcer.announce().visitDocument(document, adocFile.getAbsolutePath());

        ListIterator<AbstractBlock> iterator = document.getBlocks().listIterator();
        while (iterator.hasNext())
            visit(iterator.next());

        announcer.announce().departDocument(document);
    }

    private void visit(AbstractBlock block) {

        SafeNodeConverter safeNodeConverter = new SafeNodeConverter();

        if (block.getContext().equalsIgnoreCase("paragraph"))
            announcer.announce().visitParagraph((Block)block);
        else if (block.getContext().equalsIgnoreCase("section"))
            announcer.announce().visitSection((Section)block);
        else if (block.getContext().equalsIgnoreCase("ulist"))
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

        //ListIterator<AbstractBlock> iterator = block.getBlocks().listIterator();
        List<AbstractBlock> abstractBlockList = safeNodeConverter.getBlocks(block);
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
        else if (block.getContext().equalsIgnoreCase("list_item"))
            announcer.announce().departListItem((ListItem)block);
        else if (block.getContext().equalsIgnoreCase("listing"))
            announcer.announce().departListing((Block)block);
        else if (block.getContext().equalsIgnoreCase("sidebar"))
            announcer.announce().departSidebar((Block)block);


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
        for (RubyArray row : tableNode.headerRows()) {
            announcer.announce().visitTableRow();
            List<RubyObject> list = row.getList();
            for (RubyObject cell : list) {
                TableCell tableCell = (TableCell) new SafeNodeConverter().createASTNode(cell);
                announcer.announce().visitBodyCell(tableCell);
            }
            announcer.announce().departTableRow();
        }
        announcer.announce().departHeaderRows();

        for (RubyArray row : tableNode.bodyRows()) {
            announcer.announce().visitTableRow();
            List<RubyObject> list = row.getList();
            for (RubyObject cell : list) {
                TableCell tableCell = (TableCell) new SafeNodeConverter().createASTNode(cell);
                announcer.announce().visitBodyCell(tableCell);
            }
            announcer.announce().departTableRow();
        }

        announcer.announce().visitFooterRows();
        for (RubyArray row : tableNode.footerRows()) {
            announcer.announce().visitTableRow();
            List<RubyObject> list = row.getList();
            for (RubyObject cell : list) {
                TableCell tableCell = (TableCell) new SafeNodeConverter().createASTNode(cell);
                announcer.announce().visitBodyCell(tableCell);
            }
            announcer.announce().departTableRow();
        }
        announcer.announce().departFooterRows();


        announcer.announce().departTable(tableNode);
    }
}

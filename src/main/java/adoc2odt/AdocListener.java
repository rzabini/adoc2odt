package adoc2odt;

import org.asciidoctor.ast.*;

import java.util.EventListener;

public interface AdocListener extends EventListener {

    void visitDocument(Document document, String absolutePath);

    void departDocument(Document document);

    void visitParagraph(Block block);

    void departParagraph(Block block);

    void visitSection(Section section);

    void departSection(Section section);

    void visitList(ListNode block);

    void departList(ListNode block);

    void visitListItem(ListItem block);

    void departListItem(ListItem block);

    void visitListing(Block block);

    void departListing(Block block);

    void visitTable(TableNode block);

    void visitTableColumn(TableColumn tableColumn);

    void visitTableRow();

    void departTable(TableNode table);

    void departTableRow();

    void visitHeaderRows();

    void departHeaderRows();

    void visitFooterRows();

    void departFooterRows();

    void visitSidebar(Block block);

    void departSidebar(Block block);

    void visitAdmonition(Block block);

    void departAdmonition(Block block);

    void visitPreamble(Block block);

    void departPreamble(Block block);

    void departLiteral(Block block);

    void visitLiteral(Block block);

    void departThematicBreak(Block block);

    void departPageBreak(Block block);

    void visitPageBreak(Block block);

    void visitThematicBreak(Block block);

    void visitSimpleBodyCell(TableCell tableCell);

    void visitComplexBodyCell(TableCell tableCell);

    void departComplexTableCell(TableCell tableCell);
}

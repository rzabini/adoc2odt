package adoc2odt;

import org.asciidoctor.ast.*;
import org.asciidoctor.internal.RubyUtils;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyObject;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

public class SafeNodeConverter {

    private static final String TABLE_CLASS = "Asciidoctor::Table";
    private static final String TABLE_COLUMN_CLASS = "Asciidoctor::Table::Column";
    private static final String TABLE_ROWS_CLASS = "Asciidoctor::Table::Rows";
    private static final String TABLE_CELL_CLASS = "Asciidoctor::Table::Cell";

    public List<AbstractBlock> getBlocks(AbstractBlock parent) {

        List<AbstractBlock> rubyBlocks = parent.delegate().getBlocks();

        for (int i = 0; i < rubyBlocks.size(); i++) {
            Object abstractBlock = rubyBlocks.get(i);
            if (!(abstractBlock instanceof RubyArray) && !(abstractBlock instanceof AbstractNode)) {
                RubyObject rubyObject = (RubyObject) abstractBlock;
                rubyBlocks.set(i, (AbstractBlock) createASTNode(rubyObject));
            }
        }

        return rubyBlocks;
    }

    public AbstractNode createASTNode(IRubyObject rubyObject) {
        try {
            return NodeConverter.createASTNode(rubyObject);
        }
        catch (IllegalArgumentException ex){
            String rubyClassName = rubyObject.getMetaClass().getRealClass().getName();
            Ruby runtime = rubyObject.getRuntime();
            if (TABLE_CLASS.equals(rubyClassName)) {
                TableNode table = RubyUtils.rubyToJava(runtime, rubyObject, TableNode.class);
                return new TableImpl(table, runtime, this);
            }
            if (TABLE_COLUMN_CLASS.equals(rubyClassName)) {
                TableColumn column = RubyUtils.rubyToJava(runtime, rubyObject, TableColumn.class);
                return new TableColumnImpl(column, runtime);
            }
            if (TABLE_ROWS_CLASS.equals(rubyClassName)) {
                TableRows row = RubyUtils.rubyToJava(runtime, rubyObject, TableRows.class);
                return new TableRowsImpl(row, runtime);
            }
            if (TABLE_CELL_CLASS.equals(rubyClassName)) {
                TableCell cell = RubyUtils.rubyToJava(runtime, rubyObject, TableCell.class);
                return new TableCellImpl(cell, runtime);
            }
            else
                throw new IllegalArgumentException("Don't know what to do with a " + rubyObject);
            //return  new RubyAbstractBlock(rubyObject);
        }
    }

    public TableCell createTableCellASTNode(IRubyObject rubyObject) {
        return (TableCell) createASTNode(rubyObject);
    }

    public Document createDocumentASTNode(IRubyObject rubyObject) {
        return (Document)createASTNode(rubyObject);
    }
}

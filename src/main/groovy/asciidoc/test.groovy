package asciidoc

import adoc2odt.SafeNodeConverter
import adoc2odt.TableImpl
import org.asciidoctor.Asciidoctor
import org.asciidoctor.ast.AbstractBlock
import org.asciidoctor.ast.AbstractBlockImpl
import org.asciidoctor.ast.AbstractNode
import org.asciidoctor.ast.BlockImpl
import org.asciidoctor.ast.ContentPart
import org.asciidoctor.ast.Document
import org.asciidoctor.ast.DocumentRuby
import org.asciidoctor.ast.ListImpl
import org.asciidoctor.ast.ListItemImpl
import org.asciidoctor.ast.NodeConverter
import org.asciidoctor.ast.Section
import org.asciidoctor.ast.SectionImpl
import org.asciidoctor.ast.StructuredDocument
import org.jruby.Ruby
import org.jruby.RubyArray
import org.jruby.RubyObject




Asciidoctor asciidoctor = Asciidoctor.Factory.create();
Document document=asciidoctor.load(new File('src/test/resources/simple.adoc').text, [:])

println document.doctitle()
document.attributes.each {
    println "$it.key : $it.value"
}

SafeNodeConverter safeNodeConverter=new SafeNodeConverter()

safeNodeConverter.getBlocks(document).each { block ->
    visit(block, safeNodeConverter)

}

def visit(AbstractBlock block, SafeNodeConverter safeNodeConverter) {
    println '#####################'
    def context = block.context
    println context

    println '*********************'

    block.attributes.each {
        println "$it.key : $it.value"
    }

    println '----------------------'
    println block.class
    if (block instanceof BlockImpl)
        println block.convert()

    if (block instanceof ListItemImpl)
        println block.text
    if (block instanceof ListImpl)
        println block.convert()

    if (block instanceof SectionImpl) {
        println block.title
        println "level : ${block.level}"

    }

    if (block instanceof TableImpl) {
        println '^^^^^^^^^^^^^^^^^^^^^'
        println block.getBlocks().size()

    }



    safeNodeConverter.getBlocks(block).each {
        visit (it, safeNodeConverter)
    }
}

/*
Section block= document.blocks().get(1)

println block.context(
*/
package asciidoc

import org.asciidoctor.Asciidoctor
import org.asciidoctor.ast.AbstractBlock
import org.asciidoctor.ast.AbstractNode
import org.asciidoctor.ast.BlockImpl
import org.asciidoctor.ast.ContentPart
import org.asciidoctor.ast.Document
import org.asciidoctor.ast.ListImpl
import org.asciidoctor.ast.ListItemImpl
import org.asciidoctor.ast.NodeConverter
import org.asciidoctor.ast.Section
import org.asciidoctor.ast.SectionImpl
import org.asciidoctor.ast.StructuredDocument
import org.jruby.RubyArray
import org.jruby.RubyObject


Asciidoctor asciidoctor = Asciidoctor.Factory.create();
Document document=asciidoctor.load(new File('src/test/resources/table.adoc').text, [:])

/*println document.doctitle()
document.attributes.each {
    println "$it.key : $it.value"
}*/

RubyObject obj = document.delegate().getBlocks().get(0)

println obj.callMethod("convert")





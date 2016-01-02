package asciidoc

def NAME_SPACE_1 = 'urn:oasis:names:tc:opendocument:xmlns:office:1.0'

Map CONTENT_NAMESPACE_DICT = [
    //    'office:version': '1.0',
    'chart': 'urn:oasis:names:tc:opendocument:xmlns:chart:1.0',
    'dc': 'http://purl.org/dc/elements/1.1/',
    'dom': 'http://www.w3.org/2001/xml-events',
    'dr3d': 'urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0',
    'draw': 'urn:oasis:names:tc:opendocument:xmlns:drawing:1.0',
    'fo': 'urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0',
    'form': 'urn:oasis:names:tc:opendocument:xmlns:form:1.0',
    'math': 'http://www.w3.org/1998/Math/MathML',
    'meta': 'urn:oasis:names:tc:opendocument:xmlns:meta:1.0',
    'number': 'urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0',
    'office': NAME_SPACE_1,
    'ooo': 'http://openoffice.org/2004/office',
    'oooc': 'http://openoffice.org/2004/calc',
    'ooow': 'http://openoffice.org/2004/writer',
    'presentation': 'urn:oasis:names:tc:opendocument:xmlns:presentation:1.0',

    'script': 'urn:oasis:names:tc:opendocument:xmlns:script:1.0',
    'style': 'urn:oasis:names:tc:opendocument:xmlns:style:1.0',
    'svg': 'urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0',
    'table': 'urn:oasis:names:tc:opendocument:xmlns:table:1.0',
    'text': 'urn:oasis:names:tc:opendocument:xmlns:text:1.0',
    'xforms': 'http://www.w3.org/2002/xforms',
    'xlink': 'http://www.w3.org/1999/xlink',
    'xsd': 'http://www.w3.org/2001/XMLSchema',
    'xsi': 'http://www.w3.org/2001/XMLSchema-instance'
]




//xml.'office:document-content'(CONTENT_NAMESPACE_DICT)

def x = """
<root>
  <somenode1>
      <anode>foo</anode>
  </somenode1>
  <somenode2>
      <anode>bar</anode>
  </somenode2>
</root>
""".trim()
Node otherXml = new XmlSlurper().parseText(x)

Node root = new Node(null, 'pippo')
def builder = new groovy.xml.StreamingMarkupBuilder()
builder.encoding = "UTF-8"
def document = {
    mkp.xmlDeclaration()

    /*CONTENT_NAMESPACE_DICT.each { key, value ->
        mkp.declareNamespace("$key" : value)
    }*/

    document(){
        mkp.yield otherXml
    }

/*    person(id:100){
        firstname("Jane")
        lastname("Doe")
        mkp.yieldUnescaped(comment)
        location.address("123 Main")
    }*/
}
//def writer = new FileWriter("person.xml")
println builder.bind(document)
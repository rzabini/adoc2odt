package asciidoc

import org.jdom2.Document
import org.jdom2.input.SAXBuilder
import org.jdom2.transform.JDOMResult
import org.jdom2.transform.JDOMSource

import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamSource
import java.nio.charset.StandardCharsets

String sourceText="<div class=\"paragraph\">\n" +
        "<p>An introduction to <a href=\"http://asciidoc.org\">AsciiDoc</a>.</p>\n" +
        "</div>"

String xslt = ''

SAXBuilder saxBuilder = new SAXBuilder();
InputStream xmlStream = new ByteArrayInputStream(sourceText.getBytes(StandardCharsets.UTF_8));

return saxBuilder.build(xmlStream);

JDOMSource source = new JDOMSource(new Document());

InputStream stream = new ByteArrayInputStream(xslt.getBytes(StandardCharsets.UTF_8));
Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(stream));

JDOMResult out = new JDOMResult();

// perform the transformation
transformer.transform(source, out);
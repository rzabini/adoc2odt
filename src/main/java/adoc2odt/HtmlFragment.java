package adoc2odt;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HtmlFragment {

    private final String content;

    public HtmlFragment(String content) {
        this.content = content;
    }


    Element toValidXmlElement()  {
        String validContent = content.replaceAll ("(<img[^>]*)>", "$1 />");
        validContent = validContent.replaceAll ("(<br[^>]*)>", "$1 />");

        SAXBuilder saxBuilder = new SAXBuilder();
        InputStream xmlStream = new ByteArrayInputStream(validContent.getBytes(StandardCharsets.UTF_8));
        try {
            return saxBuilder.build(xmlStream).getRootElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

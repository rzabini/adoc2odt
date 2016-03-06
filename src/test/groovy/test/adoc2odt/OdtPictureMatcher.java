package test.adoc2odt;

import adoc2odt.Manifest;
import org.apache.commons.io.input.NullInputStream;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

import javax.xml.transform.Source;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class OdtPictureMatcher extends OdtMatcher {

    private final List<String> pictures;
    private final JAXPXPathEngine engine;

    public OdtPictureMatcher(List<String> pictures) {
        this.pictures = pictures;
        engine = new JAXPXPathEngine();
        engine.setNamespaceContext(
                new HashMap<String, String>() {{
                    put("manifest", Manifest.NAMESPACE_ID);
                }}
        );
    }

    public static OdtPictureMatcher hasPictures(List<String> pictures) {
        return new OdtPictureMatcher(pictures);
    }

    @Override
    protected boolean matchesSafely(File fileToMatch) {
        try  {
            return check(fileToMatch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private boolean check(File fileToMatch) throws IOException {
        ZipFile file = new ZipFile(fileToMatch);
        for (String picture : pictures) {
            final String relativePath = String.format("Pictures/%s", picture);
            InputStream inputStream = getInputStream(file, relativePath);
            if (inputStream == NOT_FOUND) {
                reason = String.format("file not found: %s", picture);
                return false;
            }

            Source source = Input.from(getInputStream(file, "META-INF/manifest.xml")).build();
            Iterable<Node> fileEntries = engine.selectNodes(
                    String.format("//manifest:file-entry[@manifest:full-path='%s']", relativePath), source);

            if (!fileEntries.iterator().hasNext()) {
                reason = String.format("manifest entry not found for %s", relativePath);
                return false;
            }

        }
        file.close();
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a file containing ").appendValue(pictures.toArray());
    }
}

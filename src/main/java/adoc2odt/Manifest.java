package adoc2odt;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;

import java.util.ArrayList;
import java.util.List;

public class Manifest {

    public static final String DEFAULT_MEDIA_TYPE = "image/png";
    private final List<String> fileEntries = new ArrayList<String>();
    private final Document document;
    public static final String NAMESPACE_ID = "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0";

    public Manifest(Document document) {
        this.document = document;
    }

    public void addFileEntry(String fileEntry) {
        if (!exists(fileEntry))
            fileEntries.add(fileEntry);
    }

    public void toDocument() {
        /*Document document = new Document();

        Namespace manifestNamespace = Namespace.getNamespace("manifest", "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\"");
        Element root = new Element("manifest", manifestNamespace);
        document.addContent(root);

        root.addContent(createFileEntryManifestElement("/", "application/vnd.oasis.opendocument.text"));
        root.addContent(createFileEntryManifestElement("styles.xml"));
        root.addContent(createFileEntryManifestElement("content.xml"));
        root.addContent(createFileEntryManifestElement("meta.xml"));
        root.addContent(createFileEntryManifestElement("settings.xml"));


        for (String fileEntry : fileEntries)
            root.addContent(createFileEntryManifestElement(String.format("%s", fileEntry), DEFAULT_MEDIA_TYPE));
        return document;*/
    }

    private Element createFileEntryManifestElement(String fileEntryFullPath) {
        return createFileEntryManifestElement(fileEntryFullPath, "text/xml");
    }

    private Element createFileEntryManifestElement(String fileEntryFullPath, String mediaType) {

        final Namespace namespace = document.getRootElement().getNamespace("manifest");
        Element fileEntry = new Element("file-entry", namespace);
        fileEntry.setAttribute("full-path", fileEntryFullPath, namespace);
        fileEntry.setAttribute("media-type", mediaType, namespace);
        return fileEntry;
    }

    public boolean exists(String fileRelativePath) {
        return fileEntries.contains(fileRelativePath);
    }

    String write(XMLOutputter xo) {
        for (String fileEntry : fileEntries)
            document.getRootElement().addContent(createFileEntryManifestElement(String.format("%s", fileEntry), DEFAULT_MEDIA_TYPE));

        return xo.outputString(document);
    }
}

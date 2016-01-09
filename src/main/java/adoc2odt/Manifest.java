package adoc2odt;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.ArrayList;
import java.util.List;

public class Manifest {

    public static final String DFAULT_MEDIA_TYPE = "image/png";
    private final List<String> fileEntries = new ArrayList<String>();

    public void addFileEntry(String fileEntry) {
        fileEntries.add(fileEntry);
    }

    public Document toDocument() {
        Document document = new Document();

        Namespace manifestNamespace = Namespace.getNamespace("manifest", "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\"");
        Element root = new Element("manifest", manifestNamespace);
        document.addContent(root);

        root.addContent(createFileEntryManifestElement("/", "application/vnd.oasis.opendocument.text"));
        root.addContent(createFileEntryManifestElement("styles.xml"));
        root.addContent(createFileEntryManifestElement("content.xml"));
        root.addContent(createFileEntryManifestElement("meta.xml"));
        root.addContent(createFileEntryManifestElement("settings.xml"));


        for (String fileEntry : fileEntries)
            root.addContent(createFileEntryManifestElement(String.format("%s", fileEntry), DFAULT_MEDIA_TYPE));
        return document;
    }

    private Element createFileEntryManifestElement(String fileEntryFullPath) {
        return createFileEntryManifestElement(fileEntryFullPath, "text/xml");
    }

    private Element createFileEntryManifestElement(String fileEntryFullPath, String mediaType) {
        Namespace manifestNamespace = Namespace.getNamespace("manifest", "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\"");

        Element fileEntry = new Element("file-entry", manifestNamespace);
        fileEntry.setAttribute("full-path", fileEntryFullPath, manifestNamespace);
        fileEntry.setAttribute("media-type", mediaType, manifestNamespace);
        return fileEntry;
    }

}

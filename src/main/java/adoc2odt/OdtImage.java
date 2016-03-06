package adoc2odt;

import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;

public class OdtImage extends OdtElement {
    private final Element htmlElement;
    private final File basePath;
    private static final double DEFAULT_IMAGE_RATIO_144_PPI = 28.34 * 2;


    public OdtImage(Element rootElement, Element htmlElement, File basePath) {
        super(rootElement);
        this.htmlElement = htmlElement;
        this.basePath = basePath;
    }

    public Element toOdtElement() {

        String imageFilePath = htmlElement.getAttributeValue("src");
        File imageFile = new File( basePath, imageFilePath);
        ImageInfo imageInfo = getImageInfo(imageFile);

        Element frame = createOdtElement("frame", "draw");
        frame.setAttribute(createOdtAttribute("name", "Image1", "draw"));
        frame.setAttribute(createOdtAttribute("z-index", "0", "draw"));

        frame.setAttribute(createOdtAttribute("width", getImageSizeInCm(imageInfo.getWidth()), "svg"));
        frame.setAttribute(createOdtAttribute("height",  getImageSizeInCm(imageInfo.getHeight()), "svg"));
        frame.setAttribute(createOdtAttribute("anchor-type", "char", "text"));
        frame.setAttribute(createOdtAttribute("style-name", "adoc-image", "draw"));


        Element image = createOdtElement("image", "draw");
        image.setAttribute(createOdtAttribute("type", "simple", "xlink"));
        image.setAttribute(createOdtAttribute("show", "embed", "xlink"));
        image.setAttribute(createOdtAttribute("actuate", "onLoad", "xlink"));

        frame.addContent(image);

        image.setAttribute(createOdtAttribute("href", String.format("Pictures/%s",
                imageFile.getName()) ,"xlink"));
        return frame;
    }

    private String getImageSizeInCm(int size) {
        return String.format("%scm", Double.toString (size/ DEFAULT_IMAGE_RATIO_144_PPI)).replace(',', '.');
    }

    private ImageInfo getImageInfo(File imageFile)  {
        try {
            return Sanselan.getImageInfo(imageFile);
        } catch (ImageReadException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Element createOdtElement(String name, String namespace) {
        return new Element(name, rootElement.getNamespace(namespace));
    }

}

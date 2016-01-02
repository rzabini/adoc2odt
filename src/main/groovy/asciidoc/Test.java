package asciidoc;

import java.io.IOException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;

public class Test {

    public static void main(String args[]) throws JDOMException, IOException {
        Namespace ns = Namespace.getNamespace("url");
        Namespace ns2 = Namespace.getNamespace("ns2", "url2");
        Namespace ns3 = Namespace.getNamespace("ns3", "url3");


        Document vDocument = new Document();
        org.jdom2.Element test = new org.jdom2.Element("test", ns2);
        vDocument.setRootElement(test);
        //add "url" default namespace
        test.addNamespaceDeclaration(ns);
        test.addNamespaceDeclaration(ns2);
        test.addNamespaceDeclaration(ns3);
        test.addContent("Some text");
        //dump output to System.out
        XMLOutputter xo = new XMLOutputter();
        xo.output(vDocument, System.out);

    }
}
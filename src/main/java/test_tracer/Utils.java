package test_tracer;


import org.w3c.dom.Node;

import javax.swing.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Vector;


/**
 * Created by joao-carloto on 18/02/2015.
 * <p/>
 * Miscellaneous utilities to print debug information, open web pages, etc.
 */

public class Utils {

    public static void printActNode(ActivityNode actNode) {
        System.out.println("Type: " + actNode.type);
        System.out.println("Name: " + actNode.name);
        System.out.println("Documentation: " + actNode.documentation);
        System.out.println("ID: " + actNode.id);
        System.out.println("Owner ID: " + actNode.ownerId);
        if (actNode.ownedIds != null) {
            for (String ownedId : actNode.ownedIds) {
                System.out.println("Owned Node: " + ownedId);
            }
        }
        if (actNode.outTransitions != null) {
            for (OutTransition outT : actNode.outTransitions) {
                System.out.println("Out Node: " + outT.outNodeId +
                        " | Name: " + outT.name +
                        " | Type: " + outT.type +
                        " | Guard: " + outT.guard +
                        " | Visits: " + outT.visits +
                        " | Dry: " + outT.dry +
                        " | Loop Entry: " + outT.loopEntry);
            }
        }
        if (actNode.inTransitions != null) {
            for (InTransition inT : actNode.inTransitions) {
                System.out.println("In Node: " + inT.inNodeId +
                        " | Visits: " + inT.visits);
            }
        }
    }

    public static void printPaths(Vector<Vector<String>> activityPaths, Map<String, ActivityNode> actNodeMap) {
        System.out.println("\n\nNumber of Activity Paths: " + activityPaths.size());
        for (int i = 0; i < activityPaths.size(); i++) {
            Vector<String> activityPath = activityPaths.get(i);
            System.out.println("\n\n\nActivity Path " + (i + 1));
            for (String nodeId : activityPath) {
                System.out.println("\n");
                ActivityNode node = actNodeMap.get(nodeId);
                Utils.printActNode(node);
            }
        }
    }

    public static void printActNodeMap(Map<String, ActivityNode> actNodeMap) {
        System.out.println("\nActivity Node Map:");
        for (Map.Entry<String, ActivityNode> entry : actNodeMap.entrySet()) {
            System.out.println("\n");
            ActivityNode actNode = entry.getValue();
            printActNode(actNode);
        }
    }

    public static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            JOptionPane.showMessageDialog(null, "nodeToString Transformer Exception", "Warning",
                    JOptionPane.ERROR_MESSAGE);
        }
        return sw.toString();
    }

    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Open Web Page Exception",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Open Web Page Exception",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

}
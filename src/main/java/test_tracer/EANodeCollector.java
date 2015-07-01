package test_tracer;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import javax.swing.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.table.*;

import static test_tracer.Utils.*;


/**
 * Created by joao-carloto on 23/03/2015.
 * <p/>
 * Collects information from an activity diagram and creates activity nodes.
 * Activity diagram is included in XMI file exported form Enterprise Architect.
 * XMI file is obtained by exporting an EA package (folder).
 * If more than one activity diagram is included in the package we must select one by name.
 */


//TODO check if elements are on diagram and not invisible
//TODO colet nodes based on transitions? Discard isolated nodes?

public class EANodeCollector {

    public static Node getActivityDiagram(Document doc, TracerForm mainForm) {
        try {
            String activityDiagramXPath = ".//UML:Diagram[@diagramType=\"ActivityDiagram\"]";
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            xpath.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    return prefix.equals("UML") ? "omg.org/UML1.3" : null;
                }

                public Iterator<?> getPrefixes(String val) {
                    return null;
                }

                public String getPrefix(String uri) {
                    return null;
                }
            });

            XPathExpression expr = xpath.compile(activityDiagramXPath);
            NodeList activityDiagramNL = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            if (activityDiagramNL.getLength() == 0) {
                JOptionPane.showMessageDialog(null, "No activity diagrams were found on the package", "Warning",
                        JOptionPane.INFORMATION_MESSAGE);
                return null;
            } else if (activityDiagramNL.getLength() == 1) {
                return activityDiagramNL.item(0);
            } else {
                DiagramSelectorForm selectForm = new DiagramSelectorForm();
                selectForm.mainForm = mainForm;
                selectForm.doc = doc;
                selectForm.table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                DefaultTableModel model = (DefaultTableModel) selectForm.table1.getModel();
                for (int i = 0; i < activityDiagramNL.getLength(); i++) {
                    Node diagramNode = activityDiagramNL.item(i);
                    Object[] row = new Object[2];
                    Node diagramNameNode = diagramNode.getAttributes().getNamedItem("name");
                    if (diagramNameNode != null) {
                        // diagramNames.add(diagramNameNode.getNodeValue());
                        row[0] = diagramNameNode.getNodeValue();
                    } else {
                        row[0] = "Namless Diagram";
                    }
                    row[1] = diagramNode;
                    model.insertRow(0, row);
                }
                TableColumn nodeColumnn = selectForm.table1.getColumnModel().getColumn(1);
                //TODO implement better solution
                nodeColumnn.setMinWidth(0);
                nodeColumnn.setMaxWidth(0);
                nodeColumnn.setPreferredWidth(0);
                return null;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.toString(), "Exception in main",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
    }

    public static void collectNodes(
            Document doc,
            Node diagramNode,
            Map<String, ActivityNode> activityNodeMap
    ) {
        String diagramElementSubjectXPath = ".//UML:DiagramElement/@subject";
        String documentationXPath = ".//UML:TaggedValue[@tag='documentation']/@value";
        String ownerXPath = ".//UML:TaggedValue[@tag='owner']/@value";
        String transTypeXPath = ".//UML:TaggedValue[@tag='ea_type']/@value";
        String transGuardXPath = ".//UML:Transition.guard//UML:BooleanExpression/@body";

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                return prefix.equals("UML") ? "omg.org/UML1.3" : null;
            }

            public Iterator<?> getPrefixes(String val) {
                return null;
            }

            public String getPrefix(String uri) {
                return null;
            }
        });

        try {
            XPathExpression expr = xpath.compile(diagramElementSubjectXPath);
            NodeList diagramElementIDsNL = (NodeList) expr.evaluate(diagramNode, XPathConstants.NODESET);

            if (diagramElementIDsNL.getLength() == 0) {
                JOptionPane.showMessageDialog(null, "No elements were found in diagram.", "Warning",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            HashMap<String, Node> nodeNodeMap = new HashMap<String, Node>();
            HashMap<String, Node> transitionNodeMap = new HashMap<String, Node>();

            for (int i = 0; i < diagramElementIDsNL.getLength(); i++) {
                String id = diagramElementIDsNL.item(i).getNodeValue();
                expr = xpath.compile(".//*[@xmi.id='" + id + "']");
                NodeList elementNL = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                if (elementNL.getLength() == 0) {
                    JOptionPane.showMessageDialog(null, "ID in diagram not found on the package. ID: " + id +
                                    "\nDoes this diagram contain elements form another package pasted as a link?", "Warning",
                            JOptionPane.WARNING_MESSAGE);
                    continue;
                } else if (elementNL.getLength() > 1) {
                    JOptionPane.showMessageDialog(null,
                            "More than one element was found on the package with the following ID: ." + id,
                            "Warning", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                if (elementNL.item(0).getNodeName().equals("UML:Transition")) {
                    transitionNodeMap.put(id, elementNL.item(0));
                } else {
                    nodeNodeMap.put(id, elementNL.item(0));
                }
            }

            for (Map.Entry<String, Node> nodeEntry : nodeNodeMap.entrySet()) {
                String id = nodeEntry.getKey();
                Node node = nodeEntry.getValue();
                String type = getActNodeType(node, id);

                if (type == null) {
                    JOptionPane.showMessageDialog(null, "Couldn't get the node type for node ID " + id, "Warning",
                            JOptionPane.INFORMATION_MESSAGE);
                    break;
                }
                //TODO do something with the comments
                else if (type.equals("Comment")) {
                    continue;
                }
                ActivityNode actNode = new ActivityNode();
                actNode.id = id;
                actNode.type = type;
                NamedNodeMap nNodeAttributes = node.getAttributes();

                //Get name
                if (nNodeAttributes.getNamedItem("name") != null) {
                    actNode.name = nNodeAttributes.getNamedItem("name").getNodeValue();
                }

                //Get documentation
                expr = xpath.compile(documentationXPath);
                NodeList docNL = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
                if (docNL.getLength() == 1) {
                    actNode.documentation = docNL.item(0).getNodeValue();
                }

                //Get out and in transitions
                for (Map.Entry<String, Node> transEntry : transitionNodeMap.entrySet()) {
                    Node tNode = transEntry.getValue();
                    NamedNodeMap tNodeAttributes = tNode.getAttributes();
                    String source = tNodeAttributes.getNamedItem("source").getNodeValue();
                    String target = tNodeAttributes.getNamedItem("target").getNodeValue();

                    if (source.equals(id)) {
                        OutTransition outT = new OutTransition();
                        outT.outNodeId = target;
                        //Get transition type
                        expr = xpath.compile(transTypeXPath);
                        NodeList transTypeNL = (NodeList) expr.evaluate(tNode, XPathConstants.NODESET);
                        if (transTypeNL.getLength() == 1) {
                            outT.type = transTypeNL.item(0).getNodeValue();
                        }
                        //Get transition name
                        Node transNameNode = tNodeAttributes.getNamedItem("name");
                        if (transNameNode != null) {
                            outT.name = transNameNode.getNodeValue();
                        }
                        //Get transition guard
                        expr = xpath.compile(transGuardXPath);
                        NodeList tGuardNL = (NodeList) expr.evaluate(tNode, XPathConstants.NODESET);
                        if (tGuardNL.getLength() == 1) {
                            outT.guard = tGuardNL.item(0).getNodeValue();
                        }
                        actNode.outTransitions.add(outT);
                    }
                    if (target.equals(id)) {
                        InTransition inT = new InTransition();
                        inT.inNodeId = source;
                        actNode.inTransitions.add(inT);
                    }
                }
                //Get owner
                expr = xpath.compile(ownerXPath);
                NodeList ownerNL = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
                if (ownerNL.getLength() == 1) {
                    actNode.ownerId = ownerNL.item(0).getNodeValue();
                }
                activityNodeMap.put(id, actNode);
            }

            //Get owned and clean absent owner
            for (HashMap.Entry<String, ActivityNode> entry : activityNodeMap.entrySet()) {
                String id = entry.getKey();
                ActivityNode actNode = entry.getValue();
                if (actNode.ownerId != null) {
                    ActivityNode nodeOwner = activityNodeMap.get(actNode.ownerId);
                    //Owner might not be collected e.g. Use case
                    if (nodeOwner != null) {
                        nodeOwner.ownedIds.add(id);
                    } else {
                        actNode.ownerId = null;
                    }
                }
            }

            //Establish implicit connections for owned objects
            for (HashMap.Entry<String, ActivityNode> entry : activityNodeMap.entrySet()) {
                String id = entry.getKey();
                ActivityNode actNode = entry.getValue();

                if (actNode.type.equals("ObjectNode") || actNode.type.equals("ActionPin")) {
                    if (actNode.ownerId != null) {
                        if (actNode.inTransitions.size() == 0) {
                            InTransition inT = new InTransition();
                            inT.inNodeId = actNode.ownerId;
                            actNode.inTransitions.add(inT);
                            OutTransition outT = new OutTransition();
                            outT.outNodeId = actNode.id;
                            outT.type = "ControlFlow";
                            activityNodeMap.get(actNode.ownerId).outTransitions.add(outT);
                        }
                        if (actNode.outTransitions.size() == 0) {
                            OutTransition outT = new OutTransition();
                            outT.outNodeId = actNode.ownerId;
                            outT.type = "ControlFlow";
                            actNode.outTransitions.add(outT);
                            InTransition inT = new InTransition();
                            inT.inNodeId = actNode.id;
                            activityNodeMap.get(actNode.ownerId).inTransitions.add(inT);
                        }
                    }
                }
                if (actNode.ownerId != null) {
                    ActivityNode nodeOwner = activityNodeMap.get(actNode.ownerId);
                    nodeOwner.ownedIds.add(id);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.toString(), "Exception:",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static String getActNodeType(Node node, String id) {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        //TODO review this
        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                return prefix.equals("UML") ? "omg.org/UML1.3" : null;
            }

            public Iterator<?> getPrefixes(String val) {
                return null;
            }

            public String getPrefix(String uri) {
                return null;
            }
        });

        String stype = "";
        try {
            XPathExpression expr = xpath.compile(".//UML:TaggedValue[@tag='ea_stype']/@value");
            NodeList stypeNL = (NodeList) expr.evaluate(node, XPathConstants.NODESET);

            if (stypeNL.getLength() == 0) {
                JOptionPane.showMessageDialog(null, "No stype found for pseudostate node, " + id, "Warning",
                        JOptionPane.INFORMATION_MESSAGE);
                return null;
            } else if (stypeNL.getLength() > 1) {
                JOptionPane.showMessageDialog(null,
                        "More than one stype was found for pseudostate node with the following ID: " + id,
                        "Warning", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
            stype = stypeNL.item(0).getNodeValue();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.toString(), "Exception:",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        if (node.getNodeName().equals("UML:ActionState")) {
            if (stype.equals("Action")) {
                return "Action";
            } else if (stype.equals("Activity")) {
                return "Activity";
            } else if (stype.equals("ActivityPartition")) {
                return "ActivityPartition";
            } else if (stype.equals("InterruptibleActivityRegion")) {
                return "InterruptibleActivityRegion";
            }
        } else if (node.getNodeName().equals("UML:PseudoState")) {
            if (stype.equals("StateNode")) {
                try {
                    XPathExpression expr = xpath.compile(".//UML:TaggedValue[@tag='ea_ntype']/@value");
                    NodeList ntypeNL = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
                    if (ntypeNL.getLength() == 0) {
                        JOptionPane.showMessageDialog(null, "No ntype found for StateNode, " + id, "Warning",
                                JOptionPane.INFORMATION_MESSAGE);
                        return null;
                    } else if (ntypeNL.getLength() > 1) {
                        JOptionPane.showMessageDialog(null,
                                "More than one ntype was found for StateNode with the following ID: " + id,
                                "Warning", JOptionPane.INFORMATION_MESSAGE);
                        return null;
                    }
                    String ntype = ntypeNL.item(0).getNodeValue();
                    if (ntype.equals("100")) {
                        return "Initial";
                    } else if (ntype.equals("101")) {
                        return "Final";
                    } else if (ntype.equals("102")) {
                        return "FlowFinal";
                    } else {
                        JOptionPane.showMessageDialog(null, "The type of node was not recognized. " +
                                        "This tool is only a proof of concept and the necessary code was not yet implemented. " +
                                        "If you feel it should have been, contact João Carloto \n\n" +
                                        nodeToString(node),
                                "Unrecognized ntype for StateNode", JOptionPane.INFORMATION_MESSAGE);
                        return null;
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.toString(), "Exception:",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } else if (stype.equals("Decision")) {
                return "Decision";
            } else if (stype.equals("MergeNode")) {
                return "Merge";
            } else if (stype.equals("Synchronization")) {
                return "ForkOrJoin";
            } else {
                JOptionPane.showMessageDialog(null, "The type of node was not recognized. " +
                                "This tool is only a proof of concept and the necessary code was not yet implemented. " +
                                "If you feel it should have been, contact João Carloto \n\n" +
                                nodeToString(node),
                        "Unrecognized stype for PseudoState", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
        } else if (node.getNodeName().equals("UML:ClassifierRole")) {
            if (stype.equals("Object")) {
                try {
                    XPathExpression expr = xpath.compile(".//UML:Stereotype[@name='datastore']");
                    NodeList dataStoreNL = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
                    if (dataStoreNL.getLength() == 1) {
                        return "DataStore";
                    } else {
                        return "Object";
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.toString(), "Exception:",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } else if (stype.equals("ObjectNode")) {
                return "ObjectNode";
            } else if (stype.equals("ActionPin")) {
                return "ActionPin";
            } else {
                JOptionPane.showMessageDialog(null, "The type of node was not recognized. " +
                                "This tool is only a proof of concept and the necessary code was not yet implemented. " +
                                "If you feel it should have been, contact João Carloto \n\n" +
                                nodeToString(node),
                        "Unrecognized type for ClassifierRole", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
        } else if (node.getNodeName().equals("UML:Event")) {
            return "Event";
        } else if (node.getNodeName().equals("UML:Class")) {
            if (stype.equals("CentralBufferNode")) {
                return "CentralBufferNode";
            }
        }
        //TODO do something with the different types of Comments
        else if (node.getNodeName().equals("UML:Comment")) {
            return "Comment";
        }
        JOptionPane.showMessageDialog(null, "The type of node was not recognized. " +
                        "This tool is only a proof of concept and the necessary code was not yet implemented. " +
                        "If you feel it should have been, contact João Carloto \n\n" +
                        nodeToString(node),
                "Unrecognized type of node", JOptionPane.INFORMATION_MESSAGE);
        return null;
    }
}

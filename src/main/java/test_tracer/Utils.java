package test_tracer;


import org.w3c.dom.Node;

import javax.swing.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;


/**
 * Created by joao-carloto on 18/02/2015.
 * <p/>
 * Miscellaneous utilities to help trace tests, print debug information, etc.
 */

public class Utils {

    public static Vector<Vector<String>> removeDuplicateLoops(Vector<Vector<String>> activityPaths, Map<String, ActivityNode> actNodeMap) {
        for (int i = 0; i < activityPaths.size(); i++) {
            Vector<String> activityPath = activityPaths.get(i);
            Vector<Vector<String>> loops = getLoops(activityPath, actNodeMap);
            for (Vector<String> aloop : loops) {
                String loopInString = "";
                for (String loopElement : aloop) {
                    loopInString += loopElement + "\n";
                }
                for (int k = i + 1; k < activityPaths.size(); k++) {
                    Vector<String> pathToClean = activityPaths.get(k);
                    String pathInString = "";
                    for (String pathElement : pathToClean) {
                        pathInString += pathElement + "\n";
                    }
                    String regString = "(?s)" + loopInString + ".*" + loopInString;
                    pathInString = pathInString.replaceAll(regString, loopInString);
                    Vector<String> cleanPath = new Vector<String>(Arrays.asList(pathInString.split("\\n")));
                    activityPaths.set(k, cleanPath);
                }
            }
        }
        return activityPaths;
    }

    public static Vector<Vector<String>> getLoops(Vector<String> path, Map<String, ActivityNode> actNodeMap) {
        Vector<Vector<String>> loops = new Vector<Vector<String>>();
        Vector<String> loop = new Vector<String>();
        for (int j = 0; j < path.size(); j++) {
            String id = path.get(j);
            Integer nextOccurrenceIndex = path.indexOf(id, j + 1);
            //This node is in a loop
            if (nextOccurrenceIndex != -1) {
                for (; j < path.size() - 1; j++) {
                    if (path.indexOf(path.get(j + 1), j + 2) == -1) {
                        break;
                    }
                    nextOccurrenceIndex = path.indexOf(path.get(j + 1), j + 2);
                }
                id = path.get(j);
                ActivityNode loopEntryNode = actNodeMap.get(id);
                String loopEntryNodeOutId = path.get(j + 1);
                for (OutTransition outT : loopEntryNode.outTransitions) {
                    if (outT.outNodeId.equals(loopEntryNodeOutId)) {
                        outT.loopEntry = true;
                    }
                }
                while (j < nextOccurrenceIndex) {
                    id = path.get(j);
                    loop.add(id);
                    j++;
                }
                j--;
                if (loop.size() != 0) {
                    loops.add(loop);
                    loop = new Vector<String>();
                }
            }
        }
        return loops;
    }

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

    public static void cleanVisits(Map<String, ActivityNode> actNodeMap) {
        for (Map.Entry<String, ActivityNode> entry : actNodeMap.entrySet()) {
            Vector<OutTransition> outTransitions = entry.getValue().outTransitions;
            for (OutTransition outT : outTransitions) {
                outT.visits = 0;
                outT.dry = false;
            }
            Vector<InTransition> inTransitions = entry.getValue().inTransitions;
            for (InTransition inT : inTransitions) {
                inT.visits = 0;
            }
        }
    }

    public static void join(ActivityNode joinNode, String previousNodeId) {
        for (InTransition inT : joinNode.inTransitions) {
            if (inT.inNodeId.equals(previousNodeId)) {
                inT.visits++;
                break;
            }
        }
    }

    public static Boolean allJoined(ActivityNode joinNode) {
        for (InTransition inT : joinNode.inTransitions) {
            if (inT.visits == 0) {
                return false;
            }
        }
        return true;
    }

    public static Integer getNumTrans(Map<String, ActivityNode> actNodeMap) {
        Integer numMapTrans = 0;
        for (Map.Entry<String, ActivityNode> entry : actNodeMap.entrySet()) {
            numMapTrans += entry.getValue().outTransitions.size();
        }
        return numMapTrans;
    }

    public static void printActNodeMap(Map<String, ActivityNode> actNodeMap) {
        System.out.println("\nActivity Node Map:");
        for (Map.Entry<String, ActivityNode> entry : actNodeMap.entrySet()) {
            System.out.println("\n");
            ActivityNode actNode = entry.getValue();
            printActNode(actNode);
        }
    }

    public static ActivityNode getInitialActNode(Map<String, ActivityNode> actNodeMap) {
        Vector<ActivityNode> initialNodes = new Vector<ActivityNode>();
        for (Map.Entry<String, ActivityNode> entry : actNodeMap.entrySet()) {
            ActivityNode actNode = entry.getValue();
            if (actNode.type.equals("VirtualInitial")) {
                return actNode;
            } else if (actNode.type.equals("Initial") ||
                    (actNode.inTransitions.size() == 0 &&
                            //TODO review strategy
                            !isIndirectlyInConnected(actNode, actNodeMap)
                    )) {
                initialNodes.add(entry.getValue());
            }
        }
        if (initialNodes.size() == 1) {
            return initialNodes.get(0);
        } else if (initialNodes.size() == 0) {
            return null;
        } else {
            ActivityNode virtualInitial = new ActivityNode();
            virtualInitial.type = "VirtualInitial";
            virtualInitial.id = "VirtualInitial";
            virtualInitial.outTransitions = new Vector<OutTransition>();
            for (ActivityNode initialNode : initialNodes) {
                OutTransition outT = new OutTransition();
                outT.type = "ControlFlow";
                outT.outNodeId = initialNode.id;
                virtualInitial.outTransitions.add(outT);
            }
            virtualInitial.inTransitions = new Vector<InTransition>();
            actNodeMap.put("VirtualInitial", virtualInitial);
            return virtualInitial;
        }
    }

    public static Boolean isIndirectlyInConnected(ActivityNode node, Map<String, ActivityNode> actNodeMap) {
        return ownerIsInConnected(node, actNodeMap) || aOwnedIsInConnected(node, actNodeMap);
    }

    //TODO make recursive
    public static Boolean ownerIsInConnected(ActivityNode node, Map<String, ActivityNode> actNodeMap) {
        if (node.ownerId == null) {
            return false;
        } else {
            ActivityNode ownerNode = actNodeMap.get(node.ownerId);
            return (ownerNode != null && ownerNode.inTransitions.size() > 0);
        }
    }

    //TODO make recursive
    public static Boolean aOwnedIsInConnected(ActivityNode node, Map<String, ActivityNode> actNodeMap) {
        if (node.ownedIds.size() == 0) {
            return false;
        } else {
            for (String ownedId : node.ownedIds) {
                ActivityNode ownedNode = actNodeMap.get(ownedId);
                if (ownedNode.inTransitions.size() > 0) {
                    return true;
                }
            }
            return false;
        }
    }

    public static OutTransition selectOutTransition(ActivityNode node) {
        OutTransition selOutT = null;
        if (node.outTransitions.size() == 1) {
            selOutT = node.outTransitions.get(0);
        } else if (node.outTransitions.size() > 1) {
            OutTransition minOutT = node.outTransitions.get(0);
            for (OutTransition outT : node.outTransitions) {
                //TODO review
                if (
                        outT.visits < minOutT.visits ||
                                (outT.visits.equals(minOutT.visits) && (minOutT.dry && !outT.dry)) ||
                                (outT.visits.equals(minOutT.visits) && (outT.loopEntry && !minOutT.loopEntry))
                        ) {
                    minOutT = outT;
                }
            }
            selOutT = minOutT;
        }
        return selOutT;
    }

    public static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            JOptionPane.showMessageDialog(null, "nodeToString Transformer Exception", "warning", JOptionPane.ERROR_MESSAGE);
        }
        return sw.toString();
    }
}
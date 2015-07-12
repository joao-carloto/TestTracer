package test_tracer;

import javax.swing.*;
import java.util.*;


/**
 * Created by joao-carloto on 26/02/2015.
 * <p/>
 * Traces the relevant test cases form the activity node map representing the activity diagram
 */


public class TestTracer {

    public static Integer numMapTrans;
    public static Integer numTraveledTrans;

    // TODO check if test number matches cyclomatic complexity
    static Vector<Vector<String>> createActivityPaths(Map<String, ActivityNode> actNodeMap) {

        Vector<Vector<String>> activityPaths = new Vector<Vector<String>>();
        Deque<String> stack = new ArrayDeque<String>();
        ActivityNode initNode = getInitialActNode(actNodeMap);

        if (initNode == null) {
            JOptionPane.showMessageDialog(null, "Couldn't find an initial node.", "Diagram Inconsistency",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        stack.push(initNode.id);
        ActivityNode node = initNode;

        //TODO review stop condition
        TestTracer.numMapTrans = getNumTrans(actNodeMap);
        TestTracer.numTraveledTrans = 0;
        cleanVisits(actNodeMap);

        while (true) {
            //TODO is this a possible scenario?
            if (node == null) return activityPaths;
            if (node.type.equals("Final")) {
                backTrack(stack, actNodeMap, activityPaths);
                if (numMapTrans.equals(numTraveledTrans)) break;
                node = initNode;
                stack.push(initNode.id);
            } else if (node.type.equals("FlowFinal")) {
                node = getPreceedingFork(stack, actNodeMap);
            } else if (node.type.equals("ForkOrJoin") && node.outTransitions.size() > 1) {
                processFork(actNodeMap, stack, node.id);
                node = actNodeMap.get(stack.peek());
            } else {
                OutTransition selOutT = selectOutTransition(node);
                if (selOutT == null) {
                    JOptionPane.showMessageDialog(null, "Node without exits but not Final: " + node.name,
                            "Diagram Inconsistency", JOptionPane.WARNING_MESSAGE);
                    return activityPaths;
                }
                if (selOutT.visits == 0) {
                    numTraveledTrans++;
                }
                selOutT.visits++;
                ActivityNode selOutNode = actNodeMap.get(selOutT.outNodeId);
                stack.push(selOutNode.id);
                node = selOutNode;
            }
        }
        return activityPaths;
    }

    private static void backTrack(Deque<String> stack,
                                  Map<String, ActivityNode> actNodeMap,
                                  Vector<Vector<String>> activityPaths) {
        Vector<String> activityPath = new Vector<String>();
        String lastNodeID = null;
        Boolean dry = false;
        while (stack.size() > 0) {
            String nodeId = stack.poll();
            activityPath.add(nodeId);
            ActivityNode node = actNodeMap.get(nodeId);
            if (!dry && node.outTransitions.size() > 1 && !node.type.equals("ForkJoin")) {
                for (OutTransition outT : node.outTransitions) {
                    if (outT.outNodeId.equals(lastNodeID)) {
                        outT.dry = true;
                        dry = true;
                    }
                }
            }
            lastNodeID = nodeId;
        }
        Collections.reverse(activityPath);
        activityPaths.add(activityPath);
    }

    private static ActivityNode getPreceedingFork(Deque<String> stack, Map<String, ActivityNode> actNodeMap) {
        ActivityNode node = null;
        String lastNodeID = null;
        for (String nodeId : stack) {
            node = actNodeMap.get(nodeId);
            if (node != null && node.type.equals("ForkOrJoin") && node.outTransitions.size() > 1) {
                for (OutTransition outT : node.outTransitions) {
                    if (outT.outNodeId.equals(lastNodeID)) {
                        outT.dry = true;
                    }
                }
                return node;
            }
            lastNodeID = nodeId;
        }
        return node;
    }

    private static void processFork(
            Map<String, ActivityNode> actNodeMap,
            Deque<String> stack,
            String forkId) {

        ActivityNode forkNode = actNodeMap.get(forkId);
        OutTransition selOutT = selectOutTransition(forkNode);
        if (selOutT.visits == 0) {
            numTraveledTrans++;
        }
        selOutT.visits++;
        String previousNodeId = forkId;
        ActivityNode node = actNodeMap.get(selOutT.outNodeId);

        while (true) {
            if (!node.type.equals("ForkOrJoin")) {
                stack.push(node.id);
                if (node.type.equals("Final") || node.type.equals("FlowFinal")) {
                    return;
                } else {
                    selOutT = selectOutTransition(node);
                    if (selOutT == null) {
                        JOptionPane.showMessageDialog(null, "Node without exits but not Final: " + node.name,
                                "Diagram Inconsistency", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (selOutT.visits == 0) {
                        numTraveledTrans++;
                    }
                    selOutT.visits++;
                    ActivityNode selOutNode = actNodeMap.get(selOutT.outNodeId);
                    previousNodeId = node.id;
                    node = selOutNode;
                }
            } else {
                join(node, previousNodeId);
                if (allJoined(node)) {
                    stack.push(node.id);
                    if (node.outTransitions.size() > 1) {
                        processFork(actNodeMap, stack, node.id);
                    }
                    return;
                }
                selOutT = selectOutTransition(forkNode);
                if (selOutT.visits == 0) {
                    numTraveledTrans++;
                }
                selOutT.visits++;
                previousNodeId = node.id;
                node = actNodeMap.get(selOutT.outNodeId);
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

    public static Vector<Vector<String>> removeDuplicateLoops(Vector<Vector<String>> activityPaths, Map<String,
            ActivityNode> actNodeMap) {
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

}

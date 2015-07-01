package test_tracer;

import javax.swing.*;
import java.util.*;

import static test_tracer.Utils.*;

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
}

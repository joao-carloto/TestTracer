package test_tracer;

import java.util.Map;
import java.util.Vector;

/**
 * Created by joao-carloto on 26/02/2015.
 * <p/>
 * Once the test cases a are traced as sequences of activity nodes,
 * they are written into a more human readable form.
 * It uses the name, documentation and other relevant information form each node.
 * In some cases adds it's own text, e.g. how to handle fork and join situations.
 */

public class TestWriter {

    //TODO sketchy strategy due to preserving paths only has IDs
    public static Boolean isInterrupt(String nodeId, String nextNodeId, Map<String, ActivityNode> actNodeMap) {
        ActivityNode node = actNodeMap.get(nodeId);
        OutTransition nextOutT = null;
        for (OutTransition outT : node.outTransitions) {
            if (outT.outNodeId.equals(nextNodeId)) {
                nextOutT = outT;
                break;
            }
        }
        return nextOutT != null && nextOutT.type.equals("InterruptFlow");
    }

    public static Vector<Vector<String>> writeTests(Vector<Vector<String>> activityPaths, Map<String,
            ActivityNode> actNodeMap) {
        Vector<Vector<String>> tests = new Vector<Vector<String>>();
        Vector<String> forkJoinVerifications = new Vector<String>();

        for (Vector<String> activityPath : activityPaths) {
            Vector<String> test = new Vector<String>();
            for (int j = 0; j < activityPath.size(); j++) {
                String step = "";
                String nodeId = activityPath.get(j);
                ActivityNode node = actNodeMap.get(nodeId);
                ActivityNode nodeOwner;

                if (j < activityPath.size() - 1) {
                    String nextNodeId = activityPath.get(j + 1);
                    if (isInterrupt(nodeId, nextNodeId, actNodeMap) &&
                            node.ownerId != null &&
                            actNodeMap.get(node.ownerId).type.equals("InterruptibleActivityRegion")
                            ) {
                        Vector<ActivityNode> interruptibleNodes = new Vector<ActivityNode>();
                        for (Map.Entry<String, ActivityNode> entry : actNodeMap.entrySet()) {
                            if (node.ownerId.equals(entry.getValue().ownerId) &&
                                    entry.getValue().type.equals("Action") &&
                                    !entry.getValue().id.equals(nodeId)
                                    ) {
                                interruptibleNodes.add(entry.getValue());
                            }
                        }
                        if (interruptibleNodes.size() > 0) {
                            step += "Verify that each one of the following actions can be interrupted by a " +
                                    node.name;
                            if (node.documentation != null) {
                                step += "(" + node.documentation + ")";
                            }
                            step += ": ";
                            for (ActivityNode interruptibleNode : interruptibleNodes) {
                                step += interruptibleNode.name + ", ";
                            }
                            step = step.substring(0, step.length() - 2);
                            step += ". Execute the following steps after the interrupt.";
                            test.add(step);
                            continue;
                        }
                    }
                }

                if (node.type.equals("Initial")) {
                    if (node.name != null) step += node.name + ".";
                    if (node.documentation != null) step += node.documentation;
                    if (!step.equals("")) test.add(step);
                } else if (node.type.equals("Action") ||
                        node.type.equals("Activity") ||
                        node.type.equals("CentralBufferNode") ||
                        node.type.equals("DataStore")
                        ) {
                    if (node.ownerId != null) {
                        nodeOwner = actNodeMap.get(node.ownerId);
                        //TODO can be inside an activity and inside a partition
                        if (nodeOwner.type.equals("ActivityPartition")) {
                            step += "As a " + nodeOwner.name + ", ";
                        }
                        if (nodeOwner.type.equals("Activity")) {
                            step += "In the context of a " + nodeOwner.name + ", ";
                        }
                    }
                    step += node.name;
                    if (node.documentation != null) {
                        step += ". " + node.documentation;
                    } else {
                        step += ".";
                    }
                    if (!step.equals("")) test.add(step);
                } else if (node.type.equals("Object")) {
                    if (j > 0) {
                        String prevNodeId = activityPath.get(j - 1);
                        ActivityNode prevNode = actNodeMap.get(prevNodeId);
                        step += "In the context of the previous " + prevNode.type;
                        if (prevNode.name != null) {
                            step += " (" + prevNode.name + ") a ";
                        }
                    } else {
                        step += "A ";
                    }
                    if (node.name != null && !node.name.equals("")) {
                        step += node.name;
                    }
                    step += " object is created ";
                    if (j < activityPath.size()) {
                        String subsequentNodeId = activityPath.get(j + 1);
                        ActivityNode subsequentNode = actNodeMap.get(subsequentNodeId);

                        step += " and sent to ";

                        if (subsequentNode.name != null &&
                                !subsequentNode.name.equals("")) {
                            step += subsequentNode.name;
                        } else {
                            step += " the next " + subsequentNode.type;
                        }
                        step += ".";
                        if (!step.equals("")) test.add(step);
                    }
                } else if (node.type.equals("ObjectNode") ||
                        node.type.equals("ActionPin")
                        ) {
                    if (node.ownerId != null) {
                        nodeOwner = actNodeMap.get(node.ownerId);
                        if (nodeOwner.type.equals("Activity")) {
                            step += "In the context of " + nodeOwner.name + " a ";
                        } else {
                            step += "A ";
                        }
                        if (node.name != null) {
                            step += node.name;
                        }
                        step += " is created";
                    }
                    String subsequentNodeId = activityPath.get(j + 1);
                    ActivityNode subsequentNode = actNodeMap.get(subsequentNodeId);
                    if ((subsequentNode.type.equals("ObjectNode") || subsequentNode.type.equals("ActionPin")) &&
                            subsequentNode.name.equals(node.name) &&
                            subsequentNode.ownerId != null) {
                        j++;
                        ActivityNode subsequentNodeOwner = actNodeMap.get(subsequentNode.ownerId);
                        step += " and sent to " + subsequentNodeOwner.name;
                    }
                    if (!step.equals("")) test.add(step);
                } else if (node.type.equals("ForkOrJoin")) {
                    if (node.name != null) step += node.name + ".";
                    if (node.documentation != null) step += node.documentation;
                    if (!step.equals("")) test.add(step);
                    step = "";

                    if (node.inTransitions.size() > 1) {
                        //TODO and if they are not actions?
                        step += "Verify that you must ";
                        Vector<ActivityNode> previousActionNodes = getPreviousActions(node, j, activityPath, actNodeMap);
                        for (ActivityNode actionNode : previousActionNodes) {
                            step += actionNode.name + " and ";
                        }
                        step = step.substring(0, step.length() - 5);
                        step += ", before you can ";
                        for (OutTransition outT : node.outTransitions) {
                            ActivityNode outNode = actNodeMap.get(outT.outNodeId);
                            if (outNode.name != null) {
                                step += outNode.name + " or ";
                            } else if (outNode.type.equals("Final")) {
                                step += "Finish    ";
                            } else {
                                step += outNode.type + " or ";
                            }
                        }
                        step = step.substring(0, step.length() - 4);
                        step += ".";
                        if (!step.equals("") && !forkJoinVerifications.contains(step)) {
                            forkJoinVerifications.add(step);
                            test.add(step);
                        }
                    }
                    if (node.outTransitions.size() > 1) {
                        //TODO and if they are not actions?
                        step = "";
                        step += "Verify that the following actions can be performed independently: ";
                        for (OutTransition outT : node.outTransitions) {
                            ActivityNode outNode = actNodeMap.get(outT.outNodeId);
                            step += outNode.name + ", ";
                        }
                        step = step.substring(0, step.length() - 2);
                        step += ".";
                        if (!step.equals("") && !forkJoinVerifications.contains(step)) {
                            forkJoinVerifications.add(step);
                            test.add(step);
                        }
                    }
                } else if (node.type.equals("Decision")) {
                    if (node.name != null) step += node.name + ".";
                    if (node.documentation != null) step += node.documentation + ".";
                    OutTransition tFromDecision = null;
                    //TODO sketchy strategy
                    outerloop:
                    for (int k = j + 1; k < activityPath.size(); k++) {
                        for (OutTransition outT : node.outTransitions) {
                            if (activityPath.get(k).equals(outT.outNodeId)) {
                                tFromDecision = outT;
                                break outerloop;
                            }
                        }
                    }
                    if (tFromDecision != null) {
                        if (tFromDecision.guard != null) {
                            step += "Provide conditions for a \"" + tFromDecision.guard + "\" decision.";
                        } else if (tFromDecision.name != null) {
                            step += "Provide conditions for a \"" + tFromDecision.name + "\" decision.";
                        }
                    }
                    if (!step.equals("")) test.add(step);
                } else if (node.type.equals("Event")) {
                    if (node.name != null) step += "Induce the following event: " + node.name + ".";
                    if (node.documentation != null) step += node.documentation;
                    if (!step.equals("")) test.add(step);
                } else if (node.type.equals("FlowFinal")) {
                    if (node.name != null) step += node.name + ".";
                    if (node.documentation != null) step += node.documentation;
                    if (!step.equals("")) test.add(step);
                } else if (node.type.equals("Final")) {
                    if (node.name != null) step += node.name + ".";
                    if (node.documentation != null) step += node.documentation;
                    if (!step.equals("")) test.add(step);
                }
            }
            tests.add(test);
        }
        return tests;
    }

    public static void readTests(Vector<Vector<String>> tests) {
        System.out.println("\n" + tests.size() + " Test Cases");
        for (int i = 0; i < tests.size(); i++) {
            Vector<String> test = tests.get(i);
            System.out.println("\n\nTest nº" + (i + 1) + "\n");
            for (int j = 0; j < test.size(); j++) {
                System.out.println("Step " + (j + 1) + ": " + test.get(j));
            }
        }
    }

    public static Vector<ActivityNode> getPreviousActions(ActivityNode node, int index, Vector<String> activityPath,
                                                          Map<String, ActivityNode> actNodeMap) {
        Vector<ActivityNode> previousActions = new Vector<ActivityNode>();
        for (InTransition inT : node.inTransitions) {
            String inNodeid = inT.inNodeId;
            Boolean foundInNode = false;
            for (int i = index - 1; i > 0; i--) {
                ActivityNode previousNode = actNodeMap.get(activityPath.get(i));
                if (previousNode.id.equals(inNodeid)) {
                    foundInNode = true;
                }
                if (foundInNode && (previousNode.type.equals("Action") || previousNode.type.equals("Activity"))) {
                    previousActions.add(previousNode);
                    break;
                }
            }
        }
        return previousActions;
    }
}

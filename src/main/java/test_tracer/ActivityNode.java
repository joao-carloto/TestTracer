package test_tracer;

import java.util.Vector;

/**
 * Created by joao-carloto on 07/02/2015.
 * <p/>
 * Object will store all relevant information from a diagram node.
 * Includes node attributes and relation to other nodes.
 * A collection of activity nodes, representing the activity diagram, will be used to trace the test cases.
 */

public class ActivityNode {
    String id;
    String type;
    String name;
    Vector<OutTransition> outTransitions;
    Vector<InTransition> inTransitions;
    String documentation;
    String ownerId;
    Vector<String> ownedIds;

    public ActivityNode() {
        outTransitions = new Vector<OutTransition>();
        inTransitions = new Vector<InTransition>();
        ownedIds = new Vector<String>();
    }
}

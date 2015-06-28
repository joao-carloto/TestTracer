package test_tracer;

/**
 * Created by joao-carloto on 24/02/2015.
 * <p/>
 * Represents an incoming transition to an activity node.
 * Included in a vector on the activity nodes.
 * The visits attribute is used in the Test tracer algorithm,
 * it's incremented by one unit each time the algorithm travels that transition
 */

public class InTransition {
    String inNodeId;
    Integer visits = 0;
}

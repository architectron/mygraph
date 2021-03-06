package vvakar.tree;

import java.util.TreeMap;

/**
 * A segment tree is a balanced binary tree that allows querying for local extremes (minima, maxima)
 * aka. Range (Minimum|Maximum) Queries (RMQ) in O(log(n)) time.
 * <p/>
 * The tree requires two sets of values: index positions and corresponding values. While this can be achieved
 * using arrays, note that the data can be very sparse, which may cause a lot of waste and require unnecessarily
 * large arrays. Due to that, this implementation uses a TreeMap to store the data (index -> value).
 * <p/>
 * It does not allow adding/removing elements once bootstrapped, but does updates in O(log(n)).
 * <p/>
 * Space complexity: O(2n - 1) nodes.
 *
 * @author vvakar
 *         Date: 12/13/14
 * @see <a href="http://www.geeksforgeeks.org/segment-tree-set-1-sum-of-given-range/">article</a>
 */
public abstract class AbstractSegmentTree {

    protected static final class Node {
        Node left, right, parent;
        long value = ZERO_VALUE;
        final int from, to;

        Node(Node left, Node right, Node parent, int from, int to, long v) {
            this.from = from;
            this.to = to;
            this.left = left;
            this.right = right;
            this.parent = parent;
            this.value = v;
        }
    }

    protected static final byte ZERO_VALUE = 0;
    private static final Node NULL_NODE = new Node(null, null, null, Integer.MAX_VALUE, Integer.MIN_VALUE, ZERO_VALUE);
    private final Node root;
    final TreeMap<Integer, Node> leafs; // better support sparse arrays by allocating only the strictly necessary

    /**
     * Since the tree supports only updates, it must be initialized with the list of all non-null index positions.
     *
     * @param nonEmptyPositions
     */
    public AbstractSegmentTree(int nonEmptyPositions[]) {
        this.leafs = new TreeMap<Integer, Node>();
        for (int a : nonEmptyPositions) {
            leafs.put(a, NULL_NODE);
        }

        // construction requires a sorted array of all non-empty index positions
        // there can be duplicate positions in the input!!
        int[] sortedNonemptyPositions = new int[leafs.size()];
        int count = 0;
        for (int v : leafs.keySet()) {
            sortedNonemptyPositions[count++] = v;
        }
        root = construct(leafs, sortedNonemptyPositions, 0, sortedNonemptyPositions.length - 1, null);
    }

    private Node construct(TreeMap<Integer, Node> leafs, int[] nonEmptyPositions, int from, int to, Node parent) {

        if (nonEmptyPositions.length == 0 || from > to)
            return NULL_NODE;

        int fromA = nonEmptyPositions[from];
        int toA = nonEmptyPositions[to];
        if (fromA > toA) {
            // ugh - swap
            int temp = toA;
            toA = fromA;
            fromA = temp;
        }

        if (from == to) {
            // leaf node - update map
            Node node = new Node(null, null, parent, fromA, toA, leafs.get(fromA).value);
            leafs.put(fromA, node);
            return node;
        }

        int rootIndex = (to + from) / 2;
        Node current = new Node(null, null, parent, fromA, toA, ZERO_VALUE);
        Node leftNode = construct(leafs, nonEmptyPositions, from, rootIndex, current);
        Node rightNode = construct(leafs, nonEmptyPositions, rootIndex + 1, to, current);
        current.left = leftNode;
        current.right = rightNode;
        current.value = aggregateQueryResults(leftNode.value, rightNode.value);
        return current;
    }

    /**
     * Get value found at the valid index position that occurs right before the specified one.
     * Due to sparseness, that's not necessarily index - 1.
     * Also, if index is the first and/or only available value, there will be no value before it, so
     * return the zero element.
     *
     * @param index position at which there's a valid value
     * @return value at position right before index, if available otherwise zero
     */
    public long getValueRightBeforeIndex(int index) {
        Integer keyRightBeforeThisOne = leafs.floorKey(index - 1);
        long val = ZERO_VALUE;
        if (keyRightBeforeThisOne != null) {
            val = query(leafs.firstKey(), keyRightBeforeThisOne);
        }
        return val;

    }

    /**
     * Update one value, propagating the change up the tree
     *
     * @param key key to update
     * @param val new value
     */
    public abstract void update(int key, long val);

    public long query(int from, int to) {
        return query(from, to, root);
    }

    private long query(int from, int to, Node root) {
        if (from > to || root == null) return ZERO_VALUE;
        if (from > root.to || to < root.from) return ZERO_VALUE; // out of range

        // in range - query is on or after current from, and on or before current to
        // no use further narrowing down
        if (from <= root.from && root.to <= to) {
            return root.value;
        }

        long candidateLeft = query(from, to, root.left); // narrow to the left
        long candidateRight = query(from, to, root.right);
        return aggregateQueryResults(candidateLeft, candidateRight);
    }

    protected abstract long aggregateQueryResults(long candidateLeft, long candidateRight);
}


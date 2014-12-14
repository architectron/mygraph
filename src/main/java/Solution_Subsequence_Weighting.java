import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author vvakar
 *         Date: 12/9/14
 */
public class Solution_Subsequence_Weighting {

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        final int testCases = Integer.parseInt(br.readLine().trim());  // max 5

        for(int i = 0; i < testCases; ++i) {
            final int nCount = Integer.parseInt(br.readLine().trim());  // max 150k
            String[] lineStrs = br.readLine().split("\\s+");
            int[] Ns = strArray2intArray(lineStrs);
            lineStrs = br.readLine().split("\\s+");
            int[] Ws = strArray2intArray(lineStrs);

            System.out.println(solveIt(Ns, Ws));

        }
    }

    /**
     *
     *    a =     1   2   3   4   1   2   3   4
     *    w =    10  20  30  40   15  15  15  50
     *    current = 4,50
     *
     *                                         1-4
     *                                         110
     *                                     /          \
     *                               1-2                3-4
     *                               30                 110
     *                             /   \              /     \
     *   previousState(a,w) =  1,15     2,30      3,60      4,110
     *
     *   what's the biggest w among a < a[i] in previousState? Add it to w[i]
     *   what's the
     *        candidate = 4,110
     *   is there a previousState[a] already recorded? If so, keep the value w of the two.
     *
     *
     * -----------
     * findMax(1,4, root)
     *
     *
     *        from >= root.from && to <= root.to
     * findMax(1,3) = 30      ||     findMax(3,4) = 60
     *
     *   what is previousState?
     *       - no duplicate a's  => Set
     *       - easy find by < OR ==   => SortedSet
     *       - k-v store
     *       - TreeMap?
     */
    static long solveIt(int[] As, int[]Ws) {
        long maxWeight = 0;
        Map<Integer, Long> previousState = new HashMap<Integer, Long>();
        TreeMap<Long, Integer> currentWeights = new TreeMap<Long, Integer>();

        MaxSegmentTree maxSegmentTree = new MaxSegmentTree(As);

        for(int i = 0; i < As.length; ++i)  {  // main loop
            int currentA = As[i];
            int currentW = Ws[i];

            Long existingCandidatesWeight = previousState.get(currentA);
            long maxPrevWeight = 0;

            // TODO: Optimize this loop! Can you make it log(n) time?
            // We would like:
            // Some sort of data structure that does fast summation over a range of indexes
//            for(Map.Entry<Long, Integer> entry : currentWeights.descendingMap().entrySet()) {
//                if(entry.getValue() < currentA) {
//                    maxPrevWeight = entry.getKey();
//                    break;
//                }
//            }

            maxPrevWeight = maxSegmentTree.getValueRightBeforeIndex(currentA);

            long candidateW = maxPrevWeight + currentW;
            if(existingCandidatesWeight == null || existingCandidatesWeight < candidateW) {
                previousState.put(currentA, candidateW);
                currentWeights.put(candidateW, currentA);
                maxSegmentTree.update(currentA, candidateW);
                maxWeight = Math.max(maxWeight, candidateW);

                Integer currentWithSameWeight = currentWeights.get(candidateW);
                if(currentWithSameWeight == null || currentWithSameWeight > currentA) {
                    // replace existing top weight if it's currently carried by a higher A
                    currentWeights.put(candidateW, currentA);
                }
            }
        }

        return maxWeight;
    }

    /**
     * Supports updates only
     */
    static final class MaxSegmentTree {
        private static final class Node {
            final Node left, right;
            long value;
            final int from, to;
            Node(Node left, Node right, int from, int to, long v) {
                this.from = from;
                this.to = to;
                this.left = left;
                this.right = right;
                this.value = v;
            }
        }
        private static final Node NULL_NODE = new Node(null, null, Integer.MAX_VALUE, Integer.MIN_VALUE, 0);
        private final Node root;
        final TreeMap<Integer,Long> map; // simulate sparse array

        /**
         * Since the tree supports only updates, it must be initialized with the list of all non-null index positions.
         * @param nonEmptyPositions
         */
        public MaxSegmentTree(int nonEmptyPositions[]) {
            this.map = new TreeMap<Integer, Long>();
            for(int a : nonEmptyPositions) {
                map.put(a, 0L);
            }

            // construction requires a sorted array of all non-empty index positions
            int[] sortedNonemptyPositions = new int[nonEmptyPositions.length];
            int count = 0;
            for(int v : map.keySet()) {
                sortedNonemptyPositions[count++] = v;
            }
            root = construct(map, sortedNonemptyPositions, 0, nonEmptyPositions.length - 1);
        }

        private Node construct(TreeMap<Integer,Long> map, int[] As, int from, int to) {

            if(As.length == 0 || from > to)
                return NULL_NODE;

            int fromA = As[from];
            int toA = As[to];
            if(fromA > toA) {
                // ugh - swap
                int temp = toA;
                toA = fromA;
                fromA = temp;
            }

            if(from == to) {
                return new Node(null, null, fromA, toA, map.get(As[from]));
            }

            int rootIndex = (to + from)/2;
            Node leftNode = construct(map, As, from, rootIndex);
            Node rightNode = construct(map, As, rootIndex + 1, to);
            return new Node(leftNode, rightNode, fromA, toA, Math.max(leftNode.value, rightNode.value));
        }

        /**
         * Get value found at the valid index position that occurs right before the specified one.
         * Due to sparseness, that's not necessarily index - 1.
         * Also, if index is the first and/or only available value, there will be no value before it, so
         * return the zero element.
         * @param index position at which there's a valid value
         * @return value at position right before index, if available otherwise zero
         */
        public long getValueRightBeforeIndex(int index) {
            Integer keyRightBeforeThisOne = map.floorKey(index - 1);
            long val = 0;
            if(keyRightBeforeThisOne != null) {
                val = query(map.firstKey(), keyRightBeforeThisOne);
            }
            return val;

        }
        public void update(int key, long val) {
            map.put(key, val);
            updateMax(root, key, val);
        }

        private void updateMax(Node root, int key, long maxCandidate) {
            if(root == null) return;
            if(key > root.to || key < root.from) return; // out of range

            if(root.from <= key && root.to >= key && root.value < maxCandidate) {
                root.value = maxCandidate; // in range
            }
            updateMax(root.left, key, maxCandidate);
            updateMax(root.right, key, maxCandidate);
        }

        public long query(int from, int to) {
            return findMax(from, to, root);
        }

        private long findMax(int from, int to, Node root) {
            if(from > to || root == null) return 0;
            if(from > root.to || to < root.from) return 0; // out of range

            // in range - query is on or after current from, and on or before current to
            // no use further narrowing down
            if(from <= root.from && root.to <= to) {
                return root.value;
            }

            long candidateLeft = findMax(from, to, root.left); // narrow to the left
            long candidateRight = findMax(from, to, root.right);
            return Math.max(candidateLeft, candidateRight);
        }
    }


    private static long[] strArray2longArray(String[] strs) {
        long[] ret = new long[strs.length];
        for(int i = 0; i < strs.length; ++i) {
            ret[i] = Long.parseLong(strs[i]);
        }
        return ret;
    }
    private static int[] strArray2intArray(String[] strs) {
        int[] ret = new int[strs.length];
        for(int i = 0; i < strs.length; ++i) {
            ret[i] = Integer.parseInt(strs[i]);
        }
        return ret;
    }
}

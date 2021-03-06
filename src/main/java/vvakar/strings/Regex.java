package vvakar.strings;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author vvakar
 *         Date: 1/2/15
 */
public class Regex {

    public static boolean findRegex(String str, String regex) {
        Graph graph = parseRegex(regex);

        Set<Integer> currentStates = new HashSet<Integer>();
        currentStates.add(0);

        for(int i = 0; i < str.length(); ++i) {
            Set<Integer> nextStates = new HashSet<Integer>();
            nextStates.add(0);
            char c = str.charAt(i);

            for(int currentState : currentStates) {
                Vertex currentVertex = graph.vertices.get(currentState);
                for (Map.Entry<Character, Integer> entry : currentVertex.transitions.entrySet()) {
                    char expectedChar = entry.getKey();
                    if (c == expectedChar || expectedChar == '.') {
                        nextStates.add(entry.getValue());


                        // append next epsylons immediately - free pass discovered
                        Vertex nextVertex = graph.vertices.get(entry.getValue());
                        Deque<Integer> epsylons = new ArrayDeque<Integer>(nextVertex.epsylons);
                        while(!epsylons.isEmpty()) {
                            int nextEpsylon = epsylons.poll();

                            if(!nextStates.contains(nextEpsylon)) {
                                epsylons.addAll(graph.vertices.get(nextEpsylon).epsylons);
                            }

                            nextStates.add(nextEpsylon);
                        }
                    }
                }

                // Append all current epsylons
                Deque<Integer> epsylons = new ArrayDeque<Integer>(currentVertex.epsylons);
                while(!epsylons.isEmpty()) {
                    int nextEpsylon = epsylons.poll();

                    if(!nextStates.contains(nextEpsylon)) {
                        epsylons.addAll(graph.vertices.get(nextEpsylon).epsylons);
                    }

                    nextStates.add(nextEpsylon);
                }

            }

            currentStates = nextStates;
            if(currentStates.contains(regex.length())) return true;

        }

        return false;
    }

    private static final class Vertex {
        public final int id;
        public final Map<Character, Integer> transitions = new HashMap<Character, Integer>();
        public final Set<Integer> epsylons = new HashSet<Integer>();
        Vertex(int id) { this.id = id; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex vertex = (Vertex) o;
            return id == vertex.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    /**
     *
     *     (from, input) -> to
     *
     *
     *     (0, a) -> 1
     *     (1, b) -> 2
     *     (2, c) -> c
     *     (1, e) -> 4
     *     (4, f) -> 5
     *
     */
    private static final class Graph {
        public final Map<Integer, Vertex> vertices = new HashMap<Integer, Vertex>();
        public void addTransition(int from, int to, char onInput) {
            Vertex fromVertex = vertices.getOrDefault(from, new Vertex(from));
            fromVertex.transitions.put(onInput, to);
            vertices.put(from, fromVertex);

            Vertex toVertex = vertices.getOrDefault(to, new Vertex(to));
            vertices.put(to, toVertex);

        }

        public void addEpsylonTransition(int from, int to) {
            Vertex fromVertex = vertices.getOrDefault(from, new Vertex(from));
            fromVertex.epsylons.add(to);
            vertices.put(from, fromVertex);

            Vertex toVertex = vertices.getOrDefault(to, new Vertex(to));
            vertices.put(to, toVertex);

        }
    }

    private static Graph parseRegex(String regex) {
        Graph graph = new Graph();
        Deque<Integer> stack = new ArrayDeque<Integer>();
        for(int i = 0; i < regex.length(); ++i) {
            char c = regex.charAt(i);

            boolean starLookahead = regex.length() > i + 1 && regex.charAt(i + 1) == '*';

            switch(c) {
                case '*':
                    graph.addEpsylonTransition(i, i + 1);
                    break;
                case '|':
                case '(':
                    graph.addEpsylonTransition(i, i+1);
                    stack.push(i);
                    break;
                case ')':
                    graph.addEpsylonTransition(i, i + 1);
                    int idx = stack.pop();
                    int prev = i;
                    while(regex.charAt(idx) != '(') {
                        graph.addEpsylonTransition(idx, i);
                        prev = idx;
                        idx = stack.pop();
                    }

                    if(regex.charAt(prev) == '|') {
                        // free transition from '(' to '|'
                        graph.addEpsylonTransition(idx, prev);
                    }

                    if(starLookahead) {
                        graph.addEpsylonTransition(idx, i); // '(' to ')' because it's optional
                        graph.addEpsylonTransition(i+1, idx); // '*' back to '('
                    }
                    break;
                case '.':
                default: // non-metacharacter
                    if(starLookahead) {
                        graph.addEpsylonTransition(i, i+1);
                        graph.addEpsylonTransition(i+1, i);
                    }
                    graph.addTransition(i, i + 1, c);
                    break;
            }
        }

        return graph;
    }

    private static String prettyPrintFindRegex(String str, String regex) {
        return str + "/" + regex + "/" + "  => " + findRegex(str, regex);
    }

}

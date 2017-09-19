package GeneradorLexers;/*
 * SimpleGraphView.java
 *
 * Created on March 8, 2007, 7:49 PM
 *
 * Copyright March 8, 2007 Grotto Networking
 */

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

import java.util.LinkedList;

/**
 *
 * @author Dr. Greg M. Bernstein
 */

/**
 * Esta clase fue modificada para poder aceptar el grafo generado por el regex
 */
public class SimpleGraphView2 {
    Graph<Integer, Edge> g;
    /** Creates a new instance of SimpleGraphView */
    public SimpleGraphView2(DirectedGraph automata) {
        g = new SparseMultigraph<Integer, Edge>();

        /**
         * Se va a copiar el automata a esta nueva libreria
         */
        // Obtener nodos de segundo automata
        LinkedList<DirectedGraph.NodeClass> nodos2 = automata.getAllNodes();

        // Obtener transiciones automata 2
        LinkedList<DirectedGraph.edgeContents> edges2 = automata.getEdges();

        // Copiar cada nodo a automata 1
        for (DirectedGraph.NodeClass i: nodos2) {
            g.addVertex(i.getId());
        }

        // Copiar cada transicion a automata 1
        for (DirectedGraph.edgeContents i: edges2) {
            String transition = i.getTransition();

            g.addEdge(new Edge(transition), i.getStartingNode().getId(), i.getFinishingNode().getId(), EdgeType.DIRECTED);
        }
    }

    class Edge
    {
        private final String name;

        Edge(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

}

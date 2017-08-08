import java.util.HashSet;

/**
 * La presente clase tiene como objetivo llevar a cabo el proceso de minimizacion de un dfa utilizando el algoritmo de
 * Hopcroft, se utilizo la metodologia de la tabla
 * @author Eric Mendoza
 * @version 1.0
 * @since 08/08/207
 */
public class HopcroftMinimizator {
    /**
     * Atributos
     */
    private HashSet<UnorderedPair<DirectedGraph.NodeClass>> estadosMarcados, estadosNoMarcados;

    public HopcroftMinimizator() {
        this.estadosMarcados = new HashSet<UnorderedPair<DirectedGraph.NodeClass>>();
        this.estadosNoMarcados = new HashSet<UnorderedPair<DirectedGraph.NodeClass>>();
    }

    public DirectedGraph minimizateDFA(DirectedGraph dfa){
        DirectedGraph minimizedDFA = new DirectedGraph();


        return minimizedDFA;
    }
}

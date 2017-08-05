import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * La presente clase tiene como objetivo crear un DFA a partir de un NFA
 * @author Eric Mendoza
 * @version 1.0
 * @since 3/08/207
 */
public class NFAToDFA {
    /**
     * Atributos
     */
    private HashMap<DirectedGraph.NodeClass, HashSet<DirectedGraph.NodeClass>> eClosureStates = new HashMap<DirectedGraph.NodeClass, HashSet<DirectedGraph.NodeClass>>();

    /**
     * Funcion que se encarga de guiar el algoritmo de conversion de nfa-dfa
     * @param nfa automata que se desea convertir a dfa
     * @return un dfa
     */
    public DirectedGraph evaluate(DirectedGraph nfa){
        generateSimpleEClosure(nfa);
        return null;
    }

    /**
     * Este metodo tiene como objetivo generar el e-closure de cada estado del automata nfa en primera generacion
     * @param nfa automata a utilizar
     */
    private void generateSimpleEClosure(DirectedGraph nfa) {
        // Obtener nodos de automata a procesar
        LinkedList<DirectedGraph.NodeClass> nodos = nfa.getAllNodes();

        // Guardador de estados temporal
        HashSet<DirectedGraph.NodeClass> estadosTemp;

        // Obtener cada una de las e-closure por estado
        for (DirectedGraph.NodeClass nodo: nodos) {
            estadosTemp = new HashSet<DirectedGraph.NodeClass>();
            estadosTemp = stateEClosure(nodo, estadosTemp);
            estadosTemp.add(nodo);  // Agregarse a si mismo a la cerradura
            eClosureStates.put(nodo, estadosTemp);
        }
    }

    /**
     * Devuelve el e-closure simple de un estado
     * @param nodo el estado al que se le obtendra el e-closure
     * @return retorna un conjunto con los estados a los que se puede llegar
     */
    private HashSet<DirectedGraph.NodeClass> stateEClosure(DirectedGraph.NodeClass nodo, HashSet<DirectedGraph.NodeClass> estadosTemp){
        for (DirectedGraph.edgeContents transicion: nodo.edges) {
            // Si una transicion es igual a epsilon, guardar los estados a los que me lleva
            if (transicion.getTransition().equals("!")){
                DirectedGraph.NodeClass nodoDestino = transicion.getFinishingNode();

                boolean fueAgregado = estadosTemp.add(nodoDestino);  // Agregar nodo visitado

                if (fueAgregado){
                    estadosTemp.addAll(stateEClosure(nodoDestino, estadosTemp));
                }
            }
        }

        return estadosTemp;
    }

    /**
     * Este metodo tiene como objetivo obtener el e-closure de un estado y sus estados alcanzados por epsilon
     */
    private HashSet<DirectedGraph.NodeClass> deepEClosure(HashSet<DirectedGraph.NodeClass> state){

        return new HashSet<DirectedGraph.NodeClass>();
    }


}

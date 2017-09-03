import java.util.HashSet;

/**
 * La presente clase tiene como objetivo simular un DFA o NFA
 * @author Eric Mendoza
 * @version 1.0
 * @since 5/08/207
 */
public class Simulator {

    /**
     * Constructor
     */
    public Simulator(){
    }

    public boolean simulateNFA(DirectedGraph nfa, String expr, NFAToDFA nfaToDFA){
        /*
      Atributos de simulador
     */

        // Obtener estado inicial de nfa
        DirectedGraph.NodeClass nodoInicial = nfa.getOneInicialNode();

        // Obtener nodo final de nfa
        DirectedGraph.NodeClass nodoFinal = nfa.getOneFinalNode();

        // Obtener conjunto de estados iniciales
        HashSet<DirectedGraph.NodeClass> currentStates = nfaToDFA.getEClosure(nodoInicial);
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            // Moverse hacia siguiente conjunto de estados
            currentStates = nfaToDFA.moveT(currentStates, String.valueOf(c));
        }

        return currentStates.contains(nodoFinal);
    }

    public boolean simulateDFA(DirectedGraph dfa, String expr){
        // Obtener estado inicial de dfa
        DirectedGraph.NodeClass nodoInicial = dfa.getOneInicialNode();

        // Obtener nodo final de nfa
        DirectedGraph.NodeClass nodoFinal = dfa.getOneFinalNode();

        // Fijar estado inicial
        DirectedGraph.NodeClass currentState = nodoInicial;

        // Obtener alfabeto
        HashSet<String> alphabet = dfa.getAlphabet();

        // Recorrer automata
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            String letra = String.valueOf(c);

            if (alphabet.contains(letra)){
                // Moverse hacia siguiente conjunto de estados
                currentState = move(currentState, letra);
            }

            else return false;
        }

        return currentState.isFinal();
    }

    private DirectedGraph.NodeClass move(DirectedGraph.NodeClass nodo, String symbol){
        // Analizar cada una de las transiciones del estado
        for (DirectedGraph.edgeContents transicion: nodo.edges) {
            // Si una transicion es igual a el simbolo, retornar estado
            if (transicion.getTransition().equals(symbol)){
                return transicion.getFinishingNode();
            }
        }

        return null;
    }
}

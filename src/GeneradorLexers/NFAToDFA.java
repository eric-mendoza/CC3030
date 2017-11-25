package GeneradorLexers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * La presente clase tiene como objetivo crear un DFA a partir de un NFA
 * @author Eric Mendoza
 * @version 1.0
 * @since 3/08/207
 */
public class NFAToDFA  implements Serializable {
    /**
     * Atributos
     */
    private HashMap<DirectedGraph.NodeClass, HashSet<DirectedGraph.NodeClass>> eClosureStates = new HashMap<DirectedGraph.NodeClass, HashSet<DirectedGraph.NodeClass>>();
    private LinkedList<Dstate<DirectedGraph.NodeClass>> dStates = new LinkedList<Dstate<DirectedGraph.NodeClass>>();  // Futuros estados del DFA
    private LinkedList<Dtransition> dTransitions = new LinkedList<Dtransition>();  // Futuros transiciones del DFA

    /**
     * Funcion que se encarga de guiar el algoritmo de conversion de nfa-dfa
     * @param nfa automata que se desea convertir a dfa
     * @return un dfa
     */
    public DirectedGraph convert(DirectedGraph nfa){
        generateSimpleEClosure(nfa);  // Obtener los estados que se pueden alcanzar por epsilon de un estado
        generateTransitionTable(nfa);  // Generar la tabla de transiciones para el nuevo dfa
        return generateDFA(nfa);
    }

    /**
     * Este metodo tiene como objetivo generar el e-closure de cada estado del automata y almacenarlo en una variable
     * @param nfa automata a procesar
     */
    public void generateSimpleEClosure(DirectedGraph nfa) {
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
     * Devuelve el e-closure de un estado
     * @param nodo el estado al que se le obtendra el e-closure
     * @return retorna un conjunto con los estados a los que se puede llegar
     */
    private HashSet<DirectedGraph.NodeClass> stateEClosure(DirectedGraph.NodeClass nodo, HashSet<DirectedGraph.NodeClass> estadosTemp){
        for (DirectedGraph.edgeContents transicion: nodo.edges) {
            // Si una transicion es igual a epsilon, guardar los estados a los que me lleva
            if (transicion.getTransition().equals("!")){
                DirectedGraph.NodeClass nodoDestino = transicion.getFinishingNode();

                // Intentar agregar nodo a conjunto de nodos alcanzados
                boolean fueAgregado = estadosTemp.add(nodoDestino);

                // Si se agrega, entonces visitar los estados a los que se puede llegar con dicho nodo
                if (fueAgregado){
                    estadosTemp.addAll(stateEClosure(nodoDestino, estadosTemp));
                }
            }
        }

        return estadosTemp;
    }

    /**
     * Este metodo tiene como objetivo obtener el e-closure de un conjunto de estados
     * @param set es el conjunto de estados que se desea procesar
     * @return retorna un conjunto de estados alcanzables
     */
    private HashSet<DirectedGraph.NodeClass> setEClosure(HashSet<DirectedGraph.NodeClass> set){
        HashSet<DirectedGraph.NodeClass> resultado = new HashSet<DirectedGraph.NodeClass>();
        HashSet<DirectedGraph.NodeClass> estadosAlcanzados;

        for (DirectedGraph.NodeClass nodo: set){
            estadosAlcanzados = eClosureStates.get(nodo);  // Obtener los estados a los que se llega con un estado
            resultado.addAll(estadosAlcanzados);  // Hacer la union de los estados alcanzados
        }
        return resultado;
    }

    /**
     * Este metodo tiene como objetivo obtener el conjunto de estados que se alcanzan con una entrada desde un conjunto
     * de estados
     * @param set es el conjunto de estados desde donde se analiza
     * @param symbol es la entrada que desencadena el movimiento
     * @return devuelve un conjunto de estados alcanzables
     */
    public HashSet<DirectedGraph.NodeClass> moveT(HashSet<DirectedGraph.NodeClass> set, String symbol){
        // Variable para almacenar estados
        HashSet<DirectedGraph.NodeClass> resultado = new HashSet<DirectedGraph.NodeClass>();

        // Obtener los estados alcanzados por cada nodo
        for (DirectedGraph.NodeClass nodo: set) {
            resultado.addAll(moveS(nodo, symbol));
        }
        return resultado;
    }

    /**
     * Este metodo tiene como objetivo obtener el conjunto de estados que se alcanzan desde un solo estado considera
     * las transiciones epsilon
     * @param nodo es el estado desde donde se inicia
     * @param symbol es la entrada que provoca el movimiento
     * @return devuelve un conjunto de estados alcanzables
     */
    public HashSet<DirectedGraph.NodeClass> moveS(DirectedGraph.NodeClass nodo, String symbol){
        // Variable para almacenar estados
        HashSet<DirectedGraph.NodeClass> resultado = new HashSet<DirectedGraph.NodeClass>();

        // Analizar cada una de las transiciones de los estados
        for (DirectedGraph.edgeContents transicion: nodo.edges) {
            // Si una transicion es igual a el simbolo, guardar los estados a los que me lleva
            if (transicion.getTransition().equals(symbol)){
                DirectedGraph.NodeClass nodoDestino = transicion.getFinishingNode();

                // Agregar todos los nodos alcanzados por epsilon del nodo alcanzado por a
                resultado.addAll(eClosureStates.get(nodoDestino));

            }
        }
        return resultado;
    }

    public DirectedGraph.NodeClass move(DirectedGraph.NodeClass nodo, String symbol){
        // Analizar cada una de las transiciones del estado
        for (DirectedGraph.edgeContents transicion: nodo.edges) {
            // Si una transicion es igual a el simbolo, retornar estado
            if (transicion.getTransition().equals(symbol)){
                return transicion.getFinishingNode();
            }
        }

        return null;
    }

    /**
     * Metodo que tiene como objetivo obtener todos los estados a los que se puede llegar a partir de
     *  un estado y un input.
     * @param nfa automata que se analizara para obtener resultado
     */
    private void generateTransitionTable(DirectedGraph nfa){
        // Estados iniciales de nfa (solo es uno)
        HashSet<DirectedGraph.NodeClass> nodosInicialesNfa = nfa.getInicialNode();
        DirectedGraph.NodeClass nodoInicialNfa = null;
        for (DirectedGraph.NodeClass nodo: nodosInicialesNfa) {
            nodoInicialNfa = nodo;
        }


        // Estado final de nfa
        HashSet<DirectedGraph.NodeClass> nodosFinalesNfa = nfa.getFinalNode();
        DirectedGraph.NodeClass nodoFinalNfa = null;
        for (DirectedGraph.NodeClass nodo: nodosFinalesNfa) {
            nodoFinalNfa = nodo;
        }

        // Obtener conjuntos alcanzados por nodoInicialNfa con epsilon
        HashSet<DirectedGraph.NodeClass> nodosIniciales = eClosureStates.get(nodoInicialNfa);

        // Ver si contiene el estado de aceptacion el conjunto inicial
        boolean isFinal = nodosIniciales.contains(nodoFinalNfa);

        // Crear Destado inicial con conjuntos alcanzados por estado inicial
        Dstate<DirectedGraph.NodeClass> dEstadoInicial = new Dstate<DirectedGraph.NodeClass>(nodosIniciales, true, isFinal);
        Dstate<DirectedGraph.NodeClass> estadoTemporalAgregado;

        // Agregar Destado a conjunto de Destados
        dStates.add(dEstadoInicial);

        // Obtener alphabeto de nfa
        HashSet<String> alphabeto = nfa.getAlphabet();

        // Crearn variable de estado no marcado
        Dstate<DirectedGraph.NodeClass> unmarkedState = dEstadoInicial;

        // Crear resto de Destados
        while (unmarkedState != null){
            // Marcar estado inicial ERROR
            unmarkedState.setMarked(true);

            // Procesar cada entrada del alfabeto y su resultado
            for (String input: alphabeto){
                // Obtener el conjunto de estados que se alcanzan con cierta entrada
                HashSet<DirectedGraph.NodeClass> conjuntoEstadosAlcanzados = moveT(unmarkedState.getConjuntoEstados(), input);

                // Ver si contiene el estado de aceptacion el conjunto
                isFinal = conjuntoEstadosAlcanzados.contains(nodoFinalNfa);

                // Crear GeneradorLexers.Dstate que se podria agregar
                estadoTemporalAgregado = new Dstate<DirectedGraph.NodeClass>(conjuntoEstadosAlcanzados, false, isFinal);

                // Verificar que estadoTemporalAgregado no exista en Destados
                Dstate<DirectedGraph.NodeClass> estadoTemporalEsUnico = getUniqueDstate(estadoTemporalAgregado);

                // Agregar estadoTemporalAgregado si no existe
                if (estadoTemporalEsUnico == null){
                    dStates.add(estadoTemporalAgregado);

                    // Crear Dtransicion para el estado creado
                    addDtransition(unmarkedState, estadoTemporalAgregado, input);
                }

                // Agregar transicion a estado existente
                else {
                    addDtransition(unmarkedState, estadoTemporalEsUnico, input);
                }
            }

            // Actualizar unmarkedState
            unmarkedState = existUnmarkedDstate();
        }
    }

    /**
     * Indica si existen estados sin marcar en el conjunto de estados del dfa
     * @return verdadero: si existen estados sin marcar; falso, si ya estan marcados todos los estados
     */
    private Dstate<DirectedGraph.NodeClass> existUnmarkedDstate(){
        for (Dstate<DirectedGraph.NodeClass> dEstado: dStates){
            if (!dEstado.isMarked()){
                return dEstado;
            }
        }
        return null;
    }

    /**
     * Se utiliza para crear una nueva dTransicion y anadirla al Destado
     * @param finishingState Estado al que se llega con la transicion
     * @param startingState Estado desde el que se inicia
     * @param transition Transicion que provoca el cambio
     */
    private void addDtransition(Dstate<DirectedGraph.NodeClass> startingState, Dstate<DirectedGraph.NodeClass> finishingState, String transition){
        Dtransition nuevaTransicion = new Dtransition(finishingState, startingState, transition);
        dTransitions.add(nuevaTransicion);
        startingState.addTransitions(nuevaTransicion);
    }

    /**
     * Verifica si un estado ya exite en Dstates, si ya existe, devuelve dicho estado
     * @param dstate estado a analizar
     * @return null, si no existe; dstate si existe
     */
    private Dstate<DirectedGraph.NodeClass> getUniqueDstate(Dstate<DirectedGraph.NodeClass> dstate){
        for (Dstate<DirectedGraph.NodeClass> dstate2: dStates){
            // Si ya existe el estado, devolver dicho estado
            if (dstate2.equals(dstate)){
                return dstate2;
            }
        }

        // Si no existe estado, devolver el estado ingresado
        return null;
    }

    private DirectedGraph generateDFA(DirectedGraph nfa){
        DirectedGraph dfa = new DirectedGraph();
        dfa.setAlphabet(nfa.getAlphabet());
        int contador = 0;
        HashMap<Dstate<DirectedGraph.NodeClass>, Integer> convertDstateToState = new HashMap<Dstate<DirectedGraph.NodeClass>, Integer>(dStates.size(), (float) 1.0);

        // Crear nodo con cada dstate
        for (Dstate<DirectedGraph.NodeClass> dstate: dStates){
            // Agregar nuevo nodo a dfa
            dfa.addNode(dfa, contador, dstate.isdStateInitial(), dstate.isdStateFinal());

            // Guardar equivalente en mapa
            convertDstateToState.put(dstate, contador);

            // Aumentar contador
            contador++;
        }

        // Agregar las transiciones
        for (Dtransition dtransition: dTransitions){
            // Obtener nodo inicial de transicion
            int nodoInicial = convertDstateToState.get(dtransition.getStartingState());

            // Obtener nodo final de transicion
            int nodoFinal = convertDstateToState.get(dtransition.getFinishingState());

            // Crear nueva transicion
            dfa.addEdges(dfa, dfa.getParticularNode(nodoInicial), dfa.getParticularNode(nodoFinal), dtransition.getTransition());
        }

        return dfa;
    }

    /**
     * Devuelve el e-Closure de un estado
     * @param nodo estado a analizar
     * @return conjunto de estados alcanzados
     */
    public HashSet<DirectedGraph.NodeClass> getEClosure(DirectedGraph.NodeClass nodo){
        return eClosureStates.get(nodo);
    }
}

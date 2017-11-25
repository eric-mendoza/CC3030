package GeneradorLexers;

import java.io.Serializable;
import java.util.*;

/**
 * La presente clase tiene como objetivo llevar a cabo el proceso de minimizacion de un dfa utilizando el algoritmo de
 * Hopcroft, se utilizo la metodologia de la tabla
 * @author Eric Mendoza
 * @version 1.0
 * @since 08/08/207
 */
public class HopcroftMinimizator  implements Serializable {
    /**
     * Atributos
     */
    private HashSet<HashSet<DirectedGraph.NodeClass>> estadosMarcados;
    private HashSet<HashSet<DirectedGraph.NodeClass>> estadosNoMarcados;
    private NFAToDFA funciones;
    private LinkedList<DirectedGraph.NodeClass> estadosNoAnalizados;
    private LinkedList<Dstate<DirectedGraph.NodeClass>> dStates = new LinkedList<Dstate<DirectedGraph.NodeClass>>();  // Futuros estados del DFA
    private LinkedList<Dtransition> dTransitions = new LinkedList<Dtransition>();  // Futuros transiciones del DFA
    private HashSet<String> alfabeto;

    /**
     * Constructor de la clase
     */
    public HopcroftMinimizator() {
        this.estadosNoMarcados = new HashSet<HashSet<DirectedGraph.NodeClass>>();
        this.estadosMarcados = new HashSet<HashSet<DirectedGraph.NodeClass>>();
        funciones = new NFAToDFA();
    }

    /**
     * Metodo que tiene como objetivo el desarrollo correcto del algoritmo de Hopcroft
     * @param dfa el automata que se quiere minizar
     * @return un dfa minimo
     */
    public DirectedGraph minimizateDFA(DirectedGraph dfa){
        DirectedGraph minimizedDFA = new DirectedGraph();
        alfabeto = dfa.getAlphabet();
        estadosNoAnalizados = dfa.getAllNodes();
        createFirstPairs(estadosNoAnalizados);
        evaluateAllPairs();
        mergeStates(dfa);
        createDTransitions(dfa);
        minimizedDFA = generateDFA(dfa);

        return minimizedDFA;
    }

    /**
     * Metodo que tiene como objetivo crear las transiciones entre los nuevos estados creados
     * @param dfa automata del que se utilizaran las transiciones
     */
    private void createDTransitions(DirectedGraph dfa) {

        for (Dstate<DirectedGraph.NodeClass> dEstado: dStates) {
            for (String input: alfabeto) {
                // Obtener cualquiera de los estados del conjunto de estados
                DirectedGraph.NodeClass nodo = dEstado.getConjuntoEstados().iterator().next();

                // Obtener hacia donde se dirige
                DirectedGraph.NodeClass siguienteNodo = funciones.move(nodo, input);

                // Ver a que dState se dirige
                for (Dstate<DirectedGraph.NodeClass> siguienteDEstado: dStates) {
                    HashSet<DirectedGraph.NodeClass> estados = siguienteDEstado.getConjuntoEstados();
                    if (estados.contains(siguienteNodo)){
                        dTransitions.add(new Dtransition(siguienteDEstado, dEstado, input));

                        // Terminar ciclo
                        break;
                    }
                }
            }
        }
    }

    /**
     * Metodo que se encarga de unir los estados que son indistinguibles
     * @param dfa proveera el alfabeto
     */
    private void mergeStates(DirectedGraph dfa) {
        boolean completado = false;
        while (!completado){
            int contador = 0;
            // Indica que estados eliminar de los conjuntos al final
            LinkedList<HashSet<DirectedGraph.NodeClass>> estadosPorEliminar = new LinkedList<HashSet<DirectedGraph.NodeClass>>();

            for (HashSet<DirectedGraph.NodeClass> par: estadosNoMarcados) {
                // Crear un nuevo conjunto de estados equivalentes
                HashSet<DirectedGraph.NodeClass> equivalentStates = new HashSet<DirectedGraph.NodeClass>();
                Iterator<DirectedGraph.NodeClass> elementos = par.iterator();
                DirectedGraph.NodeClass estado1 = elementos.next();
                DirectedGraph.NodeClass estado2 = elementos.next();

                equivalentStates.add(estado1);
                equivalentStates.add(estado2);

                estadosNoAnalizados.remove(estado1);
                estadosNoAnalizados.remove(estado2);
                estadosNoMarcados.remove(par);  // Eliminar par agregado
                contador++;

                // Verificar si existe algun otro par equivalente
                for (HashSet<DirectedGraph.NodeClass> par2: estadosNoMarcados) {
                    if (semiEquals(par, par2)){
                        // Obtener nodos
                        elementos = par2.iterator();
                        estado1 = elementos.next();
                        estado2 = elementos.next();

                        // Agregar a estados equivalentes
                        equivalentStates.add(estado1);
                        equivalentStates.add(estado2);

                        // Eliminar de estados no analizados
                        estadosNoAnalizados.remove(estado1);
                        estadosNoAnalizados.remove(estado2);

                        // Guardar par a eliminar al final de iteraciones
                        estadosPorEliminar.add(par2);
                        contador++;
                    }

                }

                // Crear nuevo dState
                // Ver si es inicial
                boolean isFinal = false;
                for (DirectedGraph.NodeClass nodo :equivalentStates) {
                    isFinal = isFinal || nodo.isFinal();
                }
                dStates.add(new Dstate<DirectedGraph.NodeClass>(equivalentStates, equivalentStates.contains(dfa.getOneInicialNode()), isFinal));


                // Parar loop debido a que se elimino un par
                if (contador != 0) break;
            }

            // Eliminar pares listados
            for (HashSet<DirectedGraph.NodeClass> parAEliminar: estadosPorEliminar) {
                estadosNoMarcados.remove(parAEliminar);
            }

            // Verificar si se realizaron cambios
            completado = contador == 0;
        }

        // Crear dStates con los estados no analizados
        for (DirectedGraph.NodeClass estadoNoAnalizado: estadosNoAnalizados) {
            HashSet<DirectedGraph.NodeClass> nodos = new HashSet<DirectedGraph.NodeClass>();
            nodos.add(estadoNoAnalizado);
            dStates.add(new Dstate<DirectedGraph.NodeClass>(nodos, estadoNoAnalizado.isStart(), estadoNoAnalizado.isFinal()));
        }
    }

    /**
     * Metodo que tiene como objetivo evaluar cada uno de los pares creados por el metodo anterior y marcar los que son
     * distinguibles
     */
    private void evaluateAllPairs() {
        boolean seMarcoUnEstado = true;  // Bandera para indicar que se realizo un cambio
        while (seMarcoUnEstado){
            // Contador para verificar se se marco algun estado en la iteracion
            int contadorMarcados = 0;

            // Analizar el siguiente paso de cada par no marcado para cada entrada del alfabeto
            for (HashSet<DirectedGraph.NodeClass> par: estadosNoMarcados) {

                for (String input: alfabeto) {
                    // Obtener los siguientes estados por cada entrada
                    Iterator<DirectedGraph.NodeClass> elementos = par.iterator();
                    DirectedGraph.NodeClass estado1 = elementos.next();
                    DirectedGraph.NodeClass estado2 = elementos.next();
                    DirectedGraph.NodeClass nodoSiguiente1 = funciones.move(estado1, input);
                    DirectedGraph.NodeClass nodoSiguiente2 = funciones.move(estado2, input);

                    // Crear el nuevo par
                    HashSet<DirectedGraph.NodeClass> siguientePar = new HashSet<DirectedGraph.NodeClass>();
                    siguientePar.add(nodoSiguiente1);
                    siguientePar.add(nodoSiguiente2);

                    // Verificar si el nuevo par esta marcado
                    boolean isMarcado = estadosMarcados.contains(siguientePar);
                    if (isMarcado){
                        // Agregar el par a lo estados marcados
                        estadosMarcados.add(par);

                        // Eliminar de estados no marcados
                        estadosNoMarcados.remove(par);

                        // Parar loop para este par
                        contadorMarcados++;
                        break;
                    }
                }

                if (contadorMarcados != 0) break;
            }
            // Verificar si se marco un estado para terminar iteraciones
            seMarcoUnEstado = contadorMarcados != 0;
        }
    }

    /**
     * Metodo que tiene como objetiivo crear los pares no-ordenados iniciales de la tabla diferenciando entre estados
     * de aceptacion y el resto.
     */
    private void createFirstPairs(LinkedList<DirectedGraph.NodeClass> estados) {
        for (int i = 0; i < estados.size() - 1; i++) {
            DirectedGraph.NodeClass estado1 = estados.get(i);

            for (int j = i + 1; j < estados.size(); j++) {
                DirectedGraph.NodeClass estado2 = estados.get(j);
                HashSet<DirectedGraph.NodeClass> par = new HashSet<DirectedGraph.NodeClass>();
                par.add(estado1);
                par.add(estado2);

                // Marcar los que son distinguibles desde inicio
                if (estado1.isFinal() ^ estado2.isFinal()){
                    estadosMarcados.add(par);
                } else {
                    estadosNoMarcados.add(par);
                }
            }
        }
    }


    private DirectedGraph generateDFA(DirectedGraph dfa){
        DirectedGraph minimalDFA = new DirectedGraph();
        HashSet<String> alfabetoNuevo = alfabeto;
        minimalDFA.setAlphabet(alfabetoNuevo);
        int contador = 0;
        HashMap<Dstate<DirectedGraph.NodeClass>, Integer> convertDstateToState = new HashMap<Dstate<DirectedGraph.NodeClass>, Integer>(dStates.size(), (float) 1.0);

        // Crear nodo con cada dstate
        for (Dstate<DirectedGraph.NodeClass> dstate: dStates){
            // Agregar nuevo nodo a dfa
            minimalDFA.addNode(minimalDFA, contador, dstate.isdStateInitial(), dstate.isdStateFinal());

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
            minimalDFA.addEdges(minimalDFA, minimalDFA.getParticularNode(nodoInicial), minimalDFA.getParticularNode(nodoFinal), dtransition.getTransition());
        }

        return minimalDFA;
    }

    public boolean semiEquals(HashSet<DirectedGraph.NodeClass> nodo1, HashSet<DirectedGraph.NodeClass> nodo2) {
        Iterator<DirectedGraph.NodeClass> elementos1 = nodo1.iterator();
        Iterator<DirectedGraph.NodeClass> elementos2 = nodo2.iterator();

        // Primer elemento
        DirectedGraph.NodeClass firstE = elementos1.next();
        DirectedGraph.NodeClass firstE2 = elementos2.next();

        // Comparar primeros
        if (firstE.compareTo(firstE2) == 0) return true;  // Comparar 1 con 1
        DirectedGraph.NodeClass secondE2 = elementos2.next();
        if (firstE.compareTo(secondE2) == 0) return true;  // Comparar 1 con 2

        // Comparar 2 con 1
        DirectedGraph.NodeClass secondE = elementos1.next();
        // Comparar 1 con 1
        return secondE.compareTo(firstE2) == 0 || secondE.compareTo(secondE2) == 0;
    }
}

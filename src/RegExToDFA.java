import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;

/**
 * La presente clase tiene como objetivo crear un dfa a partir de un regex
 * @author Eric Mendoza
 * @version 1.0
 * @since 11/08/207
 */
public class RegExToDFA {
    /**
     * Atributos
     */
    private BinaryTree arbolSintactico;
    private HashMap<Integer, BinaryTree> leafNodes;
    private HashSet<String> alfabeto = new HashSet<String>();
    private Integer posicionAceptacion;
    private int positionCounter;
    private LinkedList<Dstate<Integer>> dStates = new LinkedList<Dstate<Integer>>();  // Futuros estados del DFA
    private LinkedList<Dtransition> dTransitions = new LinkedList<Dtransition>();  // Futuras transiciones del DFA

    /**
     * Constructor
     */
    public RegExToDFA() {
        this.leafNodes = new HashMap<Integer, BinaryTree>();
        posicionAceptacion = 0;
    }

    /**
     * Metodo que tiene como objetivo guiar el algoritmo de construccion directa del dfa
     * @param regex expresion regular desde la que se construira el dfa
     * @return un automata finito determinista
     */
    public DirectedGraph createDFA(String regex){
        arbolSintactico = generateSyntaxTree(regex);  // Crear arbol sintacticao
        setPropiedadesArbolSintactico(arbolSintactico);  // Configura firstPos, lastPos y nullable
        createDTransitions();
        return generateDFA();
    }


    /**
     * Metodo que tiene como objetivo construir el dfa utilizando las propiedades calculadas anteriormente
     * @return un dfa
     */
    private DirectedGraph generateDFA() {
        DirectedGraph dfa = new DirectedGraph();
        dfa.setAlphabet(alfabeto);
        int contador = 0;
        HashMap<Dstate<Integer>, Integer> convertDstateToState = new HashMap<Dstate<Integer>, Integer>(dStates.size(), (float) 1.0);

        // Crear nodo con cada dstate
        for (Dstate<Integer> dstate: dStates){
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
     * Metodo que tiene como objetivo crear las DTransiciones y DEstados del automata
     */
    private void createDTransitions() {
        // Crear Destado inicial con firstpos(raiz)
        HashSet<Integer> posicionesIniciales = arbolSintactico.getFirstPos();
        boolean isFinal = posicionesIniciales.contains(posicionAceptacion);
        Dstate<Integer> dEstadoInicial = new Dstate<Integer>(posicionesIniciales, true, isFinal);
        Dstate<Integer> estadoTemporalAgregado;

        // Agregar Destado a conjunto de Destados
        dStates.add(dEstadoInicial);

        // Obtener alphabeto de nfa
        HashSet<String> alphabeto = alfabeto;

        // Crear variable de estado no marcado
        Dstate<Integer> unmarkedState = dEstadoInicial;

        // Crear resto de Destados
        while (unmarkedState != null){
            // Marcar estado inicial
            unmarkedState.setMarked(true);

            // Procesar cada entrada del alfabeto y su resultado
            for (String input: alphabeto){
                // Obtener el conjunto de estados que se alcanzan con cierta entrada
                HashSet<Integer> conjuntoEstadosAlcanzados = moveT(unmarkedState.getConjuntoEstados(), input);

                // Ver si contiene la posicion de aceptacion el conjunto nuevo
                isFinal = conjuntoEstadosAlcanzados.contains(posicionAceptacion);

                // Crear Dstate que se podria agregar
                estadoTemporalAgregado = new Dstate<Integer>(conjuntoEstadosAlcanzados, false, isFinal);

                // Verificar que estadoTemporalAgregado no exista en Destados
                Dstate<Integer> estadoTemporalEsUnico = getUniqueDstate(estadoTemporalAgregado);

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
    private Dstate<Integer> existUnmarkedDstate(){
        for (Dstate<Integer> dEstado: dStates){
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
    private void addDtransition(Dstate<Integer> startingState, Dstate<Integer> finishingState, String transition){
        Dtransition nuevaTransicion = new Dtransition(finishingState, startingState, transition);
        dTransitions.add(nuevaTransicion);
        startingState.addTransitions(nuevaTransicion);
    }


    /**
     * Verifica si un estado ya exite en Dstates, si ya existe, devuelve dicho estado
     * @param estadoTemporalAgregado estado a analizar
     * @return null, si no existe; dstate si existe
     */
    private Dstate<Integer> getUniqueDstate(Dstate<Integer> estadoTemporalAgregado) {
        for (Dstate<Integer> dstate2: dStates){
            // Si ya existe el estado, devolver dicho estado
            if (dstate2.equals(estadoTemporalAgregado)){
                return dstate2;
            }
        }

        // Si no existe estado, devolver el estado ingresado
        return null;
    }

    /**
     * Metodo que tiene como objetivo obtener el conjunto destino de un conjunto dado un input
     * @param conjuntoEstados es el conjunto desde el que se parte
     * @param input es la entrada del alfabeto
     * @return un conjunto de estados destino
     */
    private HashSet<Integer> moveT(HashSet<Integer> conjuntoEstados, String input) {
        // Conjunto resultado
        HashSet<Integer> resultado = new HashSet<Integer>();
        BinaryTree nodoTemporal;

        // Ver cuales elementos corresponden al input y obtener su followPos
        for (Integer posicion: conjuntoEstados) {
            // Obtener el correspondiente nodo
            nodoTemporal = leafNodes.get(posicion);
            String valorNodo = nodoTemporal.getValue();

            // Si corresponde, agregar el siguientePos al conjunto resultado
            if (valorNodo.equals(input)){
                resultado.addAll(nodoTemporal.getFollowPos());
            }
        }

        return resultado;
    }


    /**
     * Metodo que tiene como objetivo setear lastpos, followPos, firstpos y nullable de las propiedades del arbol sintactico
     */
    private void setPropiedadesArbolSintactico(BinaryTree tree) {
        if (tree.isLeaf()) {
            if (!tree.getValue().equals("!")){
                // FirstPos y LastPos
                HashSet<Integer> firstPos = new HashSet<Integer>();
                HashSet<Integer> lastPos = new HashSet<Integer>();
                int position = tree.getPosition();
                firstPos.add(position);
                lastPos.add(position);
                tree.setFirstPos(firstPos);
                tree.setLastPos(lastPos);

                // nullable
                tree.setNullable(false);

                // Ver si es estado de aceptacion
                if (tree.getValue().equals("#")){
                    posicionAceptacion = position;
                }
            } else {
                tree.setFirstPos(new HashSet<Integer>());
                tree.setLastPos(new HashSet<Integer>());
                tree.setNullable(false);
            }
        }
        else {
            String root;
            root = tree.getValue();
            BinaryTree leftChild = tree.getLeftChild();
            BinaryTree rightChild = tree.getRightChild();

            // Setear propiedades recursivamente
            if (leftChild != null) {
                setPropiedadesArbolSintactico(leftChild);
            }
            if (rightChild != null) {
                setPropiedadesArbolSintactico(rightChild);
            }


            HashSet<Integer> firstPos = new HashSet<Integer>();
            HashSet<Integer> lastPos = new HashSet<Integer>();
            boolean nullable = false;

            // Posibilidades de operacion
            switch (root.charAt(0)){
                case '|':
                    // FirstPos
                    firstPos.addAll(rightChild.getFirstPos());
                    firstPos.addAll(leftChild.getFirstPos());

                    // LastPos
                    lastPos.addAll(rightChild.getLastPos());
                    lastPos.addAll(leftChild.getLastPos());

                    // Nullable
                    nullable = rightChild.isNullable() || leftChild.isNullable();
                    break;

                case '.':
                    // FirstPos
                    if (leftChild.isNullable()){
                        // Union de los dos
                        firstPos.addAll(rightChild.getFirstPos());
                        firstPos.addAll(leftChild.getFirstPos());
                    } else {
                        firstPos.addAll(leftChild.getFirstPos());
                    }


                    // LastPos
                    if (rightChild.isNullable()){
                        // Union de los dos
                        lastPos.addAll(rightChild.getLastPos());
                        lastPos.addAll(leftChild.getLastPos());
                    } else {
                        lastPos.addAll(rightChild.getLastPos());
                    }

                    // Nullable
                    nullable = rightChild.isNullable() && leftChild.isNullable();

                    // Set FollowPos
                    HashSet<Integer> leftChildLastPos = leftChild.getLastPos();
                    HashSet<Integer> rightChildFirstPos = rightChild.getFirstPos();
                    BinaryTree nodoSignificativo;

                    for (Integer position: leftChildLastPos){
                        // Obtener nodo significativo
                        nodoSignificativo = leafNodes.get(position);
                        nodoSignificativo.setFollowPos(rightChildFirstPos);
                    }

                    break;

                case '*':
                    // FirstPos
                    firstPos.addAll(leftChild.getFirstPos());

                    // LastPos
                    lastPos.addAll(leftChild.getLastPos());

                    // Nullable
                    nullable = true;

                    // Set FollowPos
                    HashSet<Integer> childLastPos = leftChild.getLastPos();
                    HashSet<Integer> childFirstPos = leftChild.getFirstPos();
                    BinaryTree nodoSignificative;

                    for (Integer position: childLastPos){
                        // Obtener nodo significativo
                        nodoSignificative = leafNodes.get(position);

                        // Agregar posiciones
                        nodoSignificative.setFollowPos(childFirstPos);
                    }
                    break;
            }

            tree.setFirstPos(firstPos);
            tree.setLastPos(lastPos);
            tree.setNullable(nullable);
        }
    }

    /**
     * Metodo que tiene como objetivo generar un arbol sintactico a partir de un regex en postfix
     * @param regex expresion regular a utilizarse para generar arbol
     * @return un arbol sintactico
     */
    private BinaryTree generateSyntaxTree(String regex) {
        Stack<BinaryTree> stack = new Stack<BinaryTree>();
        BinaryTree op1, op2, result;
        HashSet<String> alphabet = new HashSet<String>();

        // Se modifico esta clase para poder soportar metacaracteres
        boolean ingresaronCaracterEscape =  false;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);

            // Verificar si ingresaron un caracter de escape
            ingresaronCaracterEscape = c == '\\';

            // Si no ingresaron un caracter de escape se hace toki normal
            if (!ingresaronCaracterEscape){
                if (RegExToNFA.isTwoOperator(c)) {
                    op2 = stack.pop();
                    op1 = stack.pop();
                    stack.push(new BinaryTree(String.valueOf(c), op1, op2));
                } else if (RegExToNFA.isOneOperator(c)) {
                    op1 = stack.pop();
                    stack.push(new BinaryTree(String.valueOf(c), op1, null));
                } else {
                    String letra = String.valueOf(c);
                    result = new BinaryTree(letra);  // Crear nodo hoja
                    if (!letra.equals("!")) {
                        int posicion = getPositionCounter();
                        result.setPosition(posicion);  // Setear posicion en arbol
                        leafNodes.put(posicion, result);  // Agregar a mapa de nodos
                    }

                    if (!letra.equals("!") && !letra.equals("#")){
                        alphabet.add(letra);  // Agregar letra a alfabeto
                    }

                    stack.push(result);
                }

            // Pero si ingresaron caracter especial, saltarse el paso para reconocerlo como de alfabeto
            } else {
                // Moverse al siguiente caracter
                i++;
                c = regex.charAt(i);

                String letra = String.valueOf(c);
                result = new BinaryTree(letra);  // Crear nodo hoja

                int posicion = getPositionCounter();
                result.setPosition(posicion);  // Setear posicion en arbol
                leafNodes.put(posicion, result);  // Agregar a mapa de nodos
                alphabet.add(letra);  // Agregar letra a alfabeto

                stack.push(result);
            }
        }

        result = stack.pop();
        alfabeto = alphabet;
        return result;
    }


    public String augmentateRegex(String regex){
        String newRegex = "(";
        newRegex = newRegex + regex + ")#";
        return newRegex;
    }

    public int getPositionCounter() {
        int result = positionCounter + 1;
        setPositionCounter(result);
        return result;
    }

    public void setPositionCounter(int positionCounter) {
        this.positionCounter = positionCounter;
    }
}

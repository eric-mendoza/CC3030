package GeneradorLexers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;


/**
 * La presente clase tiene como objetivo simular los automatas para los diferentes tipos de parser. De ahí sus siglas
 * (Parser Deterministic Finit Automata)
 * @author Eric Mendoza
 * @version 3.0
 * @since 23/07/2017
 */
public class PDFA  implements Serializable {
    /**
     * Atributos
     */
    private LinkedList<NodeClass> nodes = new LinkedList<PDFA.NodeClass>();
    private HashMap<Kernel, NodeClass> kernelsMap = new HashMap<Kernel, NodeClass>();  // Se van a guardar los kernelsMap de cada estado cuando se cree uno
    private LinkedList<edgeContents> edges = new LinkedList<PDFA.edgeContents>();
    private HashMap<Integer, NodeClass> nodeMap = new HashMap<Integer, PDFA.NodeClass>();
    private HashSet<NodeClass> nodeInitialMap = new HashSet<PDFA.NodeClass>();  // Guardar nodos iniciales
    private HashSet<NodeClass> nodeFinalMap = new HashSet<PDFA.NodeClass>();  // Guardar nodos finales
    private HashSet<String> alphabet = new HashSet<String>();  // Simbolos aceptados por el automata
    private NodeClass accNode;
    private Grammar grammar;
    private TableSLR tableSLR;

    /**
     * Contructor de la clase GeneradorLexers.PDFA
     */
    public PDFA(){}


    /**
     * Clase que simula los estados del automata
     */
    public class NodeClass implements Serializable{
        /**
         * Atributos de clase nodo
         */
        boolean Final, Start;
        int  id;
        LinkedList<edgeContents> edges;
        Kernel kernel;
        HashSet<Item> items;
        HashSet<String> nextElements;
        boolean conlictRR, conflictSR;

        /**
         * Constructor
         * @param name es el ID del estado, debe ser unico
         * @param isStart indica si el nodo es el de inicio de automata
         * @param isFinal indica si el kernel del nodo tiene el punto al final
         */
        public NodeClass(int name, boolean isStart, boolean isFinal, Kernel kernel, HashSet<Item> items){
            id = name;
            this.Final = isFinal;
            this.Start = isStart;
            edges = new LinkedList<PDFA.edgeContents>();  // Inicializar su listado de transiciones
            this.kernel = kernel;
            // Guardar el kernel nuevo en el conjunto de kernelsMap
            kernelsMap.put(kernel, this);

            if (isStart) nodeInitialMap.add(this);  // Agregar a listado de inicios de automata
            if (isFinal) nodeFinalMap.add(this);  // Agregar a listado de finales de automata

            this.items = items;
            nextElements = new HashSet<String>();
            // Guardar todos los elementos que producirian una transicion
            // Kernel
            for (Item ker : kernel.kernels) {
                String el = ker.getNext();
                if (!el.equals("$")) nextElements.add(ker.getNext());
                else accNode = this;
            }

            // Items
            for (Item it : items) {
                nextElements.add(it.getNext());
            }
        }

        /**
         * Retorna si el nodo es un estado final del automata
         * @return true, si es el final; false de lo contrario.
         */
        public boolean isFinal() {
            return Final;
        }

        /**
         * Se utiliza para cambiar el estado de si es el estado final del automata
         * @param aFinal es el nuevo estado del atributo
         */
        public void setFinal(boolean aFinal) {
            Final = aFinal;
            if (aFinal){
                nodeFinalMap.add(this);
            } else {
                nodeFinalMap.remove(this);
            }
        }

        /**
         * Retorna si el nodo es el estado inicial del automata
         * @return true, si es el final; false de lo contrario.
         */
        public boolean isStart() {
            return Start;
        }

        /**
         * Se utiliza para cambiar el estado inicial
         * @param start nuevo estado del atributo
         */
        public void setStart(boolean start) {
            Start = start;
            if (start){
                nodeInitialMap.add(this);
            } else {
                nodeInitialMap.remove(this);
            }
        }

        /**
         * Se utiliza para obtener el ID de un nodo
         * @return Retorna el ID del nodo
         */
        public int getId() {
            return id;
        }

        /**
         * Se utiliza para cambiar el id del nodo
         * @param id es el nuevo id, que debe ser unico
         */
        public void setId(int id) {
            this.id = id;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NodeClass)) return false;

            NodeClass nodeClass = (NodeClass) o;

            return kernel.equals(nodeClass.kernel);
        }

        @Override
        public int hashCode() {
            int result = (Final ? 1 : 0);
            result = 31 * result + (Start ? 1 : 0);
            result = 31 * result + (edges != null ? edges.hashCode() : 0);
            result = 31 * result + (kernel != null ? kernel.hashCode() : 0);
            result = 31 * result + (items != null ? items.hashCode() : 0);
            result = 31 * result + (nextElements != null ? nextElements.hashCode() : 0);
            result = 31 * result + (conlictRR ? 1 : 0);
            result = 31 * result + (conflictSR ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            String result = "";
            result += "Número de nodo: " + this.id + "\n";
            result += "Kernel: \n" + kernel.toString();
            result += "Items: \n";
            for (Item it: this.items) {
                result += "\t- " + it.toString() + "\n";
            }
            return result;
        }
    }

    /**
     * Clase que simula las transiciones del automata de parser
     */
    public class edgeContents {
        /**
         * Atributos de las transiciones
         */
        NodeClass startingNode;
        NodeClass finishingNode;
        String transition;
        String[] action;

        /**
         * Contructor de la clase de transicion.
         * @param startingNode indica desde que nodo inicia la transicion
         * @param finishingNode indica a que nodo se dirige la transicion
         * @param transition indica la condicion para que se cumpla la transicion
         */
        public edgeContents( NodeClass startingNode, NodeClass finishingNode, String transition, String[] action){
            this.startingNode = startingNode;
            this.finishingNode = finishingNode;
            this.transition = transition;
            this.action = action;
        }

        /**
         * Getters y setters de transiciones
         */
        public NodeClass getFinishingNode() {
            return finishingNode;
        }

        public void setFinishingNode(NodeClass finishingNode) {
            this.finishingNode = finishingNode;
        }

        public NodeClass getStartingNode() {
            return startingNode;
        }

        public void setStartingNode(NodeClass startingNode) {
            this.startingNode = startingNode;

        }

        public String getTransition() {
            return transition;
        }

        @Override
        public boolean equals(Object obj) {
            edgeContents e = (edgeContents)obj;
            return e.finishingNode.equals(finishingNode) && e.startingNode.equals(startingNode);
        }

        @Override
        public int hashCode() {
            int result = startingNode != null ? startingNode.hashCode() : 0;
            result = 31 * result + (finishingNode != null ? finishingNode.hashCode() : 0);
            result = 31 * result + (transition != null ? transition.hashCode() : 0);
            result = 31 * result + Arrays.hashCode(action);
            return result;
        }
    }


    /**
     * Clase que simula ser el kernel de un estado del automata
     */
    public static class Kernel{
        private ArrayList<Item> kernels;
        private boolean isFinal = false;
        private int finalPosition;
        public int hash;

        public Kernel(Item initialkernel) {
            finalPosition = -1; // Si para verificar errores R/R
            this.kernels = new ArrayList<Item>();
            kernels.add(initialkernel);
            isFinal = initialkernel.isEnd();
            if (isFinal) finalPosition = 0;
            hash = hashCode();
        }

        public void addKernel(Item newKernel){
            kernels.add(newKernel);
            boolean isFinalNew = newKernel.isEnd();
            // Determinar si existe un error
            if (isFinalNew) {
                if (!isFinal){
                    finalPosition = kernels.size() - 1;
                    isFinal = true;
                } else {
                    System.err.println("Error: Existe un conflicto R/R con el kernel " + toString());
                }
            }
        }

        public Item getFinalItem(){
            return kernels.get(finalPosition);
        }

        public boolean isFinal() {
            return isFinal;
        }

        public ArrayList<Item> getKernels() {
            return kernels;
        }

        public void setKernels(ArrayList<Item> kernels) {
            this.kernels = kernels;
        }

        @Override
        public boolean equals(Object o) {
            // Verificar si es el mismo elemento segun su referencia
            if (this == o) return true;

            // Verificar si el objeto a comparar es tipo Kernel
            if (!(o instanceof Kernel)) return false;

            // Casting del objeto a Kernel
            Kernel kernel = (Kernel) o;

            if (!(this.isFinal == kernel.isFinal)) return false;

            if (this.kernels.size() != kernel.kernels.size()) return false;

            // Comparar cada uno de los kernels
            int iguales = 0;
            Item itemPropio;
            for (int i = 0; i < this.kernels.size(); i++) {
                itemPropio = this.kernels.get(i);
                for (Item itemAjeno: kernel.getKernels()) {
                    if (itemAjeno.equals(itemPropio)) iguales++;
                }
            }

            if (iguales < this.kernels.size()) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = kernels.hashCode();
            result = 31 * result + (isFinal ? 1 : 0);
            result = 31 * result + finalPosition;
            return result;
        }

        @Override
        public String toString() {
            String result = "";
            for (Item item : kernels) {
                result += "\t- " + item.toString() + "\n";
            }
            return result;
        }
    }


    /**
     * Crea un nodo con las caracteristicas deseadas y lo agrega al automata parser
     * @param g automata a agregar el nodo
     * @param nodeId identificador de nodo (debe ser unico)
     * @param isStart Indicar si es nodo inicial de automata
     * @param isFinal indicar si es nodo final de automata
     */
    public void addNode(PDFA g, int nodeId, boolean isStart, boolean isFinal, Kernel kernel, HashSet<Item> items){
        NodeClass nodeToAdd = new NodeClass(nodeId, isStart, isFinal, kernel, items);
        g.nodes.add(nodeToAdd);
        g.nodeMap.put(nodeId, nodeToAdd);
    }

    /**
     *
     * @param nodeToAdd
     * Adds a node to the graph. Node is added to the HashMap nodeMap of the graph,node is added to HashSet nodes in graph
     */
    public void addNode(PDFA g, NodeClass nodeToAdd){
        g.nodes.add(nodeToAdd);
        g.nodeMap.put(nodeToAdd.id, nodeToAdd);

    }

    /**
     * Agregar y crear una transicion al automata deseado
     * @param g automata a agregar transicion
     * @param startingNode Nodo desde el que sale la transicion
     * @param finishingNode Nodo al que llega la transicion
     * @param transition Identificador de la transicion
     */
    public void addEdges(PDFA g, NodeClass startingNode, NodeClass finishingNode, String transition, boolean terminal){
        String[] action = new String[2];
        if (terminal){
            action[0] = "S";
        } else {
            action[0] = "G";
        }
        action[1] = String.valueOf(finishingNode.id);

        edgeContents edge = new edgeContents(startingNode, finishingNode, transition, action);
        g.edges.add(edge);  // Agregando a automata
        startingNode.edges.add(edge);  // Agregando transision a transiciones de nodo inicial
    }

    /**
     * Agregar una transicion a partir de una transicion existente
     * @param g automata a agregar transicion
     * @param edgeToAdd transicion a agregar
     */
    public void addEdges(PDFA g, edgeContents edgeToAdd ){
        g.edges.add(edgeToAdd);
        edgeToAdd.startingNode.edges.add(edgeToAdd);
    }

    /**
     *  Obtener el mapa de nodos
     * @return Retorna el mapa de estados del automata junto con sus identificadores
     */
    public HashMap<Integer, NodeClass> getNodeMap(){
        return nodeMap;
    }

    public void setNodeMap(HashMap<Integer, NodeClass> nodeMap){
        this.nodeMap = nodeMap;
    }

    /**
     * Obtiene un estado dependiendo de su identificador
     * @param id el identificador del nodo a buscar
     * @return retorna el estado deseado
     */
    public  NodeClass getParticularNode(int id){
        if (nodeMap.containsKey(id))
            return nodeMap.get(id);
        return null;
    }

    /**
     * Devuelve el hashmap de los nodos
     * @return hasmap de nodos
     */
    public LinkedList<NodeClass> getAllNodes(){
        return nodes;
    }

    /**
     * Es para obtener las transiciones del automata
     * @return transiciones de automata
     */
    public LinkedList<edgeContents> getEdges(){
        return edges;
    }

    /**
     * Devuelve los nodos iniciales del automata
     * @return nodo inicial
     */
    public HashSet<NodeClass> getInicialNode(){
        return nodeInitialMap;
    }

    /**
     * Obtiene los nodos finales del automata
     * @return nodo final
     */
    public HashSet<NodeClass> getFinalNode(){
        return nodeFinalMap;
    }

    /**
     * Devuelve un nodo inicial del automata
     * @return nodo inicial
     */
    public NodeClass getOneInicialNode(){
        return nodeInitialMap.iterator().next();
    }

    /**
     * Obtiene el nodo final del automata
     * @return nodo final
     */
    public NodeClass getOneFinalNode(){
        return nodeFinalMap.iterator().next();
    }

    /**
     * Recorre to do el automata para devolver su descripcion
     * @return descripcion de automata
     */
    public String automataDescription(){
        // Nodos
        LinkedList<PDFA.NodeClass> nodos = getAllNodes();
        String resultNodos = "Estados: {";
        int contador = 0;
        for (PDFA.NodeClass i: nodos) {
            resultNodos = resultNodos + String.valueOf((i.id));
            if (contador < nodos.size()-1){
                resultNodos = resultNodos + ", ";
            }
            else {
                resultNodos = resultNodos + "}\n";
            }
            contador ++;
        }

        resultNodos += "\t- Son " + contador + " estados\n";

        // Nodo Inicial
        resultNodos = resultNodos + "Estado inicial: {";
        contador = 0;
        for (NodeClass i: nodeInitialMap) {
            resultNodos = resultNodos + String.valueOf((i.id));
            if (contador < nodeInitialMap.size()-1){
                resultNodos = resultNodos + ", ";
            }
            else {
                resultNodos = resultNodos + "}\n";
            }
            contador++;
        }


        // Nodo Final
        resultNodos = resultNodos + "Estado(s) de aceptacion: {";
        contador = 0;
        for (NodeClass i: nodeFinalMap) {
            resultNodos = resultNodos + String.valueOf((i.id));
            if (contador < nodeFinalMap.size()-1){
                resultNodos += ", ";
            }
            else {
                resultNodos = resultNodos + "}\n";
            }
            contador ++;
        }

        // Simbolos
        LinkedList<PDFA.edgeContents> edges = getEdges();
        String resultEdges = "Simbolos: {";
        contador = 0;
        for (String i: alphabet) {
            resultEdges = resultEdges + i;
            if (contador < alphabet.size()-1){
                resultEdges = resultEdges + ", ";
            }
            else {
                resultEdges = resultEdges + "}\n";
            }
            contador ++;
        }

        // Edges
        resultEdges = resultEdges + "Transiciones: {";
        contador = 0;
        for (PDFA.edgeContents i: edges) {
            resultEdges = resultEdges + "(" + i.getStartingNode().getId() + ", " + i.getTransition() + ", " + i.getFinishingNode().getId() + ")";
            if (contador < edges.size()-1){
                resultEdges = resultEdges + ", ";
            }
            else {
                resultEdges = resultEdges + "}\n";
            }
            contador ++;
        }
        resultEdges += "\t- Son " + contador + " transiciones.";

        // Descripcion de todos los estados
        String resultStates = "";
        for (NodeClass nodo : nodos) {
            resultStates += "\n_____________________________\n";
            resultStates += nodo.toString();
            resultStates += "\n_____________________________\n";
        }

        // Si existe una tabla, agregarla
        if (tableSLR != null) {
            resultStates += "\n\n" + tableSLR.tableDescription();
        }

        return resultNodos + resultEdges + resultStates;
    }

    public HashSet<NodeClass> getNodeFinalMap() {
        return nodeFinalMap;
    }

    public void setNodeFinalMap(HashSet<NodeClass> nodeFinalMap) {
        this.nodeFinalMap = nodeFinalMap;
    }


    public HashSet<String> getAlphabet() {
        return alphabet;
    }

    public void setAlphabet(HashSet<String> alphabet) {
        this.alphabet = alphabet;
    }

    public boolean containsKernel(Kernel ker){
        return kernelsMap.containsKey(ker);
    }

    public PDFA.NodeClass getStateByKernel(Kernel ker){
        return kernelsMap.get(ker);
    }

    public HashMap<Kernel, NodeClass> getKernelsMap() {
        return kernelsMap;
    }

    public void setKernelsMap(HashMap<Kernel, NodeClass> kernelsMap) {
        this.kernelsMap = kernelsMap;
    }

    public void createDescriptionDocument(){
        PrintWriter pW = null;
        try {
            pW = new PrintWriter(new File("PDFA.txt"));
            pW.printf(automataDescription());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }
    }

    public NodeClass getAccNode() {
        return accNode;
    }

    public void createSLRTable(){
        tableSLR = new TableSLR();
    }

    public class TableSLR{
        HashMap<Integer, HashMap<String, String[]>> table;

        public TableSLR() {
            // Inicializar tabla
            table = new HashMap<Integer, HashMap<String, String[]>>();

            // Agregar todos los shift y los GoTo a la tabla
            for (edgeContents edge: edges) {
                addAction(edge.startingNode.id, edge.transition, edge.action);
            }

            // Agregar los reduce a la tabla
            HashSet<String> follow;
            String[] newAction;
            for (NodeClass end : getFinalNode()) {
                Item kernel = end.kernel.getFinalItem();
                follow = grammar.getFollowNonTeminal(kernel.getHead());
                newAction = new String[2];
                newAction[0] = "R";
                newAction[1] = String.valueOf(end.id);  // Guardar el estado donde se encuentra la reduccion
                for (String symbol : follow) {
                    addAction(end.id, symbol, newAction);
                }

            }

            // Agregar el estado de aceptacion
            newAction = new String[2];
            newAction[0] = "ACC";
            newAction[1] = "";
            addAction(accNode.id, "$", newAction);
        }

        public void addAction(int estado, String input, String[] action){
            HashMap<String, String[]> fila;
            if (table.containsKey(estado)){  // Si existela fila
                fila = table.get(estado);
                if (fila.containsKey(input)){  // Si existe la columna
                    String conflicto = action[0] + "/" + fila.get(input)[0];
                    System.err.println("Error: Existe un conflicto '" + conflicto + "' en la tabla.");
                }

                // Si no existe la columna
                else {
                    fila.put(input, action);
                }
            }

            // Si no existe la fila
            else {
                table.put(estado, new HashMap<String, String[]>());
                fila = table.get(estado);
                fila.put(input, action);
            }
        }

        public String[] getAction(int estado, String input){
            if (table.containsKey(estado)){
                HashMap<String, String[]> fila = table.get(estado);
                if (fila.containsKey(input)){
                    return fila.get(input);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        public String tableDescription(){
            String header = "____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________\n" +
                    "____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________\n" +
                    "                                                                                                                          SLR TABLE\n" +
                    "____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________\n" +
                    "____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________\n";

            String titles = "|\t\tEstados\t\t";
            ArrayList<String> terminales = grammar.getTerminals();
            for (String symbol: terminales) {
                titles += "|\t\t" + symbol + "\t\t";
            }
            ArrayList<String> noTerminales = grammar.getNonTerminals();
            for (String symbol: noTerminales) {
                titles += "|\t\t" + symbol + "\t\t";
            }
            titles += "|\n";

            // Agregar los estados y sus acciones
            String fila = "";
            String[] action;
            int estado;
            for (int i = 0; i < nodes.size(); i++) {
                fila += "|\t\t" + i + "\t\t";
                estado = i;

                for (String symbol: terminales) {
                    action = getAction(i, symbol);
                    if (action == null) {
                        action = new String[2];
                        action[0] = "-";
                        action[1] = "-";
                    }
                    fila += "|\t\t" + action[0] + action[1] + "\t\t";
                }
                for (String symbol: noTerminales) {
                    action = getAction(i, symbol);
                    if (action == null) {
                        action = new String[2];
                        action[1] = "-";
                        action[0] = "-";
                    }
                    fila += "|\t\t" + action[0] + action[1] + "\t\t";
                }
                fila += "|\n";
            }

            return header + titles + fila;
        }
    }

    public Grammar getGrammar() {
        return grammar;
    }

    public void setGrammar(Grammar grammar) {
        this.grammar = grammar;
    }

    public TableSLR getTableSLR() {
        return tableSLR;
    }
}
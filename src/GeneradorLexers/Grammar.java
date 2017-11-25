package GeneradorLexers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class Grammar implements Serializable{

    private ArrayList<String> terminals, nonTerminals;
    private HashMap<String, ArrayList<String>> productions;  // <No-terminal, producciones>
    private HashMap<String, ArrayList<Item>> augProductions;
    private HashMap<String, HashSet<String>> follow, first;  // <No-terminal, next terminal>
    private HashSet<String> symbols;
    private String initialNonTerminal, initialNonTerminalAug;
    private HashMap<Item, HashSet<Item>> closureItem;
    private int names;

    Grammar() {
        this.productions = new HashMap<String, ArrayList<String>>();
        this.terminals = new ArrayList<String>();
        this.nonTerminals = new ArrayList<String>();
        this.follow = new HashMap<String, HashSet<String>>();
        this.first = new HashMap<String, HashSet<String>>();
        this.symbols = new HashSet<String>();
        closureItem = new HashMap<Item, HashSet<Item>>();
        names = 0;
    }

    /**
     * Metodo que tiene como objetivo agregar varias producciones a un solo no terminal
     * @param head es el no-terminal al que se le agrgaran producciones
     * @param bodies es la lista de producciona a asociar con el no-terminal
     */
    public void addProductions(String head, ArrayList<String> bodies){
        if (productions.size() == 0){
            initialNonTerminal = head;
        }

        for (String body : bodies) {
            addProduction(head, body);
        }
    }

    /**
     * Este metodo agrega una sola produccion a un simbolo no terminal. Si no existia la cabeza antes de ser agregada
     * se agrega la cabeza al listado de simbolos y al listado de no terminales. Además, vuelve a calcular FIRST y FOLLOW
     * @param head es el símbolo no terminal al que se le quiere agregar una producción
     * @param body es el cuerpo de la producción. Este no debe incluir ningun OR, ya debió ser parseada. Cada termino separado por ' '
     */
    public void addProduction(String head, String body){
        // Ver si ya existe el head en la gramatica
        if (symbols.contains(head)){
            // Ver si es no terminal
            if (nonTerminals.contains(head)){
                // Agregar la produccion al no terminal
                productions.get(head).add(body);
            }

            // Si era terminal, debe ser movido a no terminal
            else {
                terminals.remove(head);
                nonTerminals.add(head);

                // Crear la nueva produccion
                productions.put(head, new ArrayList<String>());
                productions.get(head).add(body);
            }

        }

        // Si no existía agregarlo a símbolos y a no termiales
        else {
            symbols.add(head);
            nonTerminals.add(head);

            // Crear la nueva produccion
            productions.put(head, new ArrayList<String>());
            productions.get(head).add(body);
        }

        // Leer cada uno de sus simbolos para ver si hay nuevos simbolos
        readBodySymbols(body);
    }


    /**
     * Recibe el cuerpo de una producción y analiza cada uno de sus símbolos. Esta clase los clasifica como terminal o no
     * terminal.
     * @param body El cuerpo de una produccion. Debe tener todos los symbolos separados por ' '.
     */
    public void readBodySymbols(String body){
        String[] newSymbols = body.split(" ");
        String symbol;
        for (int i = 0; i < newSymbols.length; i++) {
            // Simbolo leido
            symbol = newSymbols[i];

            // Ver si no existe el símbolo en la gramatica
            if (!symbols.contains(symbol) && !symbol.equals("")){

                // Agregrarlo
                symbols.add(symbol);
                terminals.add(symbol);
            }
        }
    }

    public void calculateFirstForNonTerminals(){
        // Por cada no terminal
        for (String nonTerminal : nonTerminals) {
            // Verificar si no lo ha calculado
            this.first.put(nonTerminal, calculateFirst(nonTerminal));
        }
    }

    public HashSet<String> calculateFirst(String nonTerminal) {
        String[] productionSymbols;
        String symbol;
        HashSet<String> first = new HashSet<String>();
        HashSet<String> subFirst;
        boolean producesEpsilon = true;
        // Por cada produccion del no terminal
        for (String production : productions.get(nonTerminal)) {
            productionSymbols = production.trim().split(" ");

            // Por cada símbolo de la produccion
            for (int i = 0; i < productionSymbols.length; i++) {
                symbol = productionSymbols[i];

                // Si es un terminal, debe terminar el for para esta produccion
                if (terminals.contains(symbol)){
                    first.add(symbol);
                    break;
                }

                // Si es un no-terminal
                else {
                    // Verificar si ya se calculó su FIRST
                    subFirst = this.first.get(symbol);
                    if (subFirst != null){
                        first.addAll(subFirst);
                    } else {
                        // Asegurarse que no se calcula al mismo first si ya se esta calculando
                        if (!nonTerminal.equals(symbol)){
                            subFirst = calculateFirst(symbol);
                            this.first.put(symbol, subFirst);
                            first.addAll(subFirst);
                        } else {
                            subFirst = new HashSet<String>();
                        }
                    }

                    producesEpsilon = subFirst.contains("\"\"");
                    if (!producesEpsilon) break;
                }

            }
        }
        return first;
    }

    /**
     * Metodo que calcula el PRIMERO de una cadena de simbolos de una gramática.
     * @param symbols es la cadena de símbolos a analizar, debe venir separada por espacios y tener simbolos existentes
     * @return devuelve el conjunto de símbolos terminales que van primero
     */
    public HashSet<String> getFirstOfSymbols(String symbols){
        if (this.first.size() == 0){
            calculateFirstForNonTerminals();
        }

        HashSet<String> result = new HashSet<String>();
        HashSet<String> preResult;
        String[] newSymbols = symbols.split(" ");
        boolean producesEpsilon = true;

        for (String symbol : newSymbols) {
            if (!symbol.equals("")) {
                // Verificar si existe
                if (this.symbols.contains(symbol)) {
                    // Verificar si es no-terminal
                    if (nonTerminals.contains(symbol)) {
                        // Obtener el first del simbolo
                        preResult = this.first.get(symbol);
                        result.addAll(preResult);
                        producesEpsilon = preResult.contains("\"\"");
                    }

                    // Si es terminal, solo se tiene este elemento y se termina el ciclo
                    else {
                        result.add(symbol);
                        return result;
                    }

                }

                // Si no existe, devolver error
                else {
                    return null;
                }
            }
            if (!producesEpsilon) break;  // Si no produce epsilon el primer simbolo, se termina
        }

        if (!producesEpsilon){
            result.remove("\"\"");
        }

        return result;
    }

    public void calculateFollows(){
        // Inicializar todas los follows
        for (String nonTerminal: nonTerminals) {
            follow.put(nonTerminal, new HashSet<String>());
        }

        // Agregrar al inicial el signo de terminacion
        follow.get(initialNonTerminal).add("$");

        ArrayList<String> temporalProduction;
        HashSet<String> firstUltimo, firstPenultimo;
        String[] temporalSymbols;
        String preLastSymbol, lastSymbol, firstLastSymbol = "";
        ArrayList<String[]> mustHave = new ArrayList<String[]>(); // [CopiarEste, AEste]
        boolean producesEpsilon;
        // Calcular siguiente de todos los no terminales
        for (String noTerminal : nonTerminals) {
            // Analizar cada una de las producciones del no terminal
            temporalProduction = productions.get(noTerminal);
            for (String production : temporalProduction) {
                // Obtener los símbolos de la producción
                temporalSymbols = production.trim().split(" ");

                // Si la produccion tiene más de un símbolo se analiza, de lo contrario, no agrega nada
                if (temporalSymbols.length > 1){
                    for (int i = temporalSymbols.length - 1; i > 0 ; i--) {
                        // Obtener el último símbolo de la producción
                        lastSymbol = temporalSymbols[i];
                        firstUltimo = getFirstOfSymbols(lastSymbol);  // Obtener el FIRST del último simbolo
                        producesEpsilon = firstUltimo.contains("\"\"");

                        // Guardar el primer de los último símbolo de la produccion
                        if (i == temporalSymbols.length - 1){
                            firstLastSymbol = lastSymbol;
                        }

                        // Obtener el penultimo símbolo de la produccion
                        preLastSymbol = temporalSymbols[i - 1];

                        // Si el último FIRST produce épsilon
                        if (producesEpsilon){
                            firstUltimo.remove("\"\"");  // Quitarselo al FIRST del último
                            if (nonTerminals.contains(preLastSymbol)){
                                follow.get(preLastSymbol).addAll(firstUltimo);  // Agregar a FOLLOW de penultimo

                                // Lo que esta en el FOLLOW de la cabeza, debe estar en PreLastSymbol
                                // Esta copia, se hará de último
                                String[] copiar = new String[2];
                                copiar[0] = noTerminal;
                                copiar[1] = preLastSymbol;
                                mustHave.add(copiar);
                            }
                        }

                        // Si no produce epsilon
                        else {
                            if (nonTerminals.contains(preLastSymbol)){
                                follow.get(preLastSymbol).addAll(firstUltimo);  // Agregar a FOLLOW de penultimo
                            }
                        }
                    }

                    // Siempre, copiar lo que esta en el follow de la cabeza, al último símbolo de la produccion
                    String[] copiar = new String[2];
                    copiar[0] = noTerminal;
                    copiar[1] = firstLastSymbol;

                    // Copiar solamente si no son iguales y el ultimo termino es un no terminal
                    if (!copiar[1].equals(copiar[0]) && nonTerminals.contains(copiar[1])){
                        mustHave.add(copiar);
                    }

                } else {
                    if (nonTerminals.contains(temporalSymbols[0])){
                        String[] copiar = new String[2];
                        copiar[0] = noTerminal;
                        copiar[1] = temporalSymbols[0];
                        mustHave.add(copiar);
                    }
                }
            }
        }

        // Copiar todos los mustHave
        boolean seAgregoAlgo = true;
        if (mustHave.size() > 0){
            while (seAgregoAlgo){
                seAgregoAlgo = false;
                for (String[] par : mustHave) {
                    // Obtener follow de cabeza
                    HashSet<String> followCabeza = this.follow.get(par[0]);

                    // Copiar follow de cabeza a no-terminal y ver si se agrego algo nuevo
                    seAgregoAlgo = this.follow.get(par[1]).addAll(followCabeza) || seAgregoAlgo;
                }
            }
        }
    }

    public HashSet<String> getFollowNonTeminal(String nonTerminal){
        HashSet<String> result = new HashSet<String>();

        // Verificar que se hayan calculado los FIrst
        if (this.first.size() == 0){
            calculateFirstForNonTerminals();
        }

        // Verificar que se hayan calculado los follow
        if (this.follow.size() == 0){
            calculateFollows();
        }

        // Si existe el no terminal, devolver su follow
        if (nonTerminals.contains(nonTerminal)){
            result.addAll(this.follow.get(nonTerminal));
            return result;
        } else {
            return null;
        }
    }

    /**
     * Devuelve el conjunto de items que se pueden alcanzar desde un item. Este metodo NO devuelve el item que lo
     * origina.
     * @param item es el item inicial desde el que se desarrollan los otros
     * @param set es el conjunto resultado que se ira pasando por cada llamada recursiva
     * @return devuelve el conjunto de items alcanzados por el item inicial
     */
    public HashSet<Item> closure(Item item, HashSet<Item> set){
        // Obtener el elemento despues del punto
        String siguienteNoTerminal = item.getNext();
        Item itemTemp;

        // Si el siguiente elemento despues del punto es un no terminal1:
        if (nonTerminals.contains(siguienteNoTerminal) && !siguienteNoTerminal.equals(item.getHead())){
            // Obtener las producciones del no terminal
            ArrayList<String> producciones = productions.get(siguienteNoTerminal);

            // Crear un item nuevo por cada produccion del no terminal y agregar su closure
            for (String produccion : producciones) {
                itemTemp = new Item(siguienteNoTerminal, produccion.trim().split(" "), 0);
                set.add(itemTemp);  // Agregar un item punto inicial
                set.addAll(closure(itemTemp, new HashSet<Item>()));  // Agregar el closure del item nuevo
            }

            return set;

        } else {
            // Si es un no terminal, devolver el conjunto que se lleva hasta ahora
            return set;
        }
    }

    /**
     * Devuelve el closure para un kernel, no incluye a los elementos del kernel
     * @param ker es el kernel del que se quiere obtener el closure
     * @return devuelve un conjunto de items
     */
    public HashSet<Item> closureKernel(PDFA.Kernel ker){
        HashSet<Item> result = new HashSet<Item>();
        for (Item it : ker.getKernels()) {
            result.addAll(closure(it, result));
        }
        return result;
    }

    /**
     * Metodo para obtner el estado al que se mueve un automata segun su actual estado y un input
     * @param automata el automata que esta siendo creado
     * @param actualState es el estado origen
     * @param input es la entrada bajo la que se produciria la transicion
     * @param toAnalize
     * @return devuelve un estado nuevo (y se agrega de un solo al automata) o devuelve un estado ya existente
     */
    public PDFA.NodeClass goTo(PDFA automata, PDFA.NodeClass actualState, String input, Stack<PDFA.NodeClass> toAnalize){
        // Posible nuevo kernel para el nuevo estado
        PDFA.Kernel newKernel = null;
        boolean coincidence = false;

        // Buscar todos los items que coinciden con input en actualState y crear posible nuevo kernel
        // Kernel
        for (Item kernel: actualState.kernel.getKernels()) {
            // Si coincide con la entrada
            if (kernel.getNext().equals(input)){
                // Agregar a nuevo posible kernel
                if (newKernel == null){
                    newKernel = new PDFA.Kernel(new Item(kernel.getHead(), kernel.getBody(), kernel.getDot() + 1));
                    coincidence = true;
                } else {
                    newKernel.addKernel(new Item(kernel.getHead(), kernel.getBody(), kernel.getDot() + 1));
                }
            }
        }

        // Items
        for (Item it: actualState.items) {
            // Si coincide con la entrada
            if (it.getNext().equals(input)){
                // Agregar a nuevo posible kernel
                if (newKernel == null){
                    coincidence = true;
                    newKernel = new PDFA.Kernel(new Item(it.getHead(), it.getBody(), it.getDot() + 1));
                } else {
                    newKernel.addKernel(new Item(it.getHead(), it.getBody(), it.getDot() + 1));
                }
            }
        }

        // Si se encontro una coincidencia
        if (coincidence){
            // Verificar si el kernel ya existe en el automata siendo creado
            if (automata.containsKernel(newKernel)){
                return automata.getStateByKernel(newKernel);  // Devolver estado existente
            } else {
                // Crear nuevo estado y devolverlo
                automata.addNode(automata, getNewName(), false, newKernel.isFinal(), newKernel, closureKernel(newKernel));
                toAnalize.push(automata.getStateByKernel(newKernel));
                return toAnalize.peek();  // Devolver estado existente
            }
        } else {
            return null;  // Si no existe coincidencia
        }
    }


    public PDFA createLR0(){
        // Stack para aplicar goto a estado en todos sus items
        Stack<PDFA.NodeClass> toAnalize = new Stack<PDFA.NodeClass>();

        // Crear nuevo automata vacio
        PDFA lr0 = new PDFA();

        // Aumentar gramatica
        augmentateGrammar();

        // Inializar automata con produccion inicial aumentada
        PDFA.Kernel newKernel = new PDFA.Kernel(new Item(initialNonTerminalAug, productions.get(initialNonTerminalAug).get(0).trim().split(" "), 0));
        lr0.addNode(lr0, getNewName(), true, newKernel.isFinal(), newKernel, closureKernel(newKernel));

        // Por cada item (contando kernels) hacer un goto con el elemento despues del punto
        PDFA.NodeClass nodoInicial = lr0.getStateByKernel(newKernel);
        toAnalize.push(nodoInicial);

        PDFA.NodeClass actualState;
        while (!toAnalize.empty()){
            actualState = toAnalize.pop();
            HashSet<String> inputs = actualState.nextElements;  // Elementos que producen una transicion

            for (String input : inputs) {
                if (!input.equals("")){
                    PDFA.NodeClass nuevoEstado = goTo(lr0, actualState, input, toAnalize);
                    // Si existe la transicion agregarla al automata
                    if (nuevoEstado != null){
                        lr0.addEdges(lr0, actualState, nuevoEstado, input, terminals.contains(input));
                    }
                }
            }
        }

        // Guardar la gramatica que creo el automata
        lr0.setGrammar(this);
        return lr0;
    }


    /**
     * Clase que agrega una nueva produccion inicial al conjunto de producciones de la gramatica.
     */
    public void augmentateGrammar(){
        // Calcular first y follow antes de aumentar
        if (follow.size() == 0){
            calculateFollows();
        }

        // Agregar la nueva produccion
        initialNonTerminalAug = initialNonTerminal + "Aug";  // Crear nuevo no-termianl inicial
        productions.put(initialNonTerminalAug, new ArrayList<String>());  // Initialize augmented initial production
        productions.get(initialNonTerminalAug).add(initialNonTerminal + " $");  // Agregar simbolo aumentado
        symbols.add("$");
        terminals.add("$");
    }

    public int getNewName(){
        int result = names;
        names++;
        return result;
    }

    @Override
    public String toString() {
        return "Grammar{" +
                "productions=" + productions +
                '}';
    }

    public boolean parse(ArrayList<String> input, PDFA automata){
        input.add("$");  // Agregar caracter de final
        PDFA.TableSLR tableSLR = automata.getTableSLR();
        Stack<Integer> stack = new Stack<Integer>();
        stack.push(automata.getOneInicialNode().id);  // Agregar primer estado
        Stack<String> symbols = new Stack<String>();  // Lo que se lleva leido
        String nextInput;
        int actualState;
        String[] action;
        PDFA.Kernel produccion;
        Item produccionItem;
        while (!stack.empty()){
            nextInput = input.get(0);  // Siguiente token a leer
            actualState = stack.peek();
            action = tableSLR.getAction(actualState, nextInput);

            // Si no existe la transicion
            if (action == null){
                System.err.println("Error: No se puede avanzar si el presente token es: " + symbols.peek() +
                " y el siguiente: " + input.get(0));
                return false;
            }

            // Si se debe realizar un shift
            else if (action[0].equals("S")){
                stack.push(Integer.valueOf(action[1]));  // Pushear el siguiente estado
                symbols.push(nextInput);
                input.remove(0);
            }

            else if (action[0].equals("R")){
                // Obtener produccion para reduccion
                produccion = automata.getNodeMap().get(Integer.valueOf(action[1])).kernel;
                produccionItem = produccion.getFinalItem();

                for (int i = 0; i < produccionItem.getBody().size(); i++) {
                    symbols.pop();  // Eliminar symbolos segun largo del cuerpo de produccion
                    stack.pop();
                }

                // Agregar nuevo simbolo
                symbols.push(produccionItem.getHead());

                // Determinar siguiente estado
                stack.push(Integer.valueOf(tableSLR.getAction(stack.peek(), symbols.peek())[1]));
            }

            else if (action[0].equals("ACC")){
                return true;
            }
        }

        return false;
    }

    public ArrayList<String> getTerminals() {
        return terminals;
    }

    public void setTerminals(ArrayList<String> terminals) {
        this.terminals = terminals;
    }

    public ArrayList<String> getNonTerminals() {
        return nonTerminals;
    }

    public void setNonTerminals(ArrayList<String> nonTerminals) {
        this.nonTerminals = nonTerminals;
    }

    public HashMap<String, HashSet<String>> getFollow() {
        return follow;
    }

    public void setFollow(HashMap<String, HashSet<String>> follow) {
        this.follow = follow;
    }

    public HashSet<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(HashSet<String> symbols) {
        this.symbols = symbols;
    }

    public int getNames() {
        return names;
    }

    public void setNames(int names) {
        this.names = names;
    }
}

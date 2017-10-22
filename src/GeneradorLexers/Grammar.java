package GeneradorLexers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Grammar {

    private ArrayList<String> terminals, nonTerminals;
    private HashMap<String, ArrayList<String>> productions;  // <No-terminal, producciones>
    private HashMap<String, HashSet<String>> follow, first;  // <No-terminal, next terminal>
    private HashSet<String> symbols;
    private String initialNonTerminal;

    Grammar() {
        this.productions = new HashMap<String, ArrayList<String>>();
        this.terminals = new ArrayList<String>();
        this.nonTerminals = new ArrayList<String>();
        this.follow = new HashMap<String, HashSet<String>>();
        this.first = new HashMap<String, HashSet<String>>();
        this.symbols = new HashSet<String>();
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
                        subFirst = calculateFirst(symbol);
                        this.first.put(symbol, subFirst);
                        first.addAll(subFirst);
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
                for (String[] par : mustHave) {
                    // Obtener follow de cabeza
                    HashSet<String> followCabeza = this.follow.get(par[0]);

                    // Copiar follow de cabeza a no-terminal y ver si se agrego algo nuevo
                    seAgregoAlgo = this.follow.get(par[1]).addAll(followCabeza);
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


}

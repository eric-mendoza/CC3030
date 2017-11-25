package GeneradorLexers;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * La presente clase tiene como objetivo simular un Destado para un DFA
 * @author Eric Mendoza
 * @version 1.0
 * @since 5/08/207
 */
public class Dstate<E>  implements Serializable {
    /**
     * Atributos
     */
    private HashSet<E> conjuntoEstados;
    private boolean marked, dStateInitial, dStateFinal;
    private LinkedList<Dtransition> transitions;

    /**
     * Constructor para un Destado
     * @param conjuntoEstados es el conjunto de estados equivalente en un NFA
     * @param dStateFinal indica si el Destado contiene un estado de aceptacion del NFA
     * @param dStateInitial indica si el Destado es el inicial
     */
    public Dstate(HashSet<E> conjuntoEstados, boolean dStateInitial, boolean dStateFinal) {
        this.conjuntoEstados = conjuntoEstados;
        this.dStateFinal = dStateFinal;
        this.dStateInitial = dStateInitial;
        this.transitions = new LinkedList<Dtransition>();
        this.marked = false;
    }

    public HashSet<E> getConjuntoEstados() {
        return conjuntoEstados;
    }

    public void setConjuntoEstados(HashSet<E> conjuntoEstados) {
        this.conjuntoEstados = conjuntoEstados;
    }

    public boolean isdStateFinal() {
        return dStateFinal;
    }

    public void setdStateFinal(boolean dStateFinal) {
        this.dStateFinal = dStateFinal;
    }

    public boolean isdStateInitial() {
        return dStateInitial;
    }

    public void setdStateInitial(boolean dStateInitial) {
        this.dStateInitial = dStateInitial;
    }


    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public LinkedList<Dtransition> getTransitions() {
        return transitions;
    }

    public void setTransitions(LinkedList<Dtransition> transitions) {
        this.transitions = transitions;
    }

    public void addTransitions(Dtransition dtransition){
        transitions.add(dtransition);
    }

    /**
     * Verifica si dos dStates son iguales, esto depende del conjunto de estados que lo define
     * @param o dstate a comparar
     * @return verdadero si son iguales.
     */
    public boolean equals(Dstate o) {
        HashSet estados2 = o.getConjuntoEstados();
        HashSet<E> estados1 = this.getConjuntoEstados();

        return estados1.equals(estados2);
    }
}


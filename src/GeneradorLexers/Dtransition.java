package GeneradorLexers;

import java.io.Serializable;

/**
 * La presente clase tiene como objetivo simular una Dtrancision para un DFA
 * @author Eric Mendoza
 * @version 1.0
 * @since 5/08/207
 */
public class Dtransition  implements Serializable {
    /**
     * Atributos
     */
    Dstate startingState;
    Dstate finishingState;
    String transition;

    /**
     * Contructor de la clase de GeneradorLexers.Dtransition.
     * @param startingState indica desde que nodo inicia la transicion
     * @param finishingState indica a que nodo se dirige la transicion
     * @param transition indica la condicion para que se cumpla la transicion
     */
    public Dtransition(Dstate finishingState, Dstate startingState, String transition) {
        this.finishingState = finishingState;
        this.startingState = startingState;
        this.transition = transition;
    }

    public Dstate getFinishingState() {
        return finishingState;
    }

    public void setFinishingState(Dstate finishingState) {
        this.finishingState = finishingState;
    }

    public Dstate getStartingState() {
        return startingState;
    }

    public void setStartingState(Dstate startingState) {
        this.startingState = startingState;
    }

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }
}

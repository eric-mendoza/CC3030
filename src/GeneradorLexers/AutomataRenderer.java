package GeneradorLexers;

import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Esta clase tiene como objetivo mostrar en pantalla el automata que se le pase a sus metodos
 * @author Eric Mendoza
 * @version 1.0
 * @since 27/07/207
 */

public class AutomataRenderer implements Serializable{
    public static void renderAutomata(final DirectedGraph automata, String text){
        /**
         * Crear grafo visual
         */
        SimpleGraphView2 sgv = new SimpleGraphView2(automata);

        /**
         * Crear caracteristicas de despliegue
         */
        Layout<Integer, String> layout = new ISOMLayout(sgv.g);
        layout.setSize(new Dimension(300,300));
        VisualizationViewer<Integer,String> vv = new VisualizationViewer<Integer,String>(layout);
        vv.setPreferredSize(new Dimension(450,450));

        /**
         * Pintar los vertices
         */
        Transformer<Integer,Paint> vertexPaint = new Transformer<Integer,Paint>() {

            public Paint transform(Integer i) {
                Color color = Color.GREEN;
                // Obtener numeros de iniciales de nodos
                HashSet<DirectedGraph.NodeClass> nodosIniciales = automata.getInicialNode();
                LinkedList<Integer> idIniciales = new LinkedList<Integer>();
                for (DirectedGraph.NodeClass nodo: nodosIniciales){
                    idIniciales.add(nodo.getId());
                }

                // Obtener numeros de finales de nodos
                HashSet<DirectedGraph.NodeClass> nodosFinales = automata.getFinalNode();

                LinkedList<Integer> idFinales = new LinkedList<Integer>();
                for (DirectedGraph.NodeClass nodo: nodosFinales){
                    idFinales.add(nodo.getId());
                }

                if(idIniciales.contains(i)) color = Color.YELLOW;

                if(idFinales.contains(i)) color = Color.RED;
                else if (!idIniciales.contains(i)) color = Color.GREEN;

                return color;
            }
        };

        /**
         * Agregar caracteristicas interactivas
         */
        DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
        gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
        vv.setGraphMouse(gm);
        vv.getRenderContext().setEdgeShapeTransformer(
                new EdgeShape.Line<Integer,String>());

        /**
         * Agregar todo a la gui
         */
        vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
        vv.addKeyListener(gm.getModeKeyListener());

        /**
         * Desplegar grafo en pantalla
         */
        JFrame frame = new JFrame(text);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.getContentPane().add(vv);
        frame.pack();
        frame.setVisible(true);

    }
}

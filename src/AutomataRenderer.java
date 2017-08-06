import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.*;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Esta clase tiene como objetivo mostrar en pantalla el automata que se le pase a sus metodos
 * @author Eric Mendoza
 * @version 1.0
 * @since 27/07/207
 */

public class AutomataRenderer {
    public static void renderAutomata(final DirectedGraph automata){
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
                if(i == automata.getFinalNode().getId()) return Color.RED;
                else if (i == automata.getInicialNode().getId()) return Color.YELLOW;
                return Color.GREEN;
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
        JFrame frame = new JFrame("Simple Graph View 2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(vv);
        frame.pack();
        frame.setVisible(true);

    }
}

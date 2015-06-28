package test_tracer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by joao-carloto on 11/04/2015.
 * <p/>
 * Panel to select the activity diagram in case the package has more than one.
 */

public class DiagramSelectorForm extends JFrame {
    public JTable table1;
    public Map<String, ActivityNode> activityNodeMap;
    public Document doc;
    public TracerForm mainForm;
    private JButton selectButton;
    private JPanel mainPanel;
    private JScrollPane scrollPanel;

    public DiagramSelectorForm() {
        super("Select an Activity Diagram");
        setContentPane(mainPanel);
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Diagram Name");
        model.addColumn("Node");
        table1.setModel(model);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selRowIndex = table1.getSelectedRow();
                if (selRowIndex == -1) {
                    JOptionPane.showMessageDialog(null, "Please select one of the diagrams", "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                HashMap<String, ActivityNode> activityNodeMap = new HashMap<String, ActivityNode>();
                Node diagram = (Node) table1.getValueAt(selRowIndex, 1);
                Component component = (Component) e.getSource();
                JFrame form = (JFrame) SwingUtilities.getRoot(component);
                form.dispatchEvent(new WindowEvent(form, WindowEvent.WINDOW_CLOSING));
                EANodeCollector.collectNodes(doc, diagram, activityNodeMap);
                mainForm.makeTests(activityNodeMap);
            }
        });
        final JFileChooser fc = new JFileChooser();
        setSize(400, 250);
        setLocationRelativeTo(null);
        pack();
        setVisible(true);
    }
}

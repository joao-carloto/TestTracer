package test_tracer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import static test_tracer.TestTracer.createActivityPaths;
import static test_tracer.Utils.removeDuplicateLoops;

/**
 * Created by joao-carloto on 17/03/2015.
 * <p/>
 * Main GUI.
 * From here we select the exported package in the form of a XMI file and visualise the created test cases.
 */

public class TracerForm extends JFrame {

    public JTextField pathBox;
    public JTextArea textArea1;
    private JButton createTestsBtn;
    private JPanel panel1;
    private JTabbedPane tabbedPane1;
    private JPanel jpanel2;
    private JButton selectFileButton;
    private JButton helpButton;
    private JScrollPane scrollPane1;

    public TracerForm() {
        super("Test Tracer");
        setContentPane(panel1);
        tabbedPane1.setAutoscrolls(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        createTestsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filePath = pathBox.getText();
                if (filePath.equals("")) {
                    JOptionPane.showMessageDialog(null, "   \nPlease select a XML file containing the UML diagram form which you want create tests.", "Info:", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                File file = new File(filePath);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);

                try {
                    DocumentBuilder dBuilder = factory.newDocumentBuilder();
                    Document doc = dBuilder.parse(file);

                    Component component = (Component) e.getSource();
                    JFrame form = (JFrame) SwingUtilities.getRoot(component);
                    Node diagram = EANodeCollector.getActivityDiagram(doc, (TracerForm) form);
                    if (diagram != null) {
                        HashMap<String, ActivityNode> activityNodeMap = new HashMap<String, ActivityNode>();
                        EANodeCollector.collectNodes(doc, diagram, activityNodeMap);
                        makeTests(activityNodeMap);
                    }
                } catch (Exception exp) {
                    JOptionPane.showMessageDialog(null, exp.getMessage() + "\n" + e.toString(), "Exception in TracerForm", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        final JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("xml files (*.xml)", "xml");
        fc.setFileFilter(xmlfilter);
        //TODO change when supporting other UML modeling applications
        fc.setDialogTitle("Open XMI file exported from Enterprise Architect");

        selectFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fc.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    pathBox.setText(file.getPath());
                }
            }
        });

        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    URL url = new URL("http://joao-carloto.github.io/TestTracer/");
                    Utils.openWebpage(url);
                } catch (MalformedURLException exp) {
                    JOptionPane.showMessageDialog(null, exp.getMessage(), "Malformed URL Exception", JOptionPane.ERROR_MESSAGE);
                }
            }
        });


        setSize(900, 600);
        setLocationRelativeTo(null);
        pack();
        setVisible(true);
    }

    //TODO should this be here?
    public void makeTests(HashMap<String, ActivityNode> actNodeMap) {
        try {
            if (actNodeMap.size() > 0) {
                Vector<Vector<String>> activityPaths = createActivityPaths(actNodeMap);
                //TODO call the loop detector in another way
                activityPaths = removeDuplicateLoops(activityPaths, actNodeMap);
                //Yes, we are doing it again because now we know where the loops are and the result set may be smaller
                activityPaths = createActivityPaths(actNodeMap);
                activityPaths = removeDuplicateLoops(activityPaths, actNodeMap);
                Collections.sort(activityPaths, new Comparator<Vector<String>>() {
                    @Override
                    public int compare(Vector<String> v1, Vector<String> v2) {
                        return v2.size() - v1.size();
                    }
                });
                Vector<Vector<String>> tests = TestWriter.writeTests(activityPaths, actNodeMap);
                String printOutput = "";
                printOutput += "\n" + tests.size() + " Test Cases";
                for (int i = 0; i < tests.size(); i++) {
                    Vector<String> test = tests.get(i);
                    printOutput += "\n\n\nTest nº" + (i + 1) + "\n";
                    for (int j = 0; j < test.size(); j++) {
                        printOutput += "\n" + (j + 1) + ". " + test.get(j);
                    }
                }
                this.textArea1.setText(printOutput);
                this.textArea1.setCaretPosition(0);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + e.toString(), "Exception in main", JOptionPane.ERROR_MESSAGE);
        }
    }
}
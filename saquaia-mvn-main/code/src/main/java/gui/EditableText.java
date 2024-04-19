package gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author Martin
 */
public class EditableText extends JPanel {
    private boolean enabled = true;
    
    private JLabel label;
    private JTextField textField;
    private JButton button;
    private TextChangeListener listener;
    
    public EditableText(String label, String value) {
        super(new BorderLayout());
        
        if (label != null) {
            this.label = new JLabel(label); 
            add(this.label, BorderLayout.NORTH);
        }
        
        textField = new JTextField(value);
        textField.setEditable(false);
        add(textField, BorderLayout.CENTER);
    }
    
    public void setChangeListener(TextChangeListener listener) {
        this.listener = listener;
        button = new JButton("Edit");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!enabled) return;
                String newValue = JOptionPane.showInputDialog(textField, "New value: ", textField.getText());
                if (enabled && newValue != null) {
                    listener.onChange(newValue); 
                }
            }
        });
        add(button, BorderLayout.EAST);
    }
    
    public void setLabel(String label) {
        if (this.label == null) return;
        this.label.setText(label);
    }
    
    public void setValue(String value) {
        this.textField.setText(value);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.enabled = enabled;
        button.setEnabled(enabled);
    }

    public static interface TextChangeListener {
        public void onChange(String newValue);
    }
    
    public static void main(String[] args) {
        EditableText e = new EditableText("Label:", "initial value");
        e.setChangeListener(new TextChangeListener() {
            @Override
            public void onChange(String newValue) {
                e.setValue(newValue);
            }
        });
        JFrame frame = new JFrame("ListDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setLayout(new BorderLayout());
        frame.add(e, BorderLayout.CENTER);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}

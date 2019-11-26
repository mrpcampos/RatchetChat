import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

public class JanelaChat {

    Cliente cliente;

    String appName = "RatchetChat";
    JFrame newFrame = new JFrame(appName);
    JButton sendMessage;
    JTextField messageBox;
    JTextArea chatBox;

    String username;

    public JanelaChat(Cliente cliente){
        this.cliente = cliente;
        do {
            username = JOptionPane.showInputDialog(null, "Digite seu nome:");
        } while (username == null || username.trim().length() <= 1 || username.contains(":"));
    }

    public void abrirChat() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel southPanel = new JPanel();
        southPanel.setBackground(Color.BLUE);
        southPanel.setLayout(new GridBagLayout());

        messageBox = new JTextField(30);
        messageBox.addActionListener(this::enviarMensagem);
        messageBox.requestFocusInWindow();

        sendMessage = new JButton("Send Message");
        sendMessage.setEnabled(false);
        sendMessage.addActionListener(this::enviarMensagem);

        chatBox = new JTextArea();
        chatBox.setEditable(false);
        chatBox.setFont(new Font("Serif", Font.PLAIN, 15));
        chatBox.setLineWrap(true);

        mainPanel.add(new JScrollPane(chatBox), BorderLayout.CENTER);

        GridBagConstraints left = new GridBagConstraints();
        left.anchor = GridBagConstraints.LINE_START;
        left.fill = GridBagConstraints.HORIZONTAL;
        left.weightx = 512.0D;
        left.weighty = 1.0D;

        GridBagConstraints right = new GridBagConstraints();
        right.insets = new Insets(0, 10, 0, 0);
        right.anchor = GridBagConstraints.LINE_END;
        right.fill = GridBagConstraints.NONE;
        right.weightx = 1.0D;
        right.weighty = 1.0D;

        southPanel.add(messageBox, left);
        southPanel.add(sendMessage, right);

        mainPanel.add(BorderLayout.SOUTH, southPanel);

        newFrame.add(mainPanel);
        newFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        newFrame.setSize(600, 530);
        newFrame.setVisible(true);
    }

    public void permitirEnvioMensagens(boolean bool){
        sendMessage.setEnabled(bool);
    }

    public void adicionarMensagem(String msg, String remetente) {
        chatBox.append("<" + remetente + ">:  " + msg
                + "\n");
    }

    public void enviarMensagem(ActionEvent event) {
        if (messageBox.getText().trim().length() > 1) {
            if (messageBox.getText().equals(".clear")) {
                chatBox.setText("Cleared all messages\n");
                messageBox.setText("");
            } else {
                cliente.enviarMensagem(messageBox.getText(), username);
                messageBox.setText("");
            }
        }
        messageBox.requestFocusInWindow();
    }

}

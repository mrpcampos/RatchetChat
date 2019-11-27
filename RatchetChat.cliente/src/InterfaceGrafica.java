import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.*;

public class InterfaceGrafica {

    private static String appName = "RatchetChat";
    private JFrame newFrame = new JFrame(appName);
    private JButton sendMessage;
    private JTextField messageBox;
    private JTextArea chatBox;

    String username;

    InterfaceGrafica() {
    }

    static String menu(String[] opcoes) {
        return (String) JOptionPane.showInputDialog(null, "O que deseja fazer?", appName, JOptionPane.QUESTION_MESSAGE, null, opcoes, "0");
    }

    static String[] gerarOuResgatarChavesECertificado() {
        JTextField identificador = new JTextField();
        JTextField senha = new JPasswordField();
        Object[] message = {
                "Identificador:", identificador,
                "Password:", senha
        };
        do {
            Integer option = JOptionPane.showConfirmDialog(null, message, "Digite seu identificador e sua senha para o servidor de certificados, caso não possua pode criar agora:", JOptionPane.OK_CANCEL_OPTION);
        } while (identificador.getText().trim().length() <= 0 || senha.getText().trim().length() <= 2);
        return new String[]{identificador.getText().trim(), senha.getText().trim()};
    }

    static String definirIpServidor(String valorAtual) {
        return JOptionPane.showInputDialog(null, "Digite o ip do Servidor:", valorAtual);
    }

    static int definirPortaDoDestinatario(int valorAtual) {
        return Integer.valueOf(JOptionPane.showInputDialog(null, "Digite a porta da pessoa com quem quer conversar:", valorAtual));
    }

    static String definirIpDoDestinatario(String valorAtual) {
        return JOptionPane.showInputDialog(null, "Digite o ip da pessoa com quem quer conversar:", valorAtual);
    }

    static int definirPortaServidor(int valorAtual) {
        return Integer.valueOf(JOptionPane.showInputDialog(null, "Digite a porta do Servidor:", valorAtual));
    }

    static int definirPortaConversas(int valorAtual) {
        return Integer.valueOf(JOptionPane.showInputDialog(null, "Digite a porta na qual deseja receber pedidos de conversa:", valorAtual));
    }

    /**
     *
     * @param msg explicação do erro para aparecer na dialog box
     * @param messageType the type of message to be displayed:
     * <code>ERROR_MESSAGE</code>,
     * <code>INFORMATION_MESSAGE</code>,
     * <code>WARNING_MESSAGE</code>,
     * <code>QUESTION_MESSAGE</code>,
     * or <code>PLAIN_MESSAGE</code>
     */
    public static void showDialog(String msg, String title, int messageType){
        JOptionPane.showMessageDialog(null, msg, title, messageType);
    }

//    public void abrirChat() {
//        JPanel mainPanel = new JPanel();
//        mainPanel.setLayout(new BorderLayout());
//
//        JPanel southPanel = new JPanel();
//        southPanel.setBackground(Color.BLUE);
//        southPanel.setLayout(new GridBagLayout());
//
//        messageBox = new JTextField(30);
//        messageBox.addActionListener(this::enviarMensagem);
//        messageBox.requestFocusInWindow();
//
//        sendMessage = new JButton("Send Message");
//        sendMessage.setEnabled(false);
//        sendMessage.addActionListener(this::enviarMensagem);
//
//        chatBox = new JTextArea();
//        chatBox.setEditable(false);
//        chatBox.setFont(new Font("Serif", Font.PLAIN, 15));
//        chatBox.setLineWrap(true);
//
//        mainPanel.add(new JScrollPane(chatBox), BorderLayout.CENTER);
//
//        GridBagConstraints left = new GridBagConstraints();
//        left.anchor = GridBagConstraints.LINE_START;
//        left.fill = GridBagConstraints.HORIZONTAL;
//        left.weightx = 512.0D;
//        left.weighty = 1.0D;
//
//        GridBagConstraints right = new GridBagConstraints();
//        right.insets = new Insets(0, 10, 0, 0);
//        right.anchor = GridBagConstraints.LINE_END;
//        right.fill = GridBagConstraints.NONE;
//        right.weightx = 1.0D;
//        right.weighty = 1.0D;
//
//        southPanel.add(messageBox, left);
//        southPanel.add(sendMessage, right);
//
//        mainPanel.add(BorderLayout.SOUTH, southPanel);
//
//        newFrame.add(mainPanel);
//        newFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        newFrame.setSize(600, 530);
//        newFrame.setVisible(true);
//    }
//
//    public void permitirEnvioMensagens(boolean bool) {
//        sendMessage.setEnabled(bool);
//    }
//
//    public void adicionarMensagem(String msg, String remetente) {
//        chatBox.append("<" + remetente + ">:  " + msg
//                + "\n");
//    }
//
//    public void enviarMensagem(ActionEvent event) {
//        if (messageBox.getText().trim().length() > 1) {
//            if (messageBox.getText().equals(".clear")) {
//                chatBox.setText("Cleared all messages\n");
//                messageBox.setText("");
//            } else {
//                proxy.enviarMensagem(messageBox.getText(), username);
//                messageBox.setText("");
//            }
//        }
//        messageBox.requestFocusInWindow();
//    }

}

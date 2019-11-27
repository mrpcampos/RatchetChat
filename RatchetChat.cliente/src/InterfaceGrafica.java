import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class InterfaceGrafica {

    private static String appName = "RatchetChat";
    private JFrame newFrame = new JFrame(appName);
    private JButton sendMessage;
    private JTextField messageBox;
    private JTextArea chatBox;

    String username;

    InterfaceGrafica(String identificador) {
        username = identificador;
    }

    static String menu(String[] opcoes) {
//        JComboBox<String> combo = new JComboBox<>(opcoes);
//        JOptionPane pane = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE);
//        // Configure via set methods
//        JDialog dialog = pane.createDialog(null, "Menu");
//        // the line below is added to the example from the docs
////        dialog.setModal(false); // this says not to block background components
//        dialog.setModalityType(Dialog.ModalityType.MODELESS);
//
//        dialog.setVisible(true);
//        return (String) pane.getValue();
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
     * @param msg         explicação do erro para aparecer na dialog box
     * @param messageType the type of message to be displayed:
     *                    <code>ERROR_MESSAGE</code>,
     *                    <code>INFORMATION_MESSAGE</code>,
     *                    <code>WARNING_MESSAGE</code>,
     *                    <code>QUESTION_MESSAGE</code>,
     *                    or <code>PLAIN_MESSAGE</code>
     */
    public static void showDialog(String msg, String title, int messageType) {
        JOptionPane.showMessageDialog(null, msg, title, messageType);
    }

    public void abrirChat() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel southPanel = new JPanel();
        southPanel.setBackground(Color.BLUE);
        southPanel.setLayout(new GridBagLayout());

        messageBox = new JTextField(30);
        messageBox.requestFocusInWindow();

        sendMessage = new JButton("Send Message");
        sendMessage.setEnabled(false);

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

    public void addListeners(ActionListener ac) {
        messageBox.addActionListener(ac);
        sendMessage.addActionListener(ac);
    }

    public void permitirEnvioMensagens(boolean bool) {
        messageBox.setEnabled(bool);
        sendMessage.setEnabled(bool);
    }

    public void receberMensagem(String pct) {
        int divider = pct.lastIndexOf(":");
        String msg = pct.substring(0, divider);
        String username = pct.substring(pct.lastIndexOf(":") + 1);
        this.adicionarMensagem(msg, username);
    }

    public void adicionarMensagem(String msg, String remetente) {
        chatBox.append("<" + remetente + ">:  " + msg
                + "\n");
    }

    public String popMensagemParaEnviar() {
        String pct = messageBox.getText() + ":" + username;
        limparCaixaDeEnvio();
        return pct;
    }

    public String getTextoCaixaEnvio() {
        return messageBox.getText();
    }

    public void limparChat() {
        chatBox.setText("Cleared all messages\n");
        messageBox.requestFocusInWindow();
        limparCaixaDeEnvio();
    }

    public void limparCaixaDeEnvio() {
        messageBox.setText("");
    }

    public String getUsername() {
        return username;
    }
}


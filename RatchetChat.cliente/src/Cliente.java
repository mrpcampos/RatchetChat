/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Cliente {

    private Socket cliente;
    private JanelaChat janelaChat;

    private String host;
    private int porta;

    private byte[] ga;
    private byte[] gb;

    private PrintStream saida;

    public Cliente(String host, int porta) {
        this.host = host;
        this.porta = porta;
    }

    public void executa() throws UnknownHostException, IOException {
        Socket cliente = new Socket(this.host, this.porta);
        this.janelaChat = new JanelaChat(this);

        // thread para receber mensagens do servidor
        InputStream is = cliente.getInputStream();
        new Thread(() -> {
            // recebe msgs do servidor e imprime na tela
            Scanner s = new Scanner(is);
            while (s.hasNextLine()) {
                String pct = s.nextLine();
                int divider = pct.lastIndexOf(":");
                String msg = pct.substring(0, divider);
                String username = pct.substring(pct.lastIndexOf(":") + 1);
                this.janelaChat.adicionarMensagem(msg, username);
            }
        }).start();

        // Abre janela de chat e prepara para enviar mensagens
        this.saida = new PrintStream(cliente.getOutputStream());
        janelaChat.abrirChat();
        enviarMensagem(this.janelaChat.username + " se conectou ao servidor!", "");
        this.janelaChat.permitirEnvioMensagens(true);

    }

    public void enviarMensagem(String msg, String remetente) {
        saida.println(msg + ":" + remetente);
    }
}

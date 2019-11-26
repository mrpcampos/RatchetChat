/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Servidor {

    private int porta;
    private List<PrintStream> clientes;


    public Servidor(int porta) {
        this.porta = porta;
        this.clientes = new ArrayList<PrintStream>();
    }

    public void executa() throws IOException {
        ServerSocket servidor = new ServerSocket(this.porta);
        System.out.println("Porta " + this.porta + " aberta!");

        while (true) {
            // aceita um cliente
            Socket cliente = servidor.accept();
            System.out.println("Nova conexão com o cliente " + cliente.getInetAddress().getHostAddress());

            // adiciona saida do cliente à lista
            PrintStream ps = new PrintStream(cliente.getOutputStream());
            this.clientes.add(ps);

            // Inicia uma thread para tratar o cliente
            InputStream is = cliente.getInputStream();
            new Thread(() -> {
                Scanner s = new Scanner(is);
                while (s.hasNextLine()) {
                    this.distribuiMensagem(s.nextLine());
                }
                s.close();
                this.clientes.remove(ps);
            }).start();
        }
    }

    public void distribuiMensagem(String msg) {
        // envia msg para todos os clientes
        System.out.println(msg);
        for (PrintStream cliente : this.clientes) {
            cliente.println(msg);
        }
    }

    public static void main(String[] args) throws IOException {
        new Servidor(12345).executa();
    }
}
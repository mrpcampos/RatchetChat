import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class Proxy {

    private static Proxy instance;

    private Proxy() {
    }

    public static Proxy getInstance() {
        if (instance == null)
            instance = new Proxy();
        return instance;
    }

    public Socket conectarparaConversa(String ipDoDestinatario, int portaDoDestinatario) throws IOException {
        return new Socket(ipDoDestinatario, portaDoDestinatario);
    }

    public Resposta fazerRequisicao(String ip, int porta, Requisicao req) throws IOException {
        Socket servidor = new Socket(ip, porta);
        ObjectOutputStream oos = new ObjectOutputStream(servidor.getOutputStream());
        oos.writeObject(req);

        ObjectInputStream ois = new ObjectInputStream(servidor.getInputStream());
        while (true) {
            try {
                return (Resposta) ois.readObject();
            } catch (ClassNotFoundException e) {
                System.out.println("Não foi possível ler a resposta do pedido.");
                e.printStackTrace();
            }
            try {
                wait(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Thread abrirPortaParaConexoes(Pessoa pessoa, Consumer tratarTentativaDeConexao) throws IOException {
        ServerSocket servidorConversas = new ServerSocket(pessoa.getPortaConversas());
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Socket outraPessoa = servidorConversas.accept();
                    tratarTentativaDeConexao.accept(outraPessoa);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        );
        thread.start();
        return thread;
    }
}

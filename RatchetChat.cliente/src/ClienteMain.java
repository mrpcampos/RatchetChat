import javax.swing.*;
import java.io.IOException;
import java.net.UnknownHostException;

public class ClienteMain {

    public static void main(String[] args) throws UnknownHostException, IOException {
        String ip = JOptionPane.showInputDialog(null, "Digite o ip do Servidor:","127.0.0.1");
        new Cliente(ip, 12345).executa();
    }
}

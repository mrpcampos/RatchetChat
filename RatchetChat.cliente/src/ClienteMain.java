import javax.swing.*;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.function.Function;

public class ClienteMain {

    Proxy proxy;
    private Pessoa pessoa;
    private Thread threadRecebimentoPedidosDeConversa;

    private static final String DEFINIR_IP_SERVIDOR = "Definir servidor de certificados";
    private static final String GERAR_OU_RESGATAR_CHAVES_E_CERTIFICADO = "Gerar certificado e chave RSA";
    private static final String DEFINIR_PORTA_ENTRADA = "Definir porta para receber coneções";
    private static final String PERMITIR_TENTATIVAS_DE_CONEXAO = "Abrir a porta especificada para receber conexões";
    private static final String IGNORAR_TENTATIVAS_DE_CONEXAO = "Parar de ouvir a porta especificada para receber conexões";
    private static final String CONECTAR_COM_ALGUEM = "Conectar com Alguem";
    private static String[] opcoes = {DEFINIR_IP_SERVIDOR, GERAR_OU_RESGATAR_CHAVES_E_CERTIFICADO, DEFINIR_PORTA_ENTRADA, PERMITIR_TENTATIVAS_DE_CONEXAO, IGNORAR_TENTATIVAS_DE_CONEXAO, CONECTAR_COM_ALGUEM};

    ClienteMain() throws UnknownHostException, IOException {
        proxy = Proxy.getInstance();
        this.pessoa = new Pessoa();
        definirIpServidor();
        definirPortaServidor();
        definirPortaConversas();
        atualizarChaveECertificado();
        menu();
    }

    private void menu() {
        while (true) {
            String value = InterfaceGrafica.menu(opcoes);
            if (value == null)
                System.exit(0);
            switch (value) {
                case DEFINIR_IP_SERVIDOR:
                    definirIpServidor();
                    break;
                case GERAR_OU_RESGATAR_CHAVES_E_CERTIFICADO:
                    atualizarChaveECertificado();
                    break;
                case DEFINIR_PORTA_ENTRADA:
                    definirPortaServidor();
                    break;
                case PERMITIR_TENTATIVAS_DE_CONEXAO:
                    liberarPortaParaConexao();
                    break;
                case IGNORAR_TENTATIVAS_DE_CONEXAO:
                    pararReceberPedidosDeConexao();
                    break;
                case CONECTAR_COM_ALGUEM:
                    coversarComAlguem();
                    break;
                default:
                    System.out.println("Como caralhos alguem conseguiu cair nesse dafault?");
            }
        }
    }

    private void coversarComAlguem() {
        String ipDestinatario = verificarNull(InterfaceGrafica::definirIpDoDestinatario, "127.0.0.1");
        int portaDestinatario = verificarNull(InterfaceGrafica::definirPortaDoDestinatario, 8088);
        try {
            new ChatComCatraca(pessoa, proxy.conectarparaConversa(ipDestinatario, portaDestinatario), ChatComCatraca.A);
        } catch (IOException e) {
            InterfaceGrafica.showDialog("Erro na execução do chat.", "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void liberarPortaParaConexao() {
        if (threadRecebimentoPedidosDeConversa == null) {
            try {
                proxy.abrirPortaParaConexoes(pessoa, this::tratarTentativaDeConexaoRecebida);
                InterfaceGrafica.showDialog("Porta aberta com sucesso, agora é só alguem tentar conectar com você.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            InterfaceGrafica.showDialog("Porta já esta aberta", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void pararReceberPedidosDeConexao() {
        if (threadRecebimentoPedidosDeConversa != null) {
            threadRecebimentoPedidosDeConversa.stop();
            InterfaceGrafica.showDialog("Porta desligada com sucesso", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } else {
            InterfaceGrafica.showDialog("Porta já esta desligada", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tratarTentativaDeConexaoRecebida(Object outraPessoa) {
        try {
            new ChatComCatraca(pessoa, (Socket) outraPessoa, ChatComCatraca.B);
        } catch (IOException e) {

            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void definirPortaServidor() {
        this.pessoa.setPortaServidor(verificarNull(InterfaceGrafica::definirPortaServidor, this.pessoa.getPortaServidor()));
    }

    private void definirPortaConversas() {
        this.pessoa.setPortaConversas(verificarNull(InterfaceGrafica::definirPortaConversas, this.pessoa.getPortaConversas()));
    }

    void definirIpServidor() {
        this.pessoa.setIpServidor(verificarNull(InterfaceGrafica::definirIpServidor, pessoa.getIpServidor()));
    }

    //Quando você brinca com lambdas e generics
    <R, T> T verificarNull(Function<R, T> c, R parameter) {
        T value = c.apply(parameter);
        if (value == null) {
            System.exit(0);
            return null; //Compilador sendo chato
        } else {
            return value;
        }
    }

    /**
     * Envia uma requisição para o servidor, se o identificador nunca foi usado um novo certificado e par de senhas são gerados, caso contrário os anteriores são retornados
     */
    void atualizarChaveECertificado() {
        String[] loginSenha = InterfaceGrafica.gerarOuResgatarChavesECertificado();
        try {
            Resposta res = proxy.fazerRequisicao(pessoa.getIpServidor(), pessoa.getPortaServidor(), new RequisicaoDeChavesECertificado(loginSenha[0], loginSenha[1]));
            RespostaDeChavesECertificado resCEC = (RespostaDeChavesECertificado) res;
            if (resCEC.resposta.equals(Mensagens.ACEITO)) {
                pessoa.inicializar(loginSenha[0], resCEC.getCertificate(), resCEC.getPrivateKey());
                pessoa.setIdentificador(resCEC.getCertificate().getIssuerX500Principal().getName().split("=")[1]);
            } else {
                System.out.println("Pedido recusado");
                System.out.println("resposta: " + resCEC.resposta);
                System.out.println("Chave publica: " + resCEC.getCertificate().getPublicKey());
                System.out.println("Chave privada: " + resCEC.getPrivateKey());
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) throws UnknownHostException, IOException {
        new ClienteMain();
    }
}

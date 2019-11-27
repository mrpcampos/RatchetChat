import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class Servidor {

    private int porta;

    public Servidor(int porta) {
        this.porta = porta;
    }

    public void executar() throws IOException {
        prepara();
        ServerSocket servidor = new ServerSocket(this.porta);
        System.out.println("Porta " + this.porta + " aberta!");

        while (true) {
            // aceita um cliente
            Socket cliente = servidor.accept();
            System.out.println("Nova conexão com o cliente " + cliente.getInetAddress().getHostAddress());

            // adiciona saida do cliente à lista

            // Inicia uma thread para tratar o cliente
            new Thread(() -> {
                try {
                    ObjectInputStream ois = new ObjectInputStream(cliente.getInputStream());
                    Requisicao req = (Requisicao) ois.readObject();
                    ObjectOutputStream oos = new ObjectOutputStream(cliente.getOutputStream());

                    if (req.getTipo().equals(Mensagens.PEGAR_CERTIFICADO_E_CHAVES)) {
                        //Gera chaves e certificado, guarda essas informações e prepara a resposta
                        RespostaDeChavesECertificado resCEC = this.pegarChavesECertificado((RequisicaoDeChavesECertificado) req);

                        oos.writeObject(resCEC);
                    } else if (req.getTipo().equals(Mensagens.CONSULTAR_CERTIFICADO)) {
                        //Procura e retorna certificado existente, caso não encontre recusa a tentativa
                        RequisicaoConsultaCertificado reqCC = (RequisicaoConsultaCertificado) req;
                        RespostaConsultaCertificado resC = consultarCertificado(reqCC);

                        oos.writeObject(resC);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Deve inicializar variáveis e outras informações para gerar certificados e chaves RSA
     */
    private void prepara() {
        Security.addProvider(new BouncyCastleFipsProvider());
        CriptoUtils.Setup();
    }

    private RespostaConsultaCertificado consultarCertificado(RequisicaoConsultaCertificado reqCC) {
        String identificador = reqCC.getIdentificador();
        try {
            if (CriptoUtils.HasCert(identificador)) {
                X509Certificate certificado = CriptoUtils.GetCert(identificador);
                if (certificado != null) {
                    certificado.checkValidity();
                    return new RespostaConsultaCertificado(certificado);
                }
            }
        } catch (CertificateException | KeyStoreException e) {
            e.printStackTrace();
        } catch (Exception e) {//Biblioteca utiliza throw exception desnecessáriamente, cláusula não alcançavel
            e.printStackTrace();
        }
        return new RespostaConsultaCertificado();
    }

    private RespostaDeChavesECertificado pegarChavesECertificado(RequisicaoDeChavesECertificado reqCEC) {
        String identificador = reqCEC.getIdentificador();
        String segredo = reqCEC.getIdentificador();
        char[] senha = CriptoUtils.pbkdf2KeyGenerator(segredo, identificador + segredo, 100000);
        try {
            X509Certificate certificate;
            PrivateKey rsaPrivateKey;
            if (CriptoUtils.HasCert(identificador)) {
                certificate = CriptoUtils.GetCert(identificador);
                rsaPrivateKey = CriptoUtils.GetKey(identificador, senha);
            } else {
                // RSA Pair
                KeyPair rsaKeyPair = CriptoUtils.generateRSAKeyPair();
                rsaPrivateKey = rsaKeyPair.getPrivate();

                // Certificate
                certificate = CriptoUtils.makeCertificate(rsaKeyPair.getPrivate(), rsaKeyPair.getPublic(), identificador);

                CriptoUtils.StoreCert(identificador, senha, certificate);
                CriptoUtils.StoreKey(identificador, senha, rsaPrivateKey, certificate);
            }
            if (certificate != null && rsaPrivateKey != null) {
                return new RespostaDeChavesECertificado(certificate, rsaPrivateKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new RespostaDeChavesECertificado();
    }

    public static void main(String[] args) throws IOException {
        new Servidor(12345).executar();
    }
}
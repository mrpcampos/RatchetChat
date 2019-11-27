import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;

public class ChatComCatraca {
    public static final String A = "A";
    public static final String B = "B";
    private final String AouB;
    private Pessoa eu;

    private ObjectInputStream receber;
    private ObjectOutputStream enviar;

    private String identificadorOutraPessoa;
    private X509Certificate certificadoOutraPessoa;


    private InterfaceGrafica ig;
    private Proxy proxy;

    /**
     * @param eu          pessoa dona da máquina
     * @param outraPessoa pessoa com quem se está conversando
     * @param AouB        possui o valor A ou B, útil para definir quem começa o DH
     * @throws IOException se der merda
     */
    public ChatComCatraca(Pessoa eu, Socket outraPessoa, String AouB) throws Exception {
        this.AouB = AouB;
        this.ig = new InterfaceGrafica();
        this.proxy = Proxy.getInstance();
        this.eu = eu;

        CriptoUtils.SetProvider();

        enviar = new ObjectOutputStream(outraPessoa.getOutputStream());
        receber = new ObjectInputStream(outraPessoa.getInputStream());

        KeyPair chavesDH = CriptoUtils.generateDHNumberPair();

        try {
            if (this.AouB.equals(A)) {
                caminhoA(chavesDH);
            } else if (this.AouB.equals(B)) {
                caminhoB(chavesDH);
            } else {
                throw new IllegalArgumentException("O valor de AouB deve ser A ou B.");
            }
        } catch (Exception e) {
            InterfaceGrafica.showDialog("Erro nos primeiros passos do DH.", "Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    private void caminhoA(KeyPair meuParDeChavesDH) throws Exception {
        //A -> B: g^a, A
        PublicKey ga = meuParDeChavesDH.getPublic();
        enviar.writeObject(ga);                     //g^a
        enviar.writeObject(eu.getIdentificador());  //A

        //B -> A: g^b, CERTb, Eg^ab{ SIGb{g^a, g^b, A}}
        PublicKey chavePublicaOutraPessoa = (PublicKey) receber.readObject();     //g^b
        Key gab = chaveCompartilhada(chavePublicaOutraPessoa, meuParDeChavesDH.getPrivate());          //g^ab

        certificadoOutraPessoa = (X509Certificate) receber.readObject();//Suposto CERTb
        String textoCifrado = (String) receber.readObject();            //Eg^ab{ SIGb{g^a, g^b, A}}
        String textoDecifrado = CriptoUtils.DecifrarMensagem(textoCifrado, gab);
        identificadorOutraPessoa = certificadoOutraPessoa.getIssuerX500Principal().getName().split("=")[1];
        byte[] dadosQueDevemEstarAssinados =
                concatByteArrays(
                        meuParDeChavesDH.getPublic().getEncoded(),
                        chavePublicaOutraPessoa.getEncoded(),
                        eu.getIdentificador().getBytes());
        if (!verificarCertificadoEAssinatura(textoDecifrado, dadosQueDevemEstarAssinados)) {
            throw new SecurityException("Certificado RSA inválido, possível ataque!");
        }
        // A -> B: CERTa, Eg^ab {SIGa{g^a, g^b, B}}
        X509Certificate certificadoA = eu.getCertificate(); //CERTa
        byte[] infoParaAssinar =
                concatByteArrays(meuParDeChavesDH.getPublic().getEncoded(),
                        chavePublicaOutraPessoa.getEncoded(),
                        identificadorOutraPessoa.getBytes());
        String msgAssinada = CriptoUtils.SignString(eu.getPrivateKey(), infoParaAssinar);
        String mensagemCriptografada = CriptoUtils.CifrarMensagem(msgAssinada, gab);
        enviar.writeObject(certificadoA);                                           //CERTa
        enviar.writeObject(mensagemCriptografada);                                  //Ek {SIGa{g^a, g^b, B}
        InterfaceGrafica.showDialog("Deu tudo certo no caminho B!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void caminhoB(KeyPair meuParDeChavesDH) throws Exception {
        //A -> B: g^a, A
        PublicKey chavePublicaOutraPessoa = (PublicKey) receber.readObject();
        this.identificadorOutraPessoa = (String) receber.readObject();
        Key gab = chaveCompartilhada(chavePublicaOutraPessoa, meuParDeChavesDH.getPrivate());

        //B -> A: g^b, CERTb, Eg^ab{ SIGb{g^a, g^b, A}}
        PublicKey gb = meuParDeChavesDH.getPublic();     //g^b
        X509Certificate certificadoB = eu.getCertificate(); //CERTb
        byte[] infoParaAssinar =
                concatByteArrays(chavePublicaOutraPessoa.getEncoded(),
                        meuParDeChavesDH.getPublic().getEncoded(),
                        identificadorOutraPessoa.getBytes());
        String msgAssinada = CriptoUtils.SignString(eu.getPrivateKey(), infoParaAssinar);
        String mensagemCriptografada = CriptoUtils.CifrarMensagem(msgAssinada, gab);//Ek {SIGb{g^a, g^b, A}
        enviar.writeObject(gb);                     //g^b
        enviar.writeObject(certificadoB);           //CERTb
        enviar.writeObject(mensagemCriptografada);  //Ek {SIGb{g^a, g^b, A}

        //A -> B: CERTa, Eg^ab {SIGa{g^a, g^b, B}}
        certificadoOutraPessoa = (X509Certificate) receber.readObject();//Suposto CERTa
        String textoCifrado = (String) receber.readObject();            //Eg^ab{ SIGa{g^a, g^b, B}}
        String textoDecifrado = CriptoUtils.DecifrarMensagem(textoCifrado, gab);
        identificadorOutraPessoa = certificadoOutraPessoa.getIssuerX500Principal().getName().split("=")[1];
        byte[] dadosQueDevemEstarAssinados =
                concatByteArrays(
                        chavePublicaOutraPessoa.getEncoded(),
                        meuParDeChavesDH.getPublic().getEncoded(),
                        eu.getIdentificador().getBytes());
        if (!verificarCertificadoEAssinatura(textoDecifrado, dadosQueDevemEstarAssinados)) {
            throw new SecurityException("Certificado RSA inválido, possível ataque!");
        }
        InterfaceGrafica.showDialog("Deu tudo certo no caminho A!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

    }

    private boolean verificarCertificadoEAssinatura(String supostoTextoAssinado, byte[] dadosSupostamenteNaAssinatura) throws Exception {
        Requisicao req = new RequisicaoConsultaCertificado(identificadorOutraPessoa);
        RespostaConsultaCertificado res;
        res = (RespostaConsultaCertificado) this.proxy.fazerRequisicao(eu.getIpServidor(), eu.getPortaServidor(), req);
        if (res.resposta.equals(Mensagens.NEGADO))
            throw new InvalidKeyException("Certificado negado, identificador não encontrado");
        return CriptoUtils.CheckSign(res.getCertificado().getPublicKey(), supostoTextoAssinado, dadosSupostamenteNaAssinatura);
    }

    private byte[] concatByteArrays(byte[] ba1, byte[] ba2, byte[] ba3) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(ba1);
        outputStream.write(ba2);
        outputStream.write(ba3);
        return outputStream.toByteArray();
    }

    private Key chaveCompartilhada(PublicKey chavePublicaOutraPessoa, PrivateKey minhaChavePrivada) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
        KeyAgreement keyAgree = KeyAgreement.getInstance("DH", "BCFIPS");

        keyAgree.init(minhaChavePrivada);
        keyAgree.doPhase(chavePublicaOutraPessoa, true);
        MessageDigest hash = MessageDigest.getInstance("SHA256", "BCFIPS");
        return new SecretKeySpec(hash.digest(keyAgree.generateSecret()), "AES");
    }
}

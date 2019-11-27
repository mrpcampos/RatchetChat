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

    private String identOutro;
    private X509Certificate certOutro;

    private Catraca catraca;

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
        this.catraca = new Catraca(CriptoUtils.generateDHNumberPair());

        enviar = new ObjectOutputStream(outraPessoa.getOutputStream());
        receber = new ObjectInputStream(outraPessoa.getInputStream());

        try {
            if (this.AouB.equals(A)) {
                caminhoA();
            } else if (this.AouB.equals(B)) {
                caminhoB();
            } else {
                throw new IllegalArgumentException("O valor de AouB deve ser A ou B.");
            }
        } catch (Exception e) {
            InterfaceGrafica.showDialog("Erro nos primeiros passos do DH.", "Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }
    private void caminhoA() throws Exception {
        //A -> B: g^a, A
        enviar.writeObject(catraca.getMyPubKeyDH());  //g^a
        enviar.writeObject(eu.getIdentificador());  //A

        //B -> A: g^b, CERTb, Eg^ab{ SIGb{g^a, g^b, A}}
        catraca.setPubKeyOutro((PublicKey) receber.readObject());                            //g^b
        certOutro = (X509Certificate) receber.readObject();                                  //CERTb
        catraca.setSessioanKey(defSessionKey(catraca.getPubKeyOutro(), catraca.getMyPrivKeyDH()));//g^ab
        identOutro = certOutro.getIssuerX500Principal().getName().split("=")[1];
        verificarAutenticidade((String) receber.readObject(), catraca.getMyPubKeyDH(), catraca.getPubKeyOutro());

        // A -> B: CERTa, Eg^ab {SIGa{g^a, g^b, B}}
        enviar.writeObject(eu.getCertificate()); //CERTa
        enviar.writeObject(comprovarAutenticidade(catraca.getMyPubKeyDH(), catraca.getPubKeyOutro()));
        InterfaceGrafica.showDialog("Deu tudo certo no caminho A!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void caminhoB() throws Exception {
        //A -> B: g^a, A
        catraca.setPubKeyOutro((PublicKey) receber.readObject());   //g^a
        this.identOutro = (String) receber.readObject();            //A
        catraca.setSessioanKey(defSessionKey(catraca.getPubKeyOutro(), catraca.getMyPrivKeyDH()));//g^ab

        //B -> A: g^b, CERTb, Eg^ab{ SIGb{g^a, g^b, A}}
        enviar.writeObject(catraca.getMyPubKeyDH());    //g^b
        enviar.writeObject(eu.getCertificate());        //CERTb
        enviar.writeObject(comprovarAutenticidade(catraca.getPubKeyOutro(), catraca.getMyPubKeyDH()));


        //A -> B: CERTa, Eg^ab {SIGa{g^a, g^b, B}}
        certOutro = (X509Certificate) receber.readObject();             //CERTa
        verificarAutenticidade((String) receber.readObject(),
                catraca.getPubKeyOutro(),
                catraca.getMyPubKeyDH());//Eg^ab{ SIGa{g^a, g^b, B}}

        InterfaceGrafica.showDialog("Deu tudo certo no caminho B!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

    }

    //Ler Eg^ab{ SIGb{g^a, g^b, A}}
    private void verificarAutenticidade(String textoCriptografado, PublicKey pubKeyA, PublicKey pubKeyB) throws Exception {
        //Verifica se o certificado existe no servidor
        RespostaConsultaCertificado res = (RespostaConsultaCertificado) proxy.fazerRequisicao(eu.getIpServidor(), eu.getPortaServidor(), new RequisicaoConsultaCertificado(identOutro));
        if (res.resposta.equals(Mensagens.NEGADO) || !res.getCertificado().equals(certOutro)){
            System.out.println("RespostaConsulta: " + res.resposta);
            System.out.println("Certificado " + res.getCertificado().getPublicKey() +" " + res.getCertificado().getIssuerX500Principal().getName() + "");
            throw new SecurityException("Certificado forjado");
        }

        //Verifica se o certificado realmente assinou os valores
        byte[] concatBytes = concatByteArrays(pubKeyA.getEncoded(), pubKeyB.getEncoded(), eu.getIdentificador().getBytes());
        if (!CriptoUtils.CheckSign(certOutro.getPublicKey(), CriptoUtils.DecifrarMensagem(textoCriptografado, catraca.getSessionKey()), concatBytes)) {
            throw new SecurityException("Certificado RSA inválido, possível ataque!");
        }
    }

    //Gerar Eg^ab {SIGa{g^a, g^b, B}}
    private String comprovarAutenticidade(PublicKey pubKeyA, PublicKey pubKeyB) throws Exception {
        return CriptoUtils.CifrarMensagem(CriptoUtils.SignString(eu.getPrivateKey(),
                concatByteArrays(pubKeyA.getEncoded(), pubKeyB.getEncoded(), identOutro.getBytes())), catraca.getSessionKey());
    }

    private byte[] concatByteArrays(byte[] ba1, byte[] ba2, byte[] ba3) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(ba1);
        outputStream.write(ba2);
        outputStream.write(ba3);
        return outputStream.toByteArray();
    }

    private Key defSessionKey(PublicKey keyPubDHOutro, PrivateKey myPrivKeyDH) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
        KeyAgreement keyAgree = KeyAgreement.getInstance("DH", "BCFIPS");

        keyAgree.init(myPrivKeyDH);
        keyAgree.doPhase(keyPubDHOutro, true);
        MessageDigest hash = MessageDigest.getInstance("SHA256", "BCFIPS");
        return new SecretKeySpec(hash.digest(keyAgree.generateSecret()), "AES");
    }
}

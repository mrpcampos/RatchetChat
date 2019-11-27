import org.bouncycastle.util.encoders.Base64;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatComCatraca {
    public static final String A = "A";
    public static final String B = "B";
    private final String AouB;
    private Pessoa eu;

    private InterfaceGrafica ig;
    private Proxy proxy;

    private ObjectInputStream receber;
    private ObjectOutputStream enviar;

    private String identOutro;
    private X509Certificate certOutro;

    private Catraca catraca;
    private boolean vezNaCatraca;

    Thread comunicador;
    private Queue<String> mensagens;


    /**
     * @param eu          pessoa dona da máquina
     * @param outraPessoa pessoa com quem se está conversando
     * @param AouB        possui o valor A ou B, útil para definir quem começa o DH
     * @throws IOException se der merda
     */
    public ChatComCatraca(Pessoa eu, Socket outraPessoa, String AouB) throws Exception {
        this.AouB = AouB;
        this.mensagens = new ConcurrentLinkedQueue<>() {
            @Override
            public synchronized boolean add(String s) {
                notify();
                return super.add(s);
            }

            @Override
            public synchronized String remove() {
                if (isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e1) {
                        System.out.println("Não deu para usar o wait como esperado");
                    }
                }
                return super.remove();
            }
        };

        this.ig = new InterfaceGrafica(eu.getIdentificador());
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

        ig.abrirChat();
        ig.addListeners(this::ouvidorCaixaDeTexto);

        try {

            boolean parar = false;
            comunicador = new Thread(() -> {
                try {
                    if (this.AouB.equals(B)) {
                        enviarMsgCatraca();
                    }
                    while (!parar) {
                        receberMsgCatraca();
                        enviarMsgCatraca();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            comunicador.start();


        } catch (
                Exception e) {
            e.printStackTrace();
            InterfaceGrafica.showDialog("Erro durante o tratamento das mensagens do chat com as catracas", "Erro", JOptionPane.ERROR_MESSAGE);
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

    private void enviarMsgCatraca() throws Exception {
        permitirEnviarMensagens(true);
        //B -> A: Eg^ab{ g^b', msg, Eg^ab'{ SIGb{g^a, g^b', A}}}
        MensagemCatraca msgCatraca = new MensagemCatraca();

        //Atualizando valores e guardando os necessários
        Key sessionKeyAtual = catraca.getSessionKey();                                                          //g^ab
        catraca.setParDeChaves(CriptoUtils.generateDHNumberPair());                                             //gera b' e g^b'
        catraca.setSessioanKey(defSessionKey(catraca.getPubKeyOutro(), catraca.getMyPrivKeyDH()));

        //Definindo valores e enviando
        msgCatraca.setPublicKey(catraca.getMyPubKeyDH());               //g^ab'

        if (this.AouB.equals(A))
            msgCatraca.setAssCriptografada(comprovarAutenticidade(catraca.getMyPubKeyDH(), catraca.getPubKeyOutro()));//Eg^ab'{ SIGb{g^a, g^b', A}}
        else
            msgCatraca.setAssCriptografada(comprovarAutenticidade(catraca.getPubKeyOutro(), catraca.getMyPubKeyDH()));//Eg^ab'{ SIGb{g^a, g^b', A}}

        msgCatraca.setMsg(mensagens.remove());
        enviar.writeObject(CriptoUtils.CifrarMensagem(serialize(msgCatraca), sessionKeyAtual));
    }

    private void receberMsgCatraca() throws Exception {
        //B -> A: Eg^ab{ g^b', msg, Eg^ab'{ SIGb{g^a, g^b', A}}}
        MensagemCatraca msgRecebida = deserializa(CriptoUtils.DecifrarMensagem((String) receber.readObject(), catraca.getSessionKey()));

        //Atualiza valores de chave publica e de sessão
        catraca.setPubKeyOutro(msgRecebida.getPublicKey());
        catraca.setSessioanKey(defSessionKey(catraca.getPubKeyOutro(), catraca.getMyPrivKeyDH()));

        //Verifica Autenticidade
        if (this.AouB.equals(A)) {
            verificarAutenticidade(msgRecebida.getAssinaturaDasChavesCriptografada(), catraca.getMyPubKeyDH(), catraca.getPubKeyOutro());
        } else {
            verificarAutenticidade(msgRecebida.getAssinaturaDasChavesCriptografada(), catraca.getPubKeyOutro(), catraca.getMyPubKeyDH());
        }
        //Manda a mensagem para o chat
        ig.receberMensagem(msgRecebida.getMsg());
    }

    //Ler Eg^ab{ SIGb{g^a, g^b, A}}
    private void verificarAutenticidade(String textoCriptografado, PublicKey pubKeyA, PublicKey pubKeyB) throws Exception {
        //Verifica se o certificado existe no servidor
        RespostaConsultaCertificado res = (RespostaConsultaCertificado) proxy.fazerRequisicao(eu.getIpServidor(), eu.getPortaServidor(), new RequisicaoConsultaCertificado(identOutro));
        if (res.resposta.equals(Mensagens.NEGADO) || !res.getCertificado().equals(certOutro)) {
            System.out.println("RespostaConsulta: " + res.resposta);
            System.out.println("Certificado " + res.getCertificado().getPublicKey() + " " + res.getCertificado().getIssuerX500Principal().getName() + "");
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
        return CriptoUtils.CifrarMensagem(assinarChavesEIdentificador(pubKeyA, pubKeyB), catraca.getSessionKey());
    }

    private String assinarChavesEIdentificador(PublicKey pubKeyA, PublicKey pubKeyB) throws Exception {
        return CriptoUtils.SignString(eu.getPrivateKey(), concatByteArrays(pubKeyA.getEncoded(), pubKeyB.getEncoded(), identOutro.getBytes()));
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

    private String serialize(MensagemCatraca msgCatraca) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(bo);
        so.writeObject(msgCatraca);
        so.flush();
        return new String(Base64.encode(bo.toByteArray()));
    }

    private MensagemCatraca deserializa(String msgCatracaSerializado) throws IOException, ClassNotFoundException {
        byte b[] = Base64.decode(msgCatracaSerializado.getBytes());
        ByteArrayInputStream bi = new ByteArrayInputStream(b);
        ObjectInputStream si = new ObjectInputStream(bi);
        return (MensagemCatraca) si.readObject();
    }

    public void ouvidorCaixaDeTexto(ActionEvent event) {
        String textoCaixaEnvio = ig.getTextoCaixaEnvio();
        if (textoCaixaEnvio.trim().length() > 1 && vezNaCatraca) {
            if (textoCaixaEnvio.equals(".clear")) {
                ig.limparChat();
            } else {
                String msg = ig.popMensagemParaEnviar();
                permitirEnviarMensagens(false);
                mensagens.add(msg);
                ig.receberMensagem(msg);
            }
        }
    }


    private void permitirEnviarMensagens(boolean bool) {
        vezNaCatraca = bool;
        ig.permitirEnvioMensagens(bool);
    }
}

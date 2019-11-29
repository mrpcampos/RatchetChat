import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Queue;
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

    private Catraca catracaEnvio;
    private Catraca catracaRecebimento;

    Thread msgSorter;
    Thread msgEnviador;
    Thread msgReceptorConfirmador;

    private Queue<String> msgParaEnviar;

    private WaitIfEmpytQueue<MensagemCatraca> msgDeConfirmacaoRecebidas;
    private WaitIfEmpytQueue<MensagemCatraca> msgDeTextoRecebidas;


    /**
     * @param eu          pessoa dona da máquina
     * @param outraPessoa pessoa com quem se está conversando
     * @param AouB        possui o valor A ou B, útil para definir quem começa o DH
     * @throws IOException se der merda
     */
    public ChatComCatraca(Pessoa eu, Socket outraPessoa, String AouB) throws Exception {
        this.AouB = AouB;
        this.msgParaEnviar = new WaitIfEmpytQueue<>();
        this.msgDeConfirmacaoRecebidas = new WaitIfEmpytQueue<>();
        this.msgDeTextoRecebidas = new WaitIfEmpytQueue<>();

        this.ig = new InterfaceGrafica(eu.getIdentificador());
        this.proxy = Proxy.getInstance();
        this.eu = eu;

        CriptoUtils.SetProvider();
        this.catracaEnvio = new Catraca(CriptoUtils.generateDHNumberPair());

        enviar = new ObjectOutputStream(outraPessoa.getOutputStream());
        receber = new ObjectInputStream(outraPessoa.getInputStream());

        //Produz primeira chave de sessão com DH
        try {
            if (this.AouB.equals(A)) {
                caminhoA();
            } else if (this.AouB.equals(B)) {
                caminhoB();
            } else {
                throw new InvalidAlgorithmParameterException("O valor de AouB deve ser A ou B.");
            }

            System.out.println("Diffie-Hellmans concluido");

            //Inicia a segunda catraca para permitir envio assincrono de mensagens
            //TODO ver uma forma melhor de fazer isso, a primeira chave de sessão esta sendo usada duas vezes
            catracaRecebimento = catracaEnvio.clone();

            ig.abrirChat();
            ig.addListeners(this::ouvidorCaixaDeTexto);


            //Cria thread que recebe as mensagens e separa entre confirmação e texto
            msgSorter = new Thread(() -> {
                try {
                    while (true) {
                        MensagemCatraca mensagemCatraca = (MensagemCatraca) receber.readObject();
                        if (mensagemCatraca.getTipo().equals(MensagemCatraca.Tipo.MENSAGEM)) {
                            this.msgDeTextoRecebidas.add(mensagemCatraca);
                        } else if (mensagemCatraca.getTipo().equals(MensagemCatraca.Tipo.CONFIRMAÇÃO)) {
                            this.msgDeConfirmacaoRecebidas.add(mensagemCatraca);
                        } else {
                            System.out.println("Tipo de mensagem não reconhecido, impossível tratar mensagem");
                            throw new InvalidAlgorithmParameterException("Tipo de mensagem não reconhecido, impossível tratar mensagem");
                        }
                    }
                } catch (IOException | ClassNotFoundException | InvalidAlgorithmParameterException e) {
                    System.out.println("Erro separando as mensagens");
                    e.printStackTrace();
                }
            });
            msgSorter.start();

            //Começa a comunicação com uso de duas catracas duplas
            msgReceptorConfirmador = new Thread(() -> {
                try {
                    while (true) {
                        MensagemCatraca mensagemCatraca = msgDeTextoRecebidas.remove();
                        String msg = receberMsgCatraca(catracaRecebimento, mensagemCatraca);
                        ig.receberMensagem(msg);
                        enviarMsgCatraca(catracaRecebimento, MensagemCatraca.Tipo.CONFIRMAÇÃO, "Mensagem recebida com sucesso.");
                    }
                } catch (Exception e) {
                    System.out.println("Erro recebendo e confirmando mensagens");
                    e.printStackTrace();
                }
            });
            msgReceptorConfirmador.start();

            //Começa a comunicação com uso de duas catracas duplas
            msgEnviador = new Thread(() -> {
                try {
                    while (true) {
                        //Envia mensagem
                        enviarMsgCatraca(catracaEnvio, MensagemCatraca.Tipo.MENSAGEM, msgParaEnviar.remove());
                        //Espera confirmação de recebimento
                        MensagemCatraca mensagemCatraca = msgDeConfirmacaoRecebidas.remove();
                        //Atualiza valor das chaves da catraca
                        receberMsgCatraca(catracaEnvio, mensagemCatraca);
                    }
                } catch (Exception e) {
                    System.out.println("Erro enviando e recebendo confirmações de mensagens");
                    e.printStackTrace();
                }
            });
            msgEnviador.start();

            permitirEnviarMensagens(true);

            msgSorter.join();

        } catch (
                Exception e) {
            e.printStackTrace();
            InterfaceGrafica.showDialog("Erro durante o tratamento das msgParaEnviar do msgParaEnviar com as catracas", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void caminhoA() throws Exception {
        //A -> B: g^a, A
        enviar.writeObject(catracaEnvio.getMyPubKeyDH());  //g^a
        enviar.writeObject(eu.getIdentificador());  //A

        //B -> A: g^b, CERTb, Eg^ab{ SIGb{g^a, g^b, A}}
        catracaEnvio.setPubKeyOutro((PublicKey) receber.readObject());                            //g^b
        certOutro = (X509Certificate) receber.readObject();                                  //CERTb
        catracaEnvio.setSessioanKey(defSessionKey(catracaEnvio.getPubKeyOutro(), catracaEnvio.getMyPrivKeyDH()));//g^ab
        identOutro = certOutro.getIssuerX500Principal().getName().split("=")[1];
        verificarAutenticidade((String) receber.readObject(), catracaEnvio.getMyPubKeyDH(), catracaEnvio.getPubKeyOutro(), catracaEnvio.getSessionKey());

        // A -> B: CERTa, Eg^ab {SIGa{g^a, g^b, B}}
        enviar.writeObject(eu.getCertificate()); //CERTa
        enviar.writeObject(comprovarAutenticidade(catracaEnvio.getMyPubKeyDH(), catracaEnvio.getPubKeyOutro(), catracaEnvio.getSessionKey()));
    }

    private void caminhoB() throws Exception {
        //A -> B: g^a, A
        catracaEnvio.setPubKeyOutro((PublicKey) receber.readObject());   //g^a
        this.identOutro = (String) receber.readObject();            //A
        catracaEnvio.setSessioanKey(defSessionKey(catracaEnvio.getPubKeyOutro(), catracaEnvio.getMyPrivKeyDH()));//g^ab

        //B -> A: g^b, CERTb, Eg^ab{ SIGb{g^a, g^b, A}}
        enviar.writeObject(catracaEnvio.getMyPubKeyDH());    //g^b
        enviar.writeObject(eu.getCertificate());        //CERTb
        enviar.writeObject(comprovarAutenticidade(catracaEnvio.getPubKeyOutro(), catracaEnvio.getMyPubKeyDH(), catracaEnvio.getSessionKey()));


        //A -> B: CERTa, Eg^ab {SIGa{g^a, g^b, B}}
        certOutro = (X509Certificate) receber.readObject();             //CERTa
        verificarAutenticidade((String) receber.readObject(),
                catracaEnvio.getPubKeyOutro(),
                catracaEnvio.getMyPubKeyDH(), catracaEnvio.getSessionKey());//Eg^ab{ SIGa{g^a, g^b, B}}
    }

    private void enviarMsgCatraca(Catraca catraca, MensagemCatraca.Tipo tipo, String msg) throws Exception {
        permitirEnviarMensagens(true);
        //B -> A: Eg^ab{ g^b', msg, Eg^ab'{ SIGb{g^a, g^b', A}}}
        MensagemCatraca msgCatraca = new MensagemCatraca(tipo);

        //Atualizando valores e guardando os necessários
        Key sessionKeyAtual = catraca.getSessionKey();                                                          //g^ab
        catraca.setParDeChaves(CriptoUtils.generateDHNumberPair());                                             //gera b' e g^b'
        catraca.setSessioanKey(defSessionKey(catraca.getPubKeyOutro(), catraca.getMyPrivKeyDH()));

        //Definindo valores e enviando
        msgCatraca.setPublicKey(catraca.getMyPubKeyDH());               //g^ab'

        if (this.AouB.equals(A))
            msgCatraca.setAss(comprovarAutenticidade(catraca.getMyPubKeyDH(), catraca.getPubKeyOutro(), catraca.getSessionKey()));//Eg^ab'{ SIGb{g^a, g^b', A}}
        else
            msgCatraca.setAss(comprovarAutenticidade(catraca.getPubKeyOutro(), catraca.getMyPubKeyDH(), catraca.getSessionKey()));//Eg^ab'{ SIGb{g^a, g^b', A}}

        msgCatraca.setMsg(msg);

        msgCatraca.encript(sessionKeyAtual);

        enviar.writeObject(msgCatraca);
    }

    private String receberMsgCatraca(Catraca catraca, MensagemCatraca msgRecebida) throws Exception {
        //B -> A: Eg^ab{ g^b', msg, Eg^ab'{ SIGb{g^a, g^b', A}}}

        msgRecebida.decript(catraca.getSessionKey());

        //Atualiza valores de chave publica e de sessão
        catraca.setPubKeyOutro(msgRecebida.getPublicKey());
        catraca.setSessioanKey(defSessionKey(catraca.getPubKeyOutro(), catraca.getMyPrivKeyDH()));

        //Verifica Autenticidade
        if (this.AouB.equals(A)) {
            verificarAutenticidade(msgRecebida.getAss(), catraca.getMyPubKeyDH(), catraca.getPubKeyOutro(), catraca.getSessionKey());
        } else {
            verificarAutenticidade(msgRecebida.getAss(), catraca.getPubKeyOutro(), catraca.getMyPubKeyDH(), catraca.getSessionKey());
        }

        //Devolve a mensagem para ser utilizada pelo programa
        return msgRecebida.getMsg();
    }

    //Ler Eg^ab{ SIGb{g^a, g^b, A}}
    private void verificarAutenticidade(String textoCriptografado, PublicKey pubKeyA, PublicKey pubKeyB, Key sessionKey) throws
            Exception {
        //Verifica se o certificado existe no servidor
        RespostaConsultaCertificado res = (RespostaConsultaCertificado) proxy.fazerRequisicao(eu.getIpServidor(), eu.getPortaServidor(), new RequisicaoConsultaCertificado(identOutro));
        if (res.resposta.equals(Mensagens.NEGADO) || !res.getCertificado().equals(certOutro)) {
            System.out.println("RespostaConsulta: " + res.resposta);
            System.out.println("Certificado " + res.getCertificado().getPublicKey() + " " + res.getCertificado().getIssuerX500Principal().getName() + "");
            throw new SecurityException("Certificado forjado");
        }

        //Verifica se o certificado realmente assinou os valores
        byte[] concatBytes = concatByteArrays(pubKeyA.getEncoded(), pubKeyB.getEncoded(), eu.getIdentificador().getBytes());
        if (!CriptoUtils.CheckSign(certOutro.getPublicKey(), CriptoUtils.DecifrarMensagem(textoCriptografado, sessionKey), concatBytes)) {
            throw new SecurityException("Certificado RSA inválido, possível ataque!");
        }
    }

    //Gerar Eg^ab {SIGa{g^a, g^b, B}}
    private String comprovarAutenticidade(PublicKey pubKeyA, PublicKey pubKeyB, Key sessionKey) throws Exception {
        return CriptoUtils.CifrarMensagem(assinarChavesEIdentificador(pubKeyA, pubKeyB), sessionKey);
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

    private Key defSessionKey(PublicKey keyPubDHOutro, PrivateKey myPrivKeyDH) throws
            NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
        KeyAgreement keyAgree = KeyAgreement.getInstance("DH", "BCFIPS");

        keyAgree.init(myPrivKeyDH);
        keyAgree.doPhase(keyPubDHOutro, true);
        MessageDigest hash = MessageDigest.getInstance("SHA256", "BCFIPS");
        return new SecretKeySpec(hash.digest(keyAgree.generateSecret()), "AES");
    }

    public void ouvidorCaixaDeTexto(ActionEvent event) {
        String textoCaixaEnvio = ig.getTextoCaixaEnvio();
        if (textoCaixaEnvio.trim().length() > 1) {
            if (textoCaixaEnvio.equals(".clear")) {
                ig.limparChat();
            } else {
                String msg = ig.popMensagemParaEnviar();
                permitirEnviarMensagens(false);
                msgParaEnviar.add(msg);
                ig.receberMensagem(msg);
            }
        }
    }

    private void permitirEnviarMensagens(boolean bool) {
        ig.permitirEnvioMensagens(bool);
    }

    class WaitIfEmpytQueue<T> extends ConcurrentLinkedQueue<T> {
        @Override
        public synchronized boolean add(T s) {
            notifyAll();
            return super.add(s);
        }

        @Override
        public synchronized T remove() {
            if (isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e1) {
                    System.out.println("Não deu para usar o wait como esperado");
                }
            }
            return super.remove();
        }
    }
}

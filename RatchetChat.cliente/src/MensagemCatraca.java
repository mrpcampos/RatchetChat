import java.io.IOException;
import java.io.Serializable;
import java.security.Key;
import java.security.PublicKey;

/**
 * Essa classe contem tudo que vai ser enviado a cada mensagem da catraca, como as classes de requisição e resposta do módulo geral
 * O seguinte diagrama representa uma mensagem enviada de B para A com essa classe
 * B -> A: Eg^ab{ g^b', msg, Eg^ab'{ SIGb{g^a, g^b', A}}}
 */
public class MensagemCatraca implements Serializable {
    // //
    private final Tipo tipo;

    private PublicKey publicKey;
    private String publicKeyCriptografada;

    private String msg;
    private String msgCriptografada;

    private String ass;
    private String assRecriptografada;

    public MensagemCatraca(Tipo tipo) {
        this.tipo = tipo;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getAss() {
        return ass;
    }

    public void setAss(String assinaturaDasChavesCriptografada) {
        this.ass = assinaturaDasChavesCriptografada;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /**
     * Criptografa os campos com informações para envio e apaga seu valores normais,
     * sendo assim pode ser enviado pela rede em segurança
     * @param sessionKeyAtual
     * @throws IOException
     */
    public void encript(Key sessionKeyAtual) throws IOException {
        publicKeyCriptografada = CriptoUtils.CifrarMensagem(CriptoUtils.serialize(publicKey), sessionKeyAtual);
        msgCriptografada = CriptoUtils.CifrarMensagem(msg, sessionKeyAtual);
        assRecriptografada = CriptoUtils.CifrarMensagem(ass, sessionKeyAtual);
        setPublicKey(null);
        setMsg(null);
        setAss(null);
    }

    /**
     * Decripta os parametros utilizando, possibilitando a utilização dos getters para extraí-los.
     *
     * @param sessionKeyAtual chave a ser utilizada na decruptografia
     * @throws Exception caso não seja possível decriptografar algum parametro
     */
    public void decript(Key sessionKeyAtual) throws Exception {
        setMsg(CriptoUtils.DecifrarMensagem(msgCriptografada, sessionKeyAtual));
        setPublicKey((PublicKey) CriptoUtils.deserializa(CriptoUtils.DecifrarMensagem(publicKeyCriptografada, sessionKeyAtual)));
        setAss(CriptoUtils.DecifrarMensagem(assRecriptografada, sessionKeyAtual));
    }

    enum Tipo{
        CONFIRMAÇÃO,
        MENSAGEM
    }
}

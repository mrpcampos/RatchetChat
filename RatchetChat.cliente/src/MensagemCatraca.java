import java.io.Serializable;
import java.security.PublicKey;

/**
 * Essa classe contem tudo que vai ser enviado a cada mensagem da catraca, como as classes de requisição e resposta do módulo geral
 */
public class MensagemCatraca implements Serializable {
    //B -> A: Eg^ab{ g^b', msg, Eg^ab'{ SIGb{g^a, g^b', A}}} //
    private PublicKey publicKey;
    private String msg;
    private String assinaturaDasChavesCriptografada;

    public MensagemCatraca() {
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

    public String getAssinaturaDasChavesCriptografada() {
        return assinaturaDasChavesCriptografada;
    }

    public void setAssCriptografada(String assinaturaDasChavesCriptografada) {
        this.assinaturaDasChavesCriptografada = assinaturaDasChavesCriptografada;
    }
}

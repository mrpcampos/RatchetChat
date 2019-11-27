import java.io.Serializable;

public abstract class Resposta implements Serializable {
    protected final String resposta;

    public Resposta(String resposta) {
        this.resposta = resposta;
    }

    public String getResposta() {
        return resposta;
    }
}

import java.io.Serializable;

public abstract class Requisicao implements Serializable {
    protected final String tipo;

    protected Requisicao(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return tipo;
    }
}

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class RespostaDeChavesECertificado extends Resposta {
    private X509Certificate certificate;
    private PrivateKey privateKey;

    public RespostaDeChavesECertificado(X509Certificate certificate, PrivateKey privateKey) {
        super(Mensagens.ACEITO);
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

    public RespostaDeChavesECertificado() {
        super(Mensagens.NEGADO);
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getResposta() {
        return resposta;
    }
}

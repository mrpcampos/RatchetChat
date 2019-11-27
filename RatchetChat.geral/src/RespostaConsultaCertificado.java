import java.security.cert.X509Certificate;

public class RespostaConsultaCertificado extends Resposta {
    private X509Certificate certificado;

    public RespostaConsultaCertificado() {
        super(Mensagens.NEGADO);
    }

    public RespostaConsultaCertificado(X509Certificate certificado) {
        super(Mensagens.ACEITO);
        this.certificado = certificado;
    }

    public X509Certificate getCertificado() {
        return certificado;
    }
}

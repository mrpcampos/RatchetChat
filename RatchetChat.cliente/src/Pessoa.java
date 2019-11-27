import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * Essa classe guarda todas as informações referentes as preferencias de conexão
 * e chaves e certificados 'permanentes'. (Chaves DH não são consideradas permanentes)
 */
public class Pessoa {
    private String ipServidor = "127.0.0.1";
    private int portaServidor = 12345;
    private int portaConversas = 8088;

    private String identificador;
    private X509Certificate certificate;
    private PrivateKey privateKey;

    public Pessoa(){}

    public void inicializar(String identificador, X509Certificate certificate, PrivateKey privateKey){
        this.identificador = identificador;
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

    public String getIpServidor() {
        return ipServidor;
    }

    public void setIpServidor(String ipServidor) {
        this.ipServidor = ipServidor;
    }

    public int getPortaServidor() {
        return portaServidor;
    }

    public void setPortaServidor(int portaServidor) {
        this.portaServidor = portaServidor;
    }

    public int getPortaConversas() {
        return portaConversas;
    }

    public void setPortaConversas(int portaConversas) {
        this.portaConversas = portaConversas;
    }

    public String getIdentificador() {
        return identificador;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return certificate.getPublicKey();
    }
}

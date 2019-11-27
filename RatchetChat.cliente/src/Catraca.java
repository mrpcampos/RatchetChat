import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Essa classe serve como utilitária para guardar os valores das chaves DH que mudam a cada 'giro' da catraca
 */
public class Catraca {
    private KeyPair parDeChaves;
    private PublicKey pubKeyOutro;
    private Key sessioanKey;

    public Catraca(KeyPair parDeChaves) {
        this.parDeChaves = parDeChaves;
    }

    public PrivateKey getMyPrivKeyDH() {
        return parDeChaves.getPrivate();
    }

    public PublicKey getMyPubKeyDH() {
        return parDeChaves.getPublic();
    }

    public PublicKey getPubKeyOutro() {
        return pubKeyOutro;
    }

    public void setPubKeyOutro(PublicKey pubKeyOutro) {
        this.pubKeyOutro = pubKeyOutro;
    }

    public void setSessioanKey(Key sessioanKey) {
        this.sessioanKey = sessioanKey;
    }

    public Key getSessionKey() {
        return sessioanKey;
    }

    public void setParDeChaves(KeyPair parDeChaves) {
        this.parDeChaves = parDeChaves;
    }
}

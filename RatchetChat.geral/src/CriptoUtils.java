import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;

import javax.crypto.*;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class CriptoUtils {

    public static Scanner INPUT;

    // Key establishment parameters (these parameters may be public)
    private static BigInteger p512 = new BigInteger(
            "9494fec095f3b85ee286542b3836fc81a5dd0a0349b4c239dd387"
                    + "44d488cf8e31db8bcb7d33b41abb9e5a33cca9144b1cef332c94b"
                    + "f0573bf047a3aca98cdf3b", 16);

    private static BigInteger g512 = new BigInteger(
            "153d5d6172adb43045b68ae8e1de1070b6137005686d29d3d73a7"
                    + "749199681ee5b212c9b96bfdcfa5b20cd5e3fd2044895d609cf9b"
                    + "410b7a0f12ca1cb9a428cc", 16);

    public static final long THIRTY_DAYS = 1000L * 60 * 60 * 24 * 30;

    private static KeyPairGenerator keyPairGenerator;
    private static KeyStore ks;
    private static char[] masterPass;

    // Inicia o utilitario carregando ou criando o arquivo de armazenamento de
    // senhas
    // solicitando para digitar uma senha mestra
    public static void SetProvider() {
        Security.addProvider(new BouncyCastleFipsProvider());

    }

    public static void Setup() {
        try {
            SetProvider();

            INPUT = new Scanner(System.in);
            System.out.println("Digite a senha mestra: ");

//            String masterPassString = INPUT.nextLine();
            String masterPassString = "senha";
            masterPassString = Hex.encodeHexString(generateDerivedKey(masterPassString, masterPassString, 10).getEncoded());

            masterPass = masterPassString.toCharArray();

            ks = KeyStore.getInstance("BCFKS", "BCFIPS");

            File arquivo = new File("certstore.bcfks");

            if (!arquivo.exists()) {
                ks.load(null, null);
                ks.store(new FileOutputStream("certstore.bcfks"), masterPass);
            } else {
                ks.load(new FileInputStream("certstore.bcfks"), masterPass);
            }
        } catch (IOException e) {
            e.printStackTrace();

            System.out.println("Senha mestra invalida");
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void StoreCert(String nome, char[] senha, X509Certificate trustedCert) throws Exception {
        ks.setCertificateEntry(nome, trustedCert);
        ks.store(new FileOutputStream("certstore.bcfks"), senha);
    }

    public static X509Certificate GetCert(String nome) {
        X509Certificate trustedCert = null;
        try {
            trustedCert = (X509Certificate) ks.getCertificate(nome);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trustedCert;
    }

    public static boolean HasCert(String nome) throws Exception {
        return ks.containsAlias(nome);
    }

    public static void StoreKey(String nome, char[] senha, PrivateKey key, X509Certificate cert) throws Exception {
        ks.setKeyEntry(nome, key, senha, new X509Certificate[]{cert});
        ks.store(new FileOutputStream("certstore.bcfks"), senha);
    }

    public static PrivateKey GetKey(String nome, char[] senha) {
        PrivateKey sk = null;
        try {
            sk = (PrivateKey) ks.getKey(nome, senha);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sk;
    }

    // Cria uma chave derivada
    private static SecretKey generateDerivedKey(String password, String salt, Integer iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iterations, 128);
        SecretKeyFactory pbkdf2 = null;
        try {
            pbkdf2 = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey sk = pbkdf2.generateSecret(spec);

            return sk;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static char[] pbkdf2KeyGenerator(String password, String salt, Integer iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iterations, 128);
        SecretKeyFactory pbkdf2 = null;
        try {
            pbkdf2 = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey sk = pbkdf2.generateSecret(spec);

            return Arrays.toString(sk.getEncoded()).toCharArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static KeyPair generateDHNumberPair()
            throws Exception {
        DHParameterSpec dhParams = new DHParameterSpec(p512, g512);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH", "BCFIPS");
        keyGen.initialize(dhParams);

        return keyGen.generateKeyPair();
    }

    // Cria um par de chaves RSA
    public static KeyPair generateRSAKeyPair()
            throws GeneralSecurityException {
        keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BCFIPS");
        return keyPairGenerator.generateKeyPair();
    }

    // Cria um certificado
    public static X509Certificate makeCertificate(PrivateKey caSignerKey, PublicKey caPublicKey, String issuer)
            throws GeneralSecurityException, IOException, OperatorCreationException {
        X509v1CertificateBuilder v1CertBldr = new JcaX509v1CertificateBuilder(
                new X500Name("CN=" + issuer),
                BigInteger.valueOf(System.currentTimeMillis()),
                new Date(System.currentTimeMillis() - 1000L * 5),
                new Date(System.currentTimeMillis() + CriptoUtils.THIRTY_DAYS),
                new X500Name("CN=" + issuer),
                caPublicKey);

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BCFIPS");
        return new JcaX509CertificateConverter().setProvider("BCFIPS").getCertificate(v1CertBldr.build(signerBuilder.build(caSignerKey)));
    }

    public static String CifrarMensagem(String mensagem, Key key) {
        try {
            // Inicializar a montoeira de coisas que precisa
            //Key secretKey = new SecretKeySpec(key.getEncoded(), "AES");
            byte iv[];
            IvParameterSpec ivSpec;
            SecureRandom random = new SecureRandom();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
            Mac hMac = Mac.getInstance("HMacSHA256", "BCFIPS");
            Key hMacKey = new SecretKeySpec(key.getEncoded(), "HMacSHA256");

            // Gera o IV aleatorio
            //System.out.print("Gerando IV \t-> ");
            iv = new byte[16];
            random.nextBytes(iv);
            ivSpec = new IvParameterSpec(iv);
            //System.out.println("IV \t= " + Hex.encodeHexString(iv));

            // Cifrar - AQUI NAO ESTA CORRETO: o aluno concatenou o MAC que nao precisava pois esta usando GCM.
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            byte[] cipherText = new byte[cipher.getOutputSize(mensagem.length() + hMac.getMacLength())];

            int ctLength = cipher.update(toByteArray(mensagem), 0, mensagem.length(), cipherText, 0);

            hMac.init(hMacKey);
            hMac.update(toByteArray(mensagem));

            ctLength += cipher.doFinal(hMac.doFinal(), 0, hMac.getMacLength(), cipherText, ctLength);

            return Hex.encodeHexString(iv) + Hex.encodeHexString(cipherText);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public static String DecifrarMensagem(String mensagem, Key key) {
        try {
            // Inicializar a montoeira de coisas que precisa
            //Key secretKey = new SecretKeySpec(key.getEncoded(), "AES");
            byte[] messageBytes = Hex.decodeHex(mensagem.toCharArray());
            int ctLength = messageBytes.length - 16;
            byte[] iv = new byte[16];
            byte[] cipherText = new byte[ctLength];
            IvParameterSpec ivSpec;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
            Mac hMac = Mac.getInstance("HMacSHA256", "BCFIPS");
            Key hMacKey = new SecretKeySpec(key.getEncoded(), "HMacSHA256");

            System.arraycopy(messageBytes, 0, iv, 0, 16);
            System.arraycopy(messageBytes, 16, cipherText, 0, ctLength);

            ivSpec = new IvParameterSpec(iv);
            // System.out.println("IV Decifrando: " + Hex.encodeHexString(iv));

            // Decifrar
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            byte[] plainText = cipher.doFinal(cipherText, 0, ctLength);
            int messageLength = plainText.length - hMac.getMacLength();

            hMac.init(hMacKey);
            hMac.update(plainText, 0, messageLength);

            byte[] messageMac = new byte[hMac.getMacLength()];
            System.arraycopy(plainText, messageLength, messageMac, 0, messageMac.length);

            String decifrado = toString(plainText, messageLength);

            return decifrado;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public static String SignString(PrivateKey key, byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA", "BCFIPS");
        signature.initSign(key);
        signature.update(data);

        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public static boolean CheckSign(PublicKey key, String signedData, byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA", "BCFIPS");
        signature.initVerify(key);
        signature.update(data);

        return signature.verify(Base64.getDecoder().decode(signedData));
    }

    public static String toString(byte[] bytes, int length) {
        char[] chars = new char[length];

        for (int i = 0; i != chars.length; i++) {
            chars[i] = (char) (bytes[i] & 0xff);
        }

        return new String(chars);
    }

    public static byte[] toByteArray(String string) {
        byte[] bytes = new byte[string.length()];
        char[] chars = string.toCharArray();

        for (int i = 0; i != chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }

        return bytes;
    }
}

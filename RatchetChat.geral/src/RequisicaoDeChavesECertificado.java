public class RequisicaoDeChavesECertificado extends Requisicao {
    private String identificador;
    private String senha;

    public RequisicaoDeChavesECertificado(String identificador, String senha) {
        super(Mensagens.PEGAR_CERTIFICADO_E_CHAVES);
        this.identificador = identificador;
        this.senha = senha;
    }
    public String getIdentificador() {
        return identificador;
    }

    public String getSenha() {
        return senha;
    }
}

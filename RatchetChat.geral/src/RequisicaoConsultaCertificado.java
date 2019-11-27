public class RequisicaoConsultaCertificado extends Requisicao {

    private String identificador;

    public RequisicaoConsultaCertificado(String identificador) {
        super(Mensagens.CONSULTAR_CERTIFICADO);
        this.identificador = identificador;
    }

    public String getIdentificador() {
        return identificador;
    }
}

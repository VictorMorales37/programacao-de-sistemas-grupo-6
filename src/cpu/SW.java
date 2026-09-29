package cpu;

/** O código de condição usa os dois bits menos significativos: igual=0, menor=1, maior=2. */
public final class SW extends Registrador {
    public enum CodigoCondicao { IGUAL, MENOR, MAIOR }

    public SW() { super("SW", 9, BITS_24); }
    public CodigoCondicao obterCodigoCondicao() {
        return switch ((int) (obterValor() & 3)) {
            case 0 -> CodigoCondicao.IGUAL;
            case 1 -> CodigoCondicao.MENOR;
            case 2 -> CodigoCondicao.MAIOR;
            default -> throw new IllegalStateException("Código de condição reservado");
        };
    }
    public void definirCodigoCondicao(CodigoCondicao codigoCondicao) {
        if (codigoCondicao == null) throw new IllegalArgumentException("O código de condição não pode ser nulo");
        definirValor((obterValor() & ~3L) | codigoCondicao.ordinal());
    }
    public void comparar(long valorEsquerdo, long valorDireito) {
        definirCodigoCondicao(valorEsquerdo < valorDireito ? CodigoCondicao.MENOR
            : valorEsquerdo > valorDireito ? CodigoCondicao.MAIOR : CodigoCondicao.IGUAL);
    }
}

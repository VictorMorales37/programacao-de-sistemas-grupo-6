package cpu;

/** Mantém o valor do registrador limitado à largura de bits definida. */
public class Registrador {
    public static final int BITS_24 = 24;
    public static final int BITS_48 = 48;
    private final String identificador;
    private final int numero;
    private final int larguraBits;
    private final long mascara;
    private long valor;

    public Registrador(int numero, int larguraBits) { this(null, numero, larguraBits); }
    public Registrador(String identificador, int numero, int larguraBits) {
        if (larguraBits != BITS_24 && larguraBits != BITS_48)
            throw new IllegalArgumentException("A largura do registrador deve ser de 24 ou 48 bits");
        this.identificador = identificador;
        this.numero = numero;
        this.larguraBits = larguraBits;
        mascara = (1L << larguraBits) - 1;
    }
    public String obterIdentificador() { return identificador; }
    public int obterNumero() { return numero; }
    public int obterLarguraBits() { return larguraBits; }
    public long obterValor() { return valor; }
    public void definirValor(long valor) { this.valor = valor & mascara; }
    public void limpar() { definirValor(0); }
}

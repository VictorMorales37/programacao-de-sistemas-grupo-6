package cpu;

public class Registrador {

    public static final int TAMANHO_24_BITS = 24;
    public static final int TAMANHO_48_BITS = 48;

    private final String identificador;
    private final int numero;
    private final int tamanhoBits;
    private final long mascara;
    private long valor;

    public Registrador(int numero, int tamanhoBits) {
        this(null, numero, tamanhoBits);
    }

    public Registrador(String identificador, int numero, int tamanhoBits) {
        if (tamanhoBits != TAMANHO_24_BITS && tamanhoBits != TAMANHO_48_BITS) {
            throw new IllegalArgumentException("Registrador deve possuir 24 ou 48 bits");
        }

        this.identificador = identificador;
        this.numero = numero;
        this.tamanhoBits = tamanhoBits;
        this.mascara = (1L << tamanhoBits) - 1;
        this.valor = 0;
    }

    public String getIdentificador() {
        return identificador;
    }

    public int getNumero() {
        return numero;
    }

    public int getTamanhoBits() {
        return tamanhoBits;
    }

    public long getValor() {
        return valor;
    }

    public void setValor(long valor) {
        this.valor = valor & mascara;
    }

    public void limpar() {
        valor = 0;
    }
}

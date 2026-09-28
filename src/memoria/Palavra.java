package memoria;

/** Palavra de 24 bits com armazenamento sem sinal e interpretação em complemento de dois. */
public final class Palavra {
    public static final int MASCARA = 0xFFFFFF;
    private int valor;

    public Palavra(int valor) { definirValor(valor); }
    public int obterValor() { return valor; }
    public void definirValor(int valor) { this.valor = valor & MASCARA; }
    public static int comSinal(long valor) { return ((int) valor << 8) >> 8; }
}

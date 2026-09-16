package memoria;

public class Memoria {

    public static final int TAMANHO_MINIMO = 1024; // 1KB
    public static final int TAMANHO_PADRAO = 1024*1024; // 1MB igual o Beck

    private final byte[] dados;

    public Memoria(){
        this(TAMANHO_PADRAO);
    }

    public Memoria(int tamanho){
        if (tamanho < TAMANHO_MINIMO){
            throw new IllegalArgumentException("Tamanho de memoria menor que o tamanho minimo");
        }

        this.dados = new byte[tamanho];
    }














}

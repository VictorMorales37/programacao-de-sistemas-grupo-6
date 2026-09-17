package memoria;

public class Memoria {

    public static final int TAMANHO_MINIMO = 1024; // 1KB
    public static final int TAMANHO_PADRAO = 1024*1024; // 1MB igual o Beck
  
    private final byte[] memoria;

    public Memoria(){
        this(TAMANHO_PADRAO);
    }

    public Memoria(int tamanhoMemoria){
        if(tamanhoMemoria < TAMANHO_MINIMO){
            throw new IllegalArgumentException("Tamanho da memoria menor que o tamanho minimo");
        }
        memoria = new byte[tamanhoMemoria];
    }

    public int getTamanho(){
        return memoria.length;
    }

    private void validarEndereco(int endereco){
        if (endereco < 0 || endereco > memoria.length){
            throw new IndexOutOfBoundsException("Endereço inserido inválido: " + endereco);
        }
    }



    public byte lerByte(int endereco){
        validarEndereco(endereco);
        return memoria[endereco];
    }

    public void escreveByte(int endereco, int valor){
        validarEndereco(endereco);
        memoria[endereco] = (byte) valor;
    }

// ##
    public Palavra lerPalavra(int endereco) {
        validarEndereco(endereco);
        validarEndereco(endereco + 2);

        int b0 = memoria[endereco] & 0xFF;
        int b1 = memoria[endereco + 1] & 0xFF;
        int b2 = memoria[endereco + 2] & 0xFF;

        return new Palavra((b0 << 16) | (b1 << 8) | b2);
    }


    public void escreverPalavra(int endereco, Palavra palavra){
        escreverPalavra(endereco, palavra.getValor());
    }

    public void escreverPalavra(int endereco, int valor){
        validarEndereco(endereco);
        validarEndereco(endereco + 2);

        int v = valor & 0xFFFFFF;

        memoria[endereco] = (byte) ((v >> 16) & 0xFF);
        memoria[endereco + 1] = (byte) ((v >> 8) & 0xFF);
        memoria[endereco + 2] = (byte) (v & 0xFF);
    }

    public void limpar() {
        java.util.Arrays.fill(memoria, (byte ) 0);
    }
}
package memoria;

import java.util.Arrays;

/** Armazenamento de bytes big-endian; endereços lógicos apontam para palavras de três bytes. */
public final class Memoria {
    public static final int TAMANHO_MINIMO = 1024;
    public static final int TAMANHO_PADRAO = 3 * 1024 * 1024;
    private final byte[] bytesMemoria;

    public Memoria() { this(TAMANHO_PADRAO); }
    public Memoria(int tamanho) {
        if (tamanho < TAMANHO_MINIMO) throw new IllegalArgumentException("A memória deve conter pelo menos 1024 bytes");
        bytesMemoria = new byte[tamanho];
    }
    public static int converterParaEnderecoEmBytes(int endereco) {
        if (endereco < 0 || endereco > 0xFFFFFF)
            throw new IllegalArgumentException("Endereço de palavra inválido: " + endereco);
        return endereco * 3;
    }
    public int obterTamanho() { return bytesMemoria.length; }
    public void validarIntervalo(int endereco, int comprimento) {
        if (endereco < 0 || comprimento < 0 || endereco > bytesMemoria.length - comprimento)
            throw new IllegalArgumentException("Intervalo fora dos limites da memória: " + endereco + " + " + comprimento);
    }
    public int lerByte(int endereco) {
        validarIntervalo(endereco, 1);
        return bytesMemoria[endereco] & 255;
    }
    public void escreverByte(int endereco, int valor) {
        validarIntervalo(endereco, 1);
        bytesMemoria[endereco] = (byte) valor;
    }
    public Palavra lerPalavra(int endereco) {
        validarIntervalo(endereco, 3);
        return new Palavra((lerByte(endereco) << 16) | (lerByte(endereco + 1) << 8) | lerByte(endereco + 2));
    }
    public void escreverPalavra(int endereco, Palavra palavra) { escreverPalavra(endereco, palavra.obterValor()); }
    public void escreverPalavra(int endereco, int valor) {
        validarIntervalo(endereco, 3);
        bytesMemoria[endereco] = (byte) (valor >>> 16);
        bytesMemoria[endereco + 1] = (byte) (valor >>> 8);
        bytesMemoria[endereco + 2] = (byte) valor;
    }
    public void limpar() { Arrays.fill(bytesMemoria, (byte) 0); }
}

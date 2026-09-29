package testes;

import memoria.Memoria;

final class FerramentasDeTeste {
    private FerramentasDeTeste() {}

    static void escrever(Memoria memoria, int enderecoPalavra, int... bytes) {
        int deslocamento = Memoria.converterParaEnderecoEmBytes(enderecoPalavra);
        for (int i = 0; i < bytes.length; i++) {
            memoria.escreverByte(deslocamento + i, bytes[i]);
        }
    }
}

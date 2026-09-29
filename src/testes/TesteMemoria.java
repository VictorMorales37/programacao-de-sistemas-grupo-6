package testes;

import memoria.Memoria;

import static testes.Asserts.*;

public final class TesteMemoria {

    void testeUltimoEnderecoValidoNaoLancaExcecao() {
        Memoria memoria = new Memoria(Memoria.TAMANHO_MINIMO);
        memoria.lerByte(memoria.obterTamanho() - 1);
    }

    void testeEnderecoIgualAoTamanhoDeveSerRejeitado() {
        Memoria memoria = new Memoria(Memoria.TAMANHO_MINIMO);
        int tamanho = memoria.obterTamanho();
        lanca(IllegalArgumentException.class, () -> memoria.lerByte(tamanho),
                "lerByte(tamanho) deveria ser rejeitado pela validação, não pelo array");
    }

    void testeEnderecoNegativoDeveSerRejeitado() {
        Memoria memoria = new Memoria(Memoria.TAMANHO_MINIMO);
        lanca(IllegalArgumentException.class, () -> memoria.lerByte(-1),
                "endereço negativo deveria ser rejeitado");
    }

    void testeEscritaQueNaoCabeNaoDeveAlterarNadaAntesDeFalhar() {
        Memoria memoria = new Memoria(Memoria.TAMANHO_MINIMO);
        int tamanho = memoria.obterTamanho();
        lanca(IllegalArgumentException.class, () -> memoria.escreverPalavra(tamanho - 2, 0xABCDEF),
                "uma palavra que ultrapassa o limite da memória deveria ser rejeitada");
        igual(0, memoria.lerByte(tamanho - 2), "byte não deveria ter sido alterado pela escrita rejeitada");
        igual(0, memoria.lerByte(tamanho - 1), "byte não deveria ter sido alterado pela escrita rejeitada");
    }

    void testeMemoriaMenorQueOMinimoDeveFalhar() {
        lanca(IllegalArgumentException.class, () -> new Memoria(100),
                "memória menor que 1024 bytes deveria ser rejeitada");
    }

    void testePalavraEscritaEhLidaComOMesmoValor() {
        Memoria memoria = new Memoria(Memoria.TAMANHO_MINIMO);
        memoria.escreverPalavra(0, 0x0102FF);
        igual(0x0102FF, memoria.lerPalavra(0).obterValor(), "a palavra lida deveria ser igual à escrita");
    }

    void testeConversaoDeEnderecoDePalavraParaBytesMultiplicaPorTres() {
        igual(30, Memoria.converterParaEnderecoEmBytes(10), "o endereço de palavra 10 deveria corresponder ao byte 30");
    }

    void testeEnderecoDePalavraNegativoNaConversaoDeveFalhar() {
        lanca(IllegalArgumentException.class, () -> Memoria.converterParaEnderecoEmBytes(-1),
                "endereço de palavra negativo deveria ser rejeitado na conversão");
    }
}

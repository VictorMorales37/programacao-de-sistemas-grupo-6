package testes;

import cpu.Registrador;
import cpu.Registradores;
import cpu.SW;

import static testes.Asserts.*;

public final class TesteRegistradores {

    void testeRegistradorDe24BitsTruncaValoresMaiores() {
        Registrador r = new Registrador("X", 1, 24);
        r.definirValor(0x1FFFFFFL); // 25 bits
        igual(0xFFFFFFL, r.obterValor(), "registrador de 24 bits deveria manter só os 24 bits menos significativos");
    }

    void testeRegistradorDe48BitsTruncaValoresMaiores() {
        Registrador r = new Registrador("F", 6, 48);
        r.definirValor(0x1FFFFFFFFFFFFL); // 49 bits
        igual(0xFFFFFFFFFFFFL, r.obterValor(), "registrador de 48 bits deveria manter só os 48 bits menos significativos");
    }

    void testeLarguraDeBitsInvalidaDeveFalhar() {
        lanca(IllegalArgumentException.class, () -> new Registrador("Z", 7, 16),
                "uma largura diferente de 24 ou 48 bits deveria ser rejeitada");
    }

    void testeBuscaPorNumeroEncontraORegistradorCorreto() {
        Registradores registradores = new Registradores();
        registradores.obterA().definirValor(7);
        igual(7L, registradores.obterPorNumero(0).obterValor(), "obterPorNumero(0) deveria retornar o registrador A");
    }

    void testeBuscaPorIdentificadorEhInsensivelAMaiusculas() {
        Registradores registradores = new Registradores();
        registradores.obterB().definirValor(42);
        igual(42L, registradores.obterPorIdentificador("b").obterValor(),
                "a busca por identificador deveria ignorar maiúsculas/minúsculas");
    }

    void testeBuscaPorIdentificadorInvalidoDeveFalhar() {
        Registradores registradores = new Registradores();
        lanca(IllegalArgumentException.class, () -> registradores.obterPorIdentificador("Z"),
                "um identificador inexistente deveria ser rejeitado");
    }

    void testeLimparZeraTodosOsRegistradores() {
        Registradores registradores = new Registradores();
        registradores.obterA().definirValor(1);
        registradores.obterPC().definirValor(2);
        registradores.limpar();
        for (Registrador r : registradores.todos()) {
            igual(0L, r.obterValor(), "registrador " + r.obterIdentificador() + " deveria estar zerado após limpar()");
        }
    }

    void testeCapturarERestaurarEstadoDevolveOsValoresOriginais() {
        Registradores registradores = new Registradores();
        registradores.obterA().definirValor(10);
        long[] estado = registradores.capturarEstado();
        registradores.obterA().definirValor(99);
        registradores.restaurarEstado(estado);
        igual(10L, registradores.obterA().obterValor(), "restaurarEstado deveria devolver o valor original de A");
    }

    void testeSwComparaValoresEDefineOCodigoDeCondicaoCorreto() {
        SW sw = new SW();
        sw.comparar(3, 5);
        igual(SW.CodigoCondicao.MENOR, sw.obterCodigoCondicao(), "3 comparado a 5 deveria resultar em MENOR");
        sw.comparar(5, 3);
        igual(SW.CodigoCondicao.MAIOR, sw.obterCodigoCondicao(), "5 comparado a 3 deveria resultar em MAIOR");
        sw.comparar(4, 4);
        igual(SW.CodigoCondicao.IGUAL, sw.obterCodigoCondicao(), "4 comparado a 4 deveria resultar em IGUAL");
    }

    void testeSwComCodigoDeCondicaoReservadoDeveFalhar() {
        SW sw = new SW();
        sw.definirValor(3); // combinação de bits que o enum não usa
        lanca(IllegalStateException.class, sw::obterCodigoCondicao,
                "um código de condição reservado (valor 3) deveria ser rejeitado");
    }

    void testeSwComCodigoDeCondicaoNuloDeveFalhar() {
        SW sw = new SW();
        lanca(IllegalArgumentException.class, () -> sw.definirCodigoCondicao(null),
                "um código de condição nulo deveria ser rejeitado");
    }
}

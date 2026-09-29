package testes;

import cpu.instrucoes.Decodificador;
import cpu.instrucoes.Instrucao;
import cpu.instrucoes.Opcode;
import memoria.Memoria;

import static testes.Asserts.*;

public final class TesteDecodificador {

    private static Instrucao decodificar(int... bytes) {
        Memoria memoria = new Memoria();
        int deslocamento = Memoria.converterParaEnderecoEmBytes(0);
        for (int i = 0; i < bytes.length; i++) {
            memoria.escreverByte(deslocamento + i, bytes[i]);
        }
        return new Decodificador().decodificar(memoria, 0);
    }

    void testeFormato2UsaOsNibblesComoNumerosDeRegistrador() {
        // ADDR X,A: 90 10 -> primeiro registrador = X(1), segundo = A(0)
        Instrucao instrucao = decodificar(0x90, 0x10);
        igual(Opcode.ADDR, instrucao.codigoOperacao(), "o opcode deveria ser ADDR");
        igual(2, instrucao.formato(), "ADDR deveria ser decodificada como formato 2");
        igual(1, instrucao.primeiroRegistrador(), "o primeiro registrador deveria ser X (número 1)");
        igual(0, instrucao.segundoRegistrador(), "o segundo registrador deveria ser A (número 0)");
    }

    void testeEnderecamentoImediato() {
        // LDA #5 -> n=0,i=1: 01 00 05
        Instrucao instrucao = decodificar(0x01, 0x00, 0x05);
        verdadeiro(instrucao.ehImediata(), "n=0,i=1 deveria ser reconhecido como imediato");
        falso(instrucao.ehIndireta(), "uma instrução imediata não deveria ser indireta");
        igual(5, instrucao.campo(), "o campo deveria conter o valor imediato 5");
    }

    void testeEnderecamentoIndireto() {
        // LDA @6 -> n=1,i=0: 02 00 06
        Instrucao instrucao = decodificar(0x02, 0x00, 0x06);
        verdadeiro(instrucao.ehIndireta(), "n=1,i=0 deveria ser reconhecido como indireto");
        falso(instrucao.ehImediata(), "uma instrução indireta não deveria ser imediata");
        igual(6, instrucao.campo(), "o campo deveria conter o endereço 6");
    }

    void testeEnderecamentoRelativoABase() {
        // n=1,i=1,b=1,p=0,e=0, campo=5 -> 03 40 05
        Instrucao instrucao = decodificar(0x03, 0x40, 0x05);
        verdadeiro(instrucao.b(), "o bit b deveria estar ligado");
        falso(instrucao.p(), "o bit p não deveria estar ligado");
        igual(3, instrucao.formato(), "sem o bit e, o formato deveria continuar 3");
        igual(5, instrucao.campo(), "o campo deveria ser 5");
    }

    void testeEnderecamentoRelativoAoPcComDeslocamentoNegativo() {
        // JLT PC-3, igual ao usado no example.sic: 3B 2F FD
        Instrucao instrucao = decodificar(0x3B, 0x2F, 0xFD);
        igual(Opcode.JLT, instrucao.codigoOperacao(), "o opcode deveria ser JLT");
        verdadeiro(instrucao.p(), "o bit p deveria estar ligado");
        igual(0x0FFD, instrucao.campo(), "o campo bruto deveria ser 0x0FFD (interpretado como -3 no cálculo do endereço)");
    }

    void testeFormato4TemCampoDe20Bits() {
        // n=1,i=1,e=1, campo = 0x1234 -> 03 10 12 34
        Instrucao instrucao = decodificar(0x03, 0x10, 0x12, 0x34);
        igual(4, instrucao.formato(), "o bit e ligado deveria produzir formato 4");
        igual(0x1234, instrucao.campo(), "o campo de formato 4 deveria ter 20 bits completos");
        igual(2, instrucao.proximoEndereco() - instrucao.endereco(), "uma instrução de 4 bytes ocupa 2 palavras");
    }

    void testeEnderecamentoSicSimplesUsaQuinzeBits() {
        // n=0,i=0: modo SIC simples, endereço de 15 bits
        Instrucao instrucao = decodificar(0x00, 0x00, 0x64);
        verdadeiro(instrucao.ehSic(), "n=0,i=0 deveria ser reconhecido como modo SIC simples");
        igual(0x64, instrucao.campo(), "o campo deveria conter o endereço de 15 bits");
    }

    void testeCombinacaoInvalidaDeBitsBEPDeveFalhar() {
        // b=1 e p=1 ao mesmo tempo não é permitido
        lanca(IllegalArgumentException.class, () -> decodificar(0x03, 0x60, 0x00),
                "b e p ligados ao mesmo tempo deveria ser rejeitado");
    }

    void testeIndexacaoSemEnderecamentoSimplesDeveFalhar() {
        // x=1 com n=1,i=0 (indireto) não é permitido: a indexação exige n=1,i=1
        lanca(IllegalArgumentException.class, () -> decodificar(0x02, 0x80, 0x00),
                "indexação fora do endereçamento simples deveria ser rejeitada");
    }
}

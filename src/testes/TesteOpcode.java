package testes;

import cpu.instrucoes.Opcode;

import static testes.Asserts.*;

public final class TesteOpcode {

    void testeOsBitsNEISaoIgnoradosAoReconhecerOOpcodeDeFormato3() {
        igual(Opcode.LDA, Opcode.decodificar(0x03), "os bits n/i não deveriam mudar qual opcode é reconhecido");
    }

    void testeOpcodeDeFormato2ExigeCorrespondenciaExata() {
        igual(Opcode.ADDR, Opcode.decodificar(0x90), "0x90 deveria ser reconhecido como ADDR");
    }

    void testeCadaOpcodeConhecidoTemOFormatoEsperado() {
        igual(3, Opcode.LDA.formato(), "LDA deveria ser formato 3");
        igual(2, Opcode.ADDR.formato(), "ADDR deveria ser formato 2");
        igual(2, Opcode.CLEAR.formato(), "CLEAR deveria ser formato 2");
    }

    void testeCodigoDeOperacaoDesconhecidoDeveFalhar() {
        lanca(IllegalArgumentException.class, () -> Opcode.decodificar(0xFF),
                "0xFF não corresponde a nenhuma instrução implementada e deveria ser rejeitado");
    }
}

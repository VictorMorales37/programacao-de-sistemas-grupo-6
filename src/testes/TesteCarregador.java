package testes;

import cpu.CPU;
import cpu.instrucoes.Instrucao;
import entrada.CarregadorPrograma;
import entrada.ImagemPrograma;

import static testes.Asserts.*;

public final class TesteCarregador {

    private static final String PROGRAMA_SOMA = """
            ENTRY 000000
            STOP 000007
            CODE 000000 010000
            CODE 000001 050001
            CODE 000002 9010
            CODE 000003 2D0006
            CODE 000004 3B2FFD
            CODE 000005 0F0100
            CODE 000006 3F0007
            WORD 000100 000000
            """;

    void testeProgramaDeExemploSomaDeUmACincoExecutaAteOFim() {
        ImagemPrograma imagem = new CarregadorPrograma().analisar(PROGRAMA_SOMA);
        CPU maquina = imagem.criarCPU();
        int passos = 0;
        while (!imagem.estaConcluido(maquina)) {
            imagem.validarProximaInstrucao(maquina);
            Instrucao instrucao = maquina.executarPasso();
            verdadeiro(++passos <= 50, "o programa deveria terminar em poucos passos (" + instrucao + ")");
        }
        igual(19, passos, "o programa de soma de 1 a 5 deveria terminar em 19 passos");
        igual(15L, maquina.obterRegistradores().obterA().obterValor(), "1+2+3+4+5 deveria resultar em A=15");
        igual(6L, maquina.obterRegistradores().obterX().obterValor(), "o contador deveria terminar em X=6");
        igual(15, maquina.lerPalavra(0x100), "o resultado também deveria ter sido gravado na memória");
    }

    void testeFaltaDeEntryDeveFalhar() {
        String fonte = "STOP 000001\nCODE 000000 010000\n";
        lanca(IllegalArgumentException.class, () -> new CarregadorPrograma().analisar(fonte),
                "um programa sem ENTRY deveria ser rejeitado");
    }

    void testeFaltaDeStopDeveFalhar() {
        String fonte = "ENTRY 000000\nCODE 000000 010000\n";
        lanca(IllegalArgumentException.class, () -> new CarregadorPrograma().analisar(fonte),
                "um programa sem STOP deveria ser rejeitado");
    }

    void testeEntryDuplicadoDeveFalhar() {
        String fonte = "ENTRY 000000\nENTRY 000001\nSTOP 000001\nCODE 000000 010000\n";
        lanca(IllegalArgumentException.class, () -> new CarregadorPrograma().analisar(fonte),
                "dois comandos ENTRY no mesmo programa deveriam ser rejeitados");
    }

    void testeSobreposicaoDeCodigoDeveFalhar() {
        // duas instruções escritas no MESMO endereço de palavra
        String fonte = "ENTRY 000000\nSTOP 000002\nCODE 000000 010000\nCODE 000000 1C0000\n";
        lanca(IllegalArgumentException.class, () -> new CarregadorPrograma().analisar(fonte),
                "duas instruções sobrepostas no mesmo endereço deveriam ser rejeitadas");
    }

    void testeStopSobrepondoCodigoDeveFalhar() {
        String fonte = "ENTRY 000000\nSTOP 000000\nCODE 000000 010000\n";
        lanca(IllegalArgumentException.class, () -> new CarregadorPrograma().analisar(fonte),
                "o endereço de STOP não deveria coincidir com uma instrução existente");
    }

    void testeCodigoComQuantidadeInvalidaDeBytesDeveFalhar() {
        String fonte = "ENTRY 000000\nSTOP 000001\nCODE 000000 01\n"; // 1 byte não é permitido (mínimo 2)
        lanca(IllegalArgumentException.class, () -> new CarregadorPrograma().analisar(fonte),
                "uma instrução CODE com apenas 1 byte deveria ser rejeitada");
    }

    void testeTamanhoDeInstrucaoInconsistenteComOsFlagsDecodificadosDeveFalhar() {
        // 2 bytes fornecidos, mas os bits decodificados (LDA imediato) indicam formato 3
        String fonte = "ENTRY 000000\nSTOP 000001\nCODE 000000 0100\n";
        lanca(IllegalArgumentException.class, () -> new CarregadorPrograma().analisar(fonte),
                "a quantidade de bytes fornecida deveria bater com o formato decodificado da instrução");
    }

    void testeWordComHexadecimalInvalidoDeveFalhar() {
        String fonte = "ENTRY 000000\nSTOP 000002\nCODE 000000 010000\nWORD 000001 GG\n";
        lanca(IllegalArgumentException.class, () -> new CarregadorPrograma().analisar(fonte),
                "um valor hexadecimal inválido em WORD deveria ser rejeitado");
    }
}

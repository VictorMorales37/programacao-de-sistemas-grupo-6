package testes;

import cpu.CPU;
import cpu.SW;
import memoria.Memoria;

import static testes.Asserts.*;
import static testes.FerramentasDeTeste.escrever;

public final class TesteCpuExecucao {

    void testeConsultarInstrucaoNaoAvancaOPc() {
        Memoria memoria = new Memoria();
        escrever(memoria, 0, 0x01, 0x00, 0x09); // LDA #9
        CPU cpu = new CPU(memoria);
        cpu.consultarInstrucao();
        cpu.consultarInstrucao();
        igual(0L, cpu.obterRegistradores().obterPC().obterValor(), "consultarInstrucao() só deveria olhar, nunca avançar o PC");
    }

    void testeOperacoesAritmeticasComEnderecamentoImediato() {
        Memoria memoria = new Memoria();
        escrever(memoria, 0, 0x01, 0x00, 0x14); // LDA #20
        escrever(memoria, 1, 0x1D, 0x00, 0x08); // SUB #8  -> 12
        escrever(memoria, 2, 0x21, 0x00, 0x03); // MUL #3  -> 36
        escrever(memoria, 3, 0x25, 0x00, 0x06); // DIV #6  -> 6
        CPU cpu = new CPU(memoria);
        for (int i = 0; i < 4; i++) cpu.executarPasso();
        igual(6L, cpu.obterRegistradores().obterA().obterValor(), "20-8=12, 12*3=36, 36/6=6");
        igual(4L, cpu.obterCiclos(), "quatro instruções bem-sucedidas deveriam contar quatro ciclos");
    }

    void testeOperacoesLogicas() {
        Memoria memoria = new Memoria();
        escrever(memoria, 0, 0x01, 0x00, 0xFF); // LDA #0xFF
        escrever(memoria, 1, 0x41, 0x00, 0x0F); // AND #0x0F -> 0x0F
        escrever(memoria, 2, 0x45, 0x00, 0xF0); // OR  #0xF0 -> 0xFF
        CPU cpu = new CPU(memoria);
        for (int i = 0; i < 3; i++) cpu.executarPasso();
        igual(0xFFL, cpu.obterRegistradores().obterA().obterValor(), "0xFF & 0x0F = 0x0F; 0x0F | 0xF0 = 0xFF");
    }

    void testeDivisaoPorZeroDesfazOPassoPorCompleto() {
        Memoria memoria = new Memoria();
        escrever(memoria, 0, 0x01, 0x00, 0x0A); // LDA #10
        escrever(memoria, 1, 0x25, 0x00, 0x00); // DIV #0
        CPU cpu = new CPU(memoria);
        cpu.executarPasso();
        lanca(ArithmeticException.class, cpu::executarPasso, "divisão por zero deveria lançar ArithmeticException");
        igual(1L, cpu.obterRegistradores().obterPC().obterValor(), "após a falha, o PC deveria voltar para a instrução que falhou");
        igual(10L, cpu.obterRegistradores().obterA().obterValor(), "após a falha, A não deveria ter sido alterado");
        igual(1L, cpu.obterCiclos(), "um passo que falha não deveria contar como ciclo executado");
    }

    void testeArmazenamentoComEnderecamentoImediatoDeveFalhar() {
        Memoria memoria = new Memoria();
        escrever(memoria, 0, 0x0D, 0x00, 0x00); // STA com i=1 (inválido para instruções de armazenamento)
        CPU cpu = new CPU(memoria);
        lanca(IllegalArgumentException.class, cpu::executarPasso,
                "STA com endereçamento imediato deveria ser rejeitado");
    }

    void testeLdchEStchTransferemApenasUmByte() {
        Memoria memoria = new Memoria();
        memoria.escreverByte(Memoria.converterParaEnderecoEmBytes(5), 0x7A);
        escrever(memoria, 0, 0x53, 0x00, 0x05); // LDCH 5
        escrever(memoria, 1, 0x57, 0x00, 0x06); // STCH 6
        CPU cpu = new CPU(memoria);
        cpu.executarPasso();
        igual(0x7AL, cpu.obterRegistradores().obterA().obterValor(),
                "LDCH deveria carregar o byte 0x7A no byte menos significativo de A");
        cpu.executarPasso();
        igual(0x7A, memoria.lerByte(Memoria.converterParaEnderecoEmBytes(6)),
                "STCH deveria gravar o byte menos significativo de A na memória");
    }

    void testeSaltoCondicionalOcorreQuandoACondicaoEhVerdadeira() {
        Memoria memoria = new Memoria();
        escrever(memoria, 0, 0x01, 0x00, 0x07); // LDA #7
        escrever(memoria, 1, 0x29, 0x00, 0x07); // COMP #7 -> IGUAL
        escrever(memoria, 2, 0x33, 0x00, 0x04); // JEQ 4
        escrever(memoria, 3, 0x0F, 0x00, 0xC8); // STA 200 (não deveria executar)
        escrever(memoria, 4, 0x0F, 0x00, 0xC9); // STA 201 (deveria executar)
        CPU cpu = new CPU(memoria);
        for (int i = 0; i < 4; i++) cpu.executarPasso();
        igual(0, cpu.lerPalavra(200), "a instrução logo após o salto não deveria ter sido executada");
        igual(7, cpu.lerPalavra(201), "a instrução no destino do salto deveria ter sido executada");
    }

    void testeSaltoCondicionalNaoOcorreQuandoACondicaoEhFalsa() {
        Memoria memoria = new Memoria();
        escrever(memoria, 0, 0x01, 0x00, 0x05); // LDA #5
        escrever(memoria, 1, 0x29, 0x00, 0x07); // COMP #7 -> MENOR (5<7)
        escrever(memoria, 2, 0x33, 0x00, 0x04); // JEQ 4 (não deveria saltar)
        escrever(memoria, 3, 0x0F, 0x00, 0xCA); // STA 202 (deveria executar, em sequência)
        CPU cpu = new CPU(memoria);
        for (int i = 0; i < 4; i++) cpu.executarPasso();
        igual(5, cpu.lerPalavra(202), "sem a condição satisfeita, a execução deveria continuar na próxima instrução");
        igual(4L, cpu.obterRegistradores().obterPC().obterValor(), "o PC deveria estar após a instrução sequencial, sem ter saltado");
    }

    void testeChamadaERetornoDeSubrotina() {
        Memoria memoria = new Memoria();
        escrever(memoria, 0, 0x4B, 0x00, 0x03); // JSUB 3
        escrever(memoria, 1, 0x0F, 0x00, 0xD2); // STA 210 (executado após o retorno)
        escrever(memoria, 3, 0x01, 0x00, 0x37); // LDA #55 (subrotina)
        escrever(memoria, 4, 0x4C, 0x00, 0x00); // RSUB
        CPU cpu = new CPU(memoria);
        for (int i = 0; i < 4; i++) cpu.executarPasso();
        igual(1L, cpu.obterRegistradores().obterL().obterValor(), "JSUB deveria guardar em L o endereço de retorno");
        igual(55, cpu.lerPalavra(210), "após o RSUB, a execução deveria continuar de onde o JSUB foi chamado");
        igual(2L, cpu.obterRegistradores().obterPC().obterValor(), "após executar a instrução de retorno, o PC deveria estar em 2");
    }

    void testeInstrucoesDeFormato2ComRegistradores() {
        Memoria memoria = new Memoria();
        escrever(memoria, 0, 0xB4, 0x00);       // CLEAR A
        escrever(memoria, 1, 0x05, 0x00, 0x09); // LDX #9
        escrever(memoria, 2, 0xAC, 0x10);       // RMO X,A     -> A=9
        escrever(memoria, 3, 0xA4, 0x01);       // SHIFTL A,2  -> A=36
        escrever(memoria, 4, 0xB4, 0x10);       // CLEAR X
        escrever(memoria, 5, 0xB8, 0x00);       // TIXR A      -> X=1, compara com A(36)
        CPU cpu = new CPU(memoria);
        for (int i = 0; i < 6; i++) cpu.executarPasso();
        igual(36L, cpu.obterRegistradores().obterA().obterValor(), "CLEAR, RMO e SHIFTL deveriam resultar em A=36");
        igual(1L, cpu.obterRegistradores().obterX().obterValor(), "TIXR deveria incrementar X para 1");
        igual(SW.CodigoCondicao.MENOR, cpu.obterRegistradores().obterSW().obterCodigoCondicao(), "1 comparado a 36 deveria ser MENOR");
    }
}

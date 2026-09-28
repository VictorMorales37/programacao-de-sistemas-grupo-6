package cpu;

import cpu.instrucoes.*;
import memoria.Memoria;

/** Executa um ciclo de busca, decodificação e execução usando endereços de palavra. */
public final class CPU {
    private final Memoria memoria;
    private final Registradores registradores = new Registradores();
    private final Decodificador decodificador = new Decodificador();
    private final ExecutorInstrucoes executor = new ExecutorInstrucoes();
    private long ciclos;

    public CPU() { this(new Memoria()); }
    public CPU(Memoria memoria) { this.memoria = memoria; }
    public Memoria obterMemoria() { return memoria; }
    public Registradores obterRegistradores() { return registradores; }
    public long obterCiclos() { return ciclos; }
    public int enderecoEmBytes(int endereco) { return Memoria.converterParaEnderecoEmBytes(endereco); }
    public int lerPalavra(int endereco) { return memoria.lerPalavra(enderecoEmBytes(endereco)).obterValor(); }
    public void escreverPalavra(int endereco, long valor) { memoria.escreverPalavra(enderecoEmBytes(endereco), (int) valor); }
    public void validarEndereco(int endereco) { memoria.validarIntervalo(enderecoEmBytes(endereco), 3); }
    public Instrucao consultarInstrucao() { return decodificador.decodificar(memoria, (int) registradores.obterPC().obterValor()); }

    public Instrucao executarPasso() {
        Instrucao instrucao = consultarInstrucao();
        long[] estadoAnterior = registradores.capturarEstado();
        try {
            registradores.obterPC().definirValor(instrucao.proximoEndereco());
            executor.executar(this, instrucao);
            ciclos++;
            return instrucao;
        } catch (RuntimeException erro) {
            registradores.restaurarEstado(estadoAnterior);
            throw erro;
        }
    }
}

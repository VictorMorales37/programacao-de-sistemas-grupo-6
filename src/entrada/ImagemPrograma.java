package entrada;

import cpu.CPU;
import java.util.*;

/** Mantém o conteúdo inicial para que uma reinicialização possa criar uma nova máquina. */
public final class ImagemPrograma {
    public record Entrada(int endereco, byte[] dados, boolean executavel) {
        public Entrada { dados = dados.clone(); }
        @Override public byte[] dados() { return dados.clone(); }
    }

    private final int enderecoEntrada;
    private final int enderecoParada;
    private final List<Entrada> entradas;
    private final Set<Integer> enderecosInstrucoes = new HashSet<>();

    public ImagemPrograma(int enderecoEntrada, int enderecoParada, List<Entrada> entradas) {
        this.enderecoEntrada = enderecoEntrada;
        this.enderecoParada = enderecoParada;
        this.entradas = List.copyOf(entradas);
        for (Entrada entrada : entradas)
            if (entrada.executavel()) enderecosInstrucoes.add(entrada.endereco());
    }

    public CPU criarCPU() {
        CPU maquina = new CPU();
        for (Entrada entrada : entradas) {
            int deslocamento = maquina.enderecoEmBytes(entrada.endereco());
            byte[] dadosEntrada = entrada.dados();
            for (int indice = 0; indice < dadosEntrada.length; indice++)
                maquina.obterMemoria().escreverByte(deslocamento + indice, dadosEntrada[indice]);
        }
        maquina.obterRegistradores().obterPC().definirValor(enderecoEntrada);
        return maquina;
    }

    public boolean estaConcluido(CPU maquina) {
        return maquina.obterRegistradores().obterPC().obterValor() == enderecoParada;
    }

    public void validarProximaInstrucao(CPU maquina) {
        int endereco = (int) maquina.obterRegistradores().obterPC().obterValor();
        if (!enderecosInstrucoes.contains(endereco))
            throw new IllegalStateException(String.format("PC %06X não aponta para uma instrução", endereco));
    }
}

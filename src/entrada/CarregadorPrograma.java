package entrada;

import cpu.CPU;
import memoria.Memoria;
import java.util.*;

/** Carrega código de máquina e palavras de dados; não traduz linguagem assembly. */
public final class CarregadorPrograma {
    public ImagemPrograma analisar(String fonte) {
        int enderecoEntrada = -1;
        int enderecoParada = -1;
        List<ImagemPrograma.Entrada> entradas = new ArrayList<>();
        BitSet ocupado = new BitSet(Memoria.TAMANHO_PADRAO);
        Memoria memoria = new Memoria();
        String[] linhas = fonte.replace("\uFEFF", "").split("\\R");

        for (int numeroLinha = 0; numeroLinha < linhas.length; numeroLinha++) {
            String linha = linhas[numeroLinha].split(";", 2)[0].trim();
            if (linha.isEmpty()) continue;
            String[] campos = linha.split("\\s+");
            String comando = campos[0].toUpperCase(Locale.ROOT);

            try {
                if (comando.equals("ENTRY") || comando.equals("STOP")) {
                    if (campos.length != 2) throw new IllegalArgumentException("Esperados um comando e um endereço");
                    if (comando.equals("ENTRY")) {
                        if (enderecoEntrada != -1) throw new IllegalArgumentException("Comando ENTRY duplicado");
                        enderecoEntrada = endereco(campos[1]);
                    } else {
                        if (enderecoParada != -1) throw new IllegalArgumentException("Comando STOP duplicado");
                        enderecoParada = endereco(campos[1]);
                    }
                    continue;
                }

                if (campos.length != 3 || !(comando.equals("CODE") || comando.equals("WORD")))
                    throw new IllegalArgumentException("Esperados CODE ou WORD, um endereço e um valor hexadecimal");

                int endereco = endereco(campos[1]);
                int deslocamento = Memoria.converterParaEnderecoEmBytes(endereco);
                boolean executavel = comando.equals("CODE");
                byte[] bytes;

                if (executavel) {
                    if (!campos[2].matches("(?:[0-9A-Fa-f]{2}){2,4}"))
                        throw new IllegalArgumentException("CODE exige uma instrução de 2, 3 ou 4 bytes");
                    bytes = HexFormat.of().parseHex(campos[2]);
                } else {
                    if (!campos[2].matches("[0-9A-Fa-f]{1,6}"))
                        throw new IllegalArgumentException("WORD exige de 1 a 6 dígitos hexadecimais");
                    int valor = Integer.parseInt(campos[2], 16);
                    bytes = new byte[]{(byte) (valor >>> 16), (byte) (valor >>> 8), (byte) valor};
                }

                int comprimento = ((bytes.length + 2) / 3) * 3;
                memoria.validarIntervalo(deslocamento, comprimento);
                int colisao = ocupado.nextSetBit(deslocamento);
                if (colisao >= 0 && colisao < deslocamento + comprimento)
                    throw new IllegalArgumentException("Sobreposição de código ou dados");
                ocupado.set(deslocamento, deslocamento + comprimento);
                entradas.add(new ImagemPrograma.Entrada(endereco, bytes, executavel));
            } catch (RuntimeException erro) {
                throw new IllegalArgumentException("Linha " + (numeroLinha + 1) + ": " + erro.getMessage(), erro);
            }
        }

        if (enderecoEntrada < 0 || enderecoParada < 0)
            throw new IllegalArgumentException("Os comandos ENTRY e STOP são obrigatórios");

        ImagemPrograma imagem = new ImagemPrograma(enderecoEntrada, enderecoParada, entradas);
        CPU maquina = imagem.criarCPU();
        maquina.validarEndereco(enderecoEntrada);
        maquina.validarEndereco(enderecoParada);
        imagem.validarProximaInstrucao(maquina);
        if (ocupado.get(maquina.enderecoEmBytes(enderecoParada)))
            throw new IllegalArgumentException("O endereço de STOP se sobrepõe a código ou dados");

        for (ImagemPrograma.Entrada entrada : entradas) {
            if (!entrada.executavel()) continue;
            maquina.obterRegistradores().obterPC().definirValor(entrada.endereco());
            if (maquina.consultarInstrucao().formato() != entrada.dados().length)
                throw new IllegalArgumentException(
                    String.format("Tamanho incorreto da instrução no endereço %06X", entrada.endereco()));
        }
        return imagem;
    }

    public static int endereco(String valor) {
        if (!valor.matches("[0-9A-Fa-f]{1,6}"))
            throw new IllegalArgumentException("Esperado um endereço hexadecimal");
        return Integer.parseInt(valor, 16);
    }
}

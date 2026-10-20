package montador;

import java.util.List;

/**
 * Saída pública do montador. {@code enderecoEntrada} e {@code enderecoParada} (primeira
 * palavra livre após o fim do programa, destino implícito do símbolo STOP) são endereços
 * lógicos em palavras, assim como todos os demais endereços aqui presentes.
 */
public record ProgramaMontado(List<Entrada> entradas, int enderecoEntrada, int enderecoParada,
        List<Simbolo> tabelaSimbolos, List<LinhaListagem> listagem, List<ErroMontagem> erros,
        List<String> avisos) {

    /** Erro de montagem: número da linha fonte e mensagem descritiva. */
    public record ErroMontagem(int linha, String mensagem) {}

    /** Dados gravados na memória: até 4 bytes por instrução (formato 4) ou 3 por palavra. */
    public record Entrada(int endereco, byte[] dados, boolean executavel) {
        public Entrada { dados = dados.clone(); }
        @Override public byte[] dados() { return dados.clone(); }
    }

    public record Simbolo(String nome, int endereco) {}

    /** Uma linha fonte e o código que ela gerou (código em hex, "" se nada gerado). */
    public record LinhaListagem(int numero, int endereco, String codigo, String fonte) {}

    /** true se não houve erros de montagem. */
    public boolean validada() { return erros.isEmpty(); }
}

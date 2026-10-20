package montador;

import java.util.TreeMap;

/** Mapa ordenado de nome → endereço lógico (em palavras). Nomes não diferenciam maiúsculas. */
public final class TabelaSimbolos {

    private final TreeMap<String, Integer> simbolos = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /** Insere um símbolo; devolve false se nome já existe (erro de duplicação). */
    public boolean inserir(String nome, int endereco) {
        return simbolos.putIfAbsent(nome.toUpperCase(), endereco) == null;
    }

    /** Endereço do símbolo, ou null se indefinido. */
    public Integer consultar(String nome) {
        return simbolos.get(nome.toUpperCase());
    }

    public boolean contem(String nome) {
        return simbolos.containsKey(nome.toUpperCase());
    }

    /** Listagem ordenada por nome. */
    public java.util.List<ProgramaMontado.Simbolo> listar() {
        return simbolos.entrySet().stream()
            .map(entrada -> new ProgramaMontado.Simbolo(entrada.getKey(), entrada.getValue()))
            .toList();
    }
}

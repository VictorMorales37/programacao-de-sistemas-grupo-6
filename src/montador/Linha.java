package montador;

import java.util.ArrayList;
import java.util.List;

/**
 * Campos de uma linha de assembly SIC/XE.
 * <p>
 * O texto da linha tem o comentário (a partir de ';') desconsiderado e os campos são
 * separados por espaços: {@code [rótulo] operador [opd1 [opd2]]}.
 * A regra de desambiguação: o primeiro campo é o operador se ele (ou ele sem o prefixo '+')
 * for mnemônico conhecido (instrução ou diretiva); caso contrário, é um rótulo.
 * Aqui nada além da estrutura é validado.
 */
public record Linha(int numeroLinha, String rotulo, String operador, boolean formato4, List<String> operandos) {

    /** Constrói a linha a partir do texto bruto (comentários e espaços são desconsiderados). */
    public static Linha ler(int numeroLinha, String texto) {
        String limpa = texto;
        int indiceComentario = limpa.indexOf(';');
        if (indiceComentario >= 0) limpa = limpa.substring(0, indiceComentario);
        String[] campos = limpa.trim().split("\\s+");
        if (campos.length == 1 && campos[0].isEmpty())
            return new Linha(numeroLinha, null, null, false, List.of());

        int indiceOperador = 0;
        if (!ehMnemonico(campos[0])) {
            indiceOperador = 1;
            if (campos.length == 1) // apenas um termo: tratado como operador para o passo 1 reportar erro
                return new Linha(numeroLinha, null, campos[0].toUpperCase(), false, List.of());
        }
        String campoOperador = campos[indiceOperador].toUpperCase();
        boolean formato4 = campoOperador.startsWith("+");
        if (formato4) campoOperador = campoOperador.substring(1);
        List<String> operandos = List.of();
        if (indiceOperador + 1 < campos.length)
            operandos = expandirOperando(campos, indiceOperador + 1);
        return new Linha(numeroLinha, indiceOperador > 0 ? campos[0] : null, campoOperador, formato4, operandos);
    }

    /** Operandos: espaços e vírgulas separa (ex.: 'ADDR X,A' → operandos X e A). */
    private static List<String> expandirOperando(String[] campos, int primeiroIndice) {
        List<String> operandos = new ArrayList<>();
        for (int indice = primeiroIndice; indice < campos.length; indice++)
            for (String parte : campos[indice].split(","))
                if (!parte.isEmpty()) operandos.add(parte);
        return List.copyOf(operandos);
    }

    /** O campo é mnemônico conhecido (instrução ou diretiva), considerando o prefixo '+'. */
    private static boolean ehMnemonico(String campo) {
        String nome = campo.startsWith("+") ? campo.substring(1) : campo;
        return TabelaInstrucoes.de(nome) != null || TabelaDiretivas.de(nome) != null;
    }
}

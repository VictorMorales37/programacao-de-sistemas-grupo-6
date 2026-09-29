package testes;

import java.util.Objects;

final class Asserts {
    private Asserts() {}

    static void igual(long esperado, long obtido, String mensagem) {
        if (esperado != obtido)
            throw new AssertionError(mensagem + " (esperado=" + esperado + ", obtido=" + obtido + ")");
    }

    static void igual(Object esperado, Object obtido, String mensagem) {
        if (!Objects.equals(esperado, obtido))
            throw new AssertionError(mensagem + " (esperado=" + esperado + ", obtido=" + obtido + ")");
    }

    static void verdadeiro(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }

    static void falso(boolean condicao, String mensagem) {
        verdadeiro(!condicao, mensagem);
    }
    static void lanca(Class<? extends Throwable> tipoEsperado, Runnable acao, String mensagem) {
        try {
            acao.run();
        } catch (Throwable lancada) {
            if (tipoEsperado.isInstance(lancada)) return;
            throw new AssertionError(mensagem + " (lançou " + lancada.getClass().getSimpleName()
                    + ": " + lancada.getMessage() + "; esperado " + tipoEsperado.getSimpleName() + ")", lancada);
        }
        throw new AssertionError(mensagem + " (não lançou nenhuma exceção; esperado " + tipoEsperado.getSimpleName() + ")");
    }
}

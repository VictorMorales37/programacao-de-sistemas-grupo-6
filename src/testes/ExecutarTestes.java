package testes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ExecutarTestes {

    private static final Class<?>[] SUITES = {
            TesteMemoria.class,
            TesteRegistradores.class,
            TesteOpcode.class,
            TesteDecodificador.class,
            TesteCpuExecucao.class,
            TesteCarregador.class,
    };

    public static void main(String[] args) {
        int total = 0;
        int falhas = 0;

        for (Class<?> suite : SUITES) {
            List<Method> metodos = new ArrayList<>();
            for (Method metodo : suite.getDeclaredMethods()) {
                if (metodo.getName().startsWith("teste") && metodo.getParameterCount() == 0) {
                    metodos.add(metodo);
                }
            }
            metodos.sort(Comparator.comparing(Method::getName));

            System.out.println("== " + suite.getSimpleName() + " ==");
            for (Method metodo : metodos) {
                total++;
                try {
                    Object instancia = suite.getDeclaredConstructor().newInstance();
                    metodo.setAccessible(true);
                    metodo.invoke(instancia);
                    System.out.println("  ok     " + metodo.getName());
                } catch (Exception falhaDeInvocacao) {
                    falhas++;
                    Throwable causa = falhaDeInvocacao.getCause() != null ? falhaDeInvocacao.getCause() : falhaDeInvocacao;
                    System.out.println("  FALHA  " + metodo.getName());
                    System.out.println("           -> " + causa);
                }
            }
            System.out.println();
        }

        System.out.println(total + " teste(s): " + (total - falhas) + " ok, " + falhas + " falha(s)");
        if (falhas > 0) {
            System.exit(1);
        }
    }
}

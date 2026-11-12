import interfacegrafica.PainelSimulador;
import montador.ProcessadorMacros;
import javax.swing.*;
import java.nio.file.Path;
import java.util.Locale;

public final class Main {
    public static void main(String[] argumentos) {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        JComponent.setDefaultLocale(Locale.getDefault());
        if (argumentos.length > 0) {
            if (argumentos.length != 1) {
                System.err.println("Uso: java -jar sic-xe-simulator.jar [arquivo-fonte.asm]");
                System.exit(2);
            }
            try {
                Path saida = ProcessadorMacros.processarArquivo(Path.of(argumentos[0]));
                System.out.println("Fonte expandido gerado em " + saida);
            } catch (Exception erro) {
                System.err.println("Erro ao processar macros: " + erro.getMessage());
                System.exit(1);
            }
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame("Simulador SIC/XE");
            janela.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            janela.setContentPane(new PainelSimulador());
            janela.setSize(1100, 700);
            janela.setLocationRelativeTo(null);
            janela.setVisible(true);
        });
    }
}

import interfacegrafica.PainelSimulador;
import javax.swing.*;
import java.util.Locale;

public final class Main {
    public static void main(String[] argumentos) {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        JComponent.setDefaultLocale(Locale.getDefault());
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

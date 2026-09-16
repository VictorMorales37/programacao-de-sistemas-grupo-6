package memoria;

public class Memoria {
    Palavra[] memoria;

    public Memoria(int tamanhoMemoria) {
        memoria = new Palavra[tamanhoMemoria];
        for (Palavra palavra : memoria) {
            palavra = new Palavra(0);
        }
    }

    public void escrever(int endereco, int valor) {
        memoria[endereco].setValor(valor);
    }
}

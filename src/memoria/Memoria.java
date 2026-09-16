package memoria;

public class Memoria {

    public static final int TAMANHO_MINIMO = 1024; // 1KB
    public static final int TAMANHO_PADRAO = 1024*1024; // 1MB igual o Beck
  
    Palavra[] memoria;

    public Memoria(int tamanhoMemoria) {
         
        if (tamanhoMemoria < TAMANHO_MINIMO){
            throw new IllegalArgumentException("Tamanho de memoria menor que o tamanho minimo");
        }
      
        memoria = new Palavra[tamanhoMemoria];
        for (int i =0; i < memoria.length; i++) {
            memoria[i] = new Palavra(0);
        }
    }

    public void escrever(int endereco, int valor) {
        memoria[endereco].setValor(valor);
    }
}
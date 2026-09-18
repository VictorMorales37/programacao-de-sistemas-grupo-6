package cpu;

public class SW extends Registrador {

    public enum CodigoCondicional {
        MENOR,
        IGUAL,
        MAIOR
    }

    private CodigoCondicional codigoCondicional;

    public SW() {
        super("SW", 9, TAMANHO_24_BITS);
        codigoCondicional = null;
    }

    public CodigoCondicional getCodigoCondicional() {
        return codigoCondicional;
    }

    public void setCodigoCondicional(CodigoCondicional codigoCondicional) {
        if (codigoCondicional == null) {
            throw new IllegalArgumentException("Codigo condicional nao pode ser nulo");
        }
        this.codigoCondicional = codigoCondicional;
    }

    @Override
    public void limpar() {
        super.limpar();
        codigoCondicional = null;
    }
}

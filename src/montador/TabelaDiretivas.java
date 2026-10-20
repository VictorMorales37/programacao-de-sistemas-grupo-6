package montador;

/** Diretivas do montador: quantidade de operandos esperada (mínimo e máximo) e descrição. */
public enum TabelaDiretivas {
    START(1, 1, "Define o contador de posição inicial e o endereço de entrada padrão"),
    END(0, 1, "Encerra o programa; o operando opcional define endereço de entrada"),
    WORD(1, 1, "Grava uma palavra (3 bytes, 0..0xFFFFFF)"),
    RESW(1, 1, "Reserva n palavras sem gravar nada"),
    RESB(1, 1, "Reserva n bytes, arredondando para cima em palavras (ceil(n/3))"),
    EQU(1, 1, "Define o rótulo com o valor do operando (número ou símbolo já definido), sem ocupar memória"),
    ORG(1, 1, "Reinicializa o contador de posição (LC) com o valor do operando"),
    BASE(1, 1, "Valor que o registrador B terá na execução; permite endereçamento relativo à base no formato 3"),
    BYTE(1, 1, "Grava bytes: X'0F1A' (hexadecimal, par de dígitos) ou C'ab' (caracteres)");

    private final int minOperandos;
    private final int maxOperandos;
    private final String descricao;

    TabelaDiretivas(int minOperandos, int maxOperandos, String descricao) {
        this.minOperandos = minOperandos;
        this.maxOperandos = maxOperandos;
        this.descricao = descricao;
    }

    public int minOperandos() { return minOperandos; }
    public int maxOperandos() { return maxOperandos; }
    public String descricao() { return descricao; }

    /** Encontra a diretiva pelo mnemônico (sem diferenciar maiúsculas), ou null. */
    public static TabelaDiretivas de(String mnemonico) {
        try {
            return valueOf(mnemonico.toUpperCase());
        } catch (IllegalArgumentException desconhecida) {
            return null;
        }
    }

    public boolean aceitaQuantidade(int quantidade) {
        return quantidade >= minOperandos && quantidade <= maxOperandos;
    }
}

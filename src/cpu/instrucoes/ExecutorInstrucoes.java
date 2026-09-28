package cpu.instrucoes;

import cpu.*;
import memoria.Palavra;

public final class ExecutorInstrucoes {
    public int calcularEnderecoEfetivo(CPU maquina, Instrucao instrucao) {
        Registradores registradores = maquina.obterRegistradores();
        int alvo = instrucao.campo();
        if (instrucao.p())
            alvo = instrucao.proximoEndereco() + (alvo >= 0x800 ? alvo - 0x1000 : alvo);
        else if (instrucao.b())
            alvo += (int) registradores.obterB().obterValor();
        if (instrucao.x())
            alvo += (int) registradores.obterX().obterValor();
        if (alvo < 0 || alvo > 0xFFFFFF)
            throw new IllegalArgumentException("O endereço de destino excede os limites permitidos");
        if (instrucao.ehIndireta())
            alvo = maquina.lerPalavra(alvo);
        return alvo;
    }

    private Registrador obterRegistradorInteiro(Registradores registradores, int numero) {
        Registrador resultado = registradores.obterPorNumero(numero);
        if (resultado.obterLarguraBits() != 24)
            throw new IllegalArgumentException("Operações com inteiros não podem usar o registrador F");
        return resultado;
    }

    private long dividir(long dividendo, long divisor) {
        if (divisor == 0)
            throw new ArithmeticException("Divisão por zero");
        return dividendo / divisor;
    }

    public void executar(CPU maquina, Instrucao instrucao) {
        Registradores registradores = maquina.obterRegistradores();
        Opcode codigoOperacao = instrucao.codigoOperacao();

        if (instrucao.formato() == 2) {
            int primeiro = instrucao.primeiroRegistrador();
            int segundo = instrucao.segundoRegistrador();
            if (codigoOperacao == Opcode.CLEAR) {
                if (segundo != 0)
                    throw new IllegalArgumentException("CLEAR exige que os quatro bits menos significativos sejam zero");
                registradores.obterPorNumero(primeiro).limpar();
                return;
            }

            Registrador esquerdo = obterRegistradorInteiro(registradores, primeiro);
            if (codigoOperacao == Opcode.TIXR) {
                if (segundo != 0)
                    throw new IllegalArgumentException("TIXR exige que os quatro bits menos significativos sejam zero");
                registradores.obterX().definirValor(registradores.obterX().obterValor() + 1);
                registradores.obterSW().comparar(Palavra.comSinal(registradores.obterX().obterValor()),
                        Palavra.comSinal(esquerdo.obterValor()));
                return;
            }

            if (codigoOperacao == Opcode.SHIFTL || codigoOperacao == Opcode.SHIFTR) {
                int quantidade = segundo + 1;
                long valor = esquerdo.obterValor();
                esquerdo.definirValor(codigoOperacao == Opcode.SHIFTL
                        ? (valor << quantidade) | (valor >>> (24 - quantidade))
                        : Palavra.comSinal(valor) >> quantidade);
                return;
            }

            Registrador direito = obterRegistradorInteiro(registradores, segundo);
            long valorEsquerdo = Palavra.comSinal(esquerdo.obterValor());
            long valorDireito = Palavra.comSinal(direito.obterValor());
            switch (codigoOperacao) {
                case ADDR -> direito.definirValor(valorDireito + valorEsquerdo);
                case SUBR -> direito.definirValor(valorDireito - valorEsquerdo);
                case MULR -> direito.definirValor(valorDireito * valorEsquerdo);
                case DIVR -> direito.definirValor(dividir(valorDireito, valorEsquerdo));
                case COMPR -> registradores.obterSW().comparar(valorEsquerdo, valorDireito);
                case RMO -> direito.definirValor(esquerdo.obterValor());
                default -> throw new IllegalArgumentException("Instrução de registrador inválida");
            }
            return;
        }

        if (codigoOperacao == Opcode.RSUB) {
            if (instrucao.campo() != 0 || instrucao.x() || instrucao.b() || instrucao.p()
                    || instrucao.ehImediata() || instrucao.ehIndireta())
                throw new IllegalArgumentException("RSUB não possui operando");
            int endereco = (int) registradores.obterL().obterValor();
            maquina.validarEndereco(endereco);
            registradores.obterPC().definirValor(endereco);
            return;
        }

        int endereco = calcularEnderecoEfetivo(maquina, instrucao);
        if (codigoOperacao == Opcode.J || codigoOperacao == Opcode.JEQ || codigoOperacao == Opcode.JLT
                || codigoOperacao == Opcode.JGT || codigoOperacao == Opcode.JSUB) {
            boolean deveSaltar = switch (codigoOperacao) {
                case JEQ -> registradores.obterSW().obterCodigoCondicao() == SW.CodigoCondicao.IGUAL;
                case JLT -> registradores.obterSW().obterCodigoCondicao() == SW.CodigoCondicao.MENOR;
                case JGT -> registradores.obterSW().obterCodigoCondicao() == SW.CodigoCondicao.MAIOR;
                default -> true;
            };
            if (deveSaltar) {
                maquina.validarEndereco(endereco);
                if (codigoOperacao == Opcode.JSUB)
                    registradores.obterL().definirValor(registradores.obterPC().obterValor());
                registradores.obterPC().definirValor(endereco);
            }
            return;
        }

        Registrador registradorArmazenamento = switch (codigoOperacao) {
            case STA, STCH -> registradores.obterA();
            case STB -> registradores.obterB();
            case STL -> registradores.obterL();
            case STS -> registradores.obterS();
            case STT -> registradores.obterT();
            case STX -> registradores.obterX();
            default -> null;
        };
        if (registradorArmazenamento != null) {
            if (instrucao.ehImediata())
                throw new IllegalArgumentException("Instruções de armazenamento não podem usar endereçamento imediato");
            if (codigoOperacao == Opcode.STCH)
                maquina.obterMemoria().escreverByte(maquina.enderecoEmBytes(endereco), (int) registradorArmazenamento.obterValor());
            else
                maquina.escreverPalavra(endereco, registradorArmazenamento.obterValor());
            return;
        }

        int valor = instrucao.ehImediata() ? endereco
                : codigoOperacao == Opcode.LDCH ? maquina.obterMemoria().lerByte(maquina.enderecoEmBytes(endereco))
                : maquina.lerPalavra(endereco);
        long acumulador = Palavra.comSinal(registradores.obterA().obterValor());
        long operando = Palavra.comSinal(valor);

        switch (codigoOperacao) {
            case LDA -> registradores.obterA().definirValor(valor);
            case LDB -> registradores.obterB().definirValor(valor);
            case LDL -> registradores.obterL().definirValor(valor);
            case LDS -> registradores.obterS().definirValor(valor);
            case LDT -> registradores.obterT().definirValor(valor);
            case LDX -> registradores.obterX().definirValor(valor);
            case LDCH -> registradores.obterA().definirValor((registradores.obterA().obterValor() & 0xFFFF00) | (valor & 255));
            case ADD -> registradores.obterA().definirValor(acumulador + operando);
            case SUB -> registradores.obterA().definirValor(acumulador - operando);
            case MUL -> registradores.obterA().definirValor(acumulador * operando);
            case DIV -> registradores.obterA().definirValor(dividir(acumulador, operando));
            case AND -> registradores.obterA().definirValor(registradores.obterA().obterValor() & valor);
            case OR -> registradores.obterA().definirValor(registradores.obterA().obterValor() | valor);
            case COMP -> registradores.obterSW().comparar(acumulador, operando);
            case TIX -> {
                registradores.obterX().definirValor(registradores.obterX().obterValor() + 1);
                registradores.obterSW().comparar(Palavra.comSinal(registradores.obterX().obterValor()), operando);
            }
            default -> throw new IllegalArgumentException("Instrução não suportada");
        }
    }
}

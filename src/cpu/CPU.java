package cpu;

public class CPU {
    //Armazena os dados (carregados e resultantes) das operações da Unid. de Lógica e Aritmética
    Registrador rA = new Registrador(0, 24);

    //Usado para endereçamento.
    Registrador rX = new Registrador(1, 24);

    //A instrução Jump to Subrotine (JSUB) armazena o endereço de retorno nesse registrador.
    Registrador rL = new Registrador(2, 24);

    //Usado para endereçamento.
    Registrador rB = new Registrador(3, 24);

    //Uso geral
    Registrador rS = new Registrador(4, 24);
    Registrador rT = new Registrador(5, 24);

    //Armazena os dados (carregados e resultantes) das operações da Unid. de Lógica e Aritmética em ponto flutuante.
    Registrador rF = new Registrador(6, 48);

    //Mantém o endereço da próxima instrução a ser executada
    Registrador rPC = new Registrador(8, 24);

    //Contém várias informações, incluindo código condicional (CC)
    Registrador rSW= new Registrador(9, 24);
}

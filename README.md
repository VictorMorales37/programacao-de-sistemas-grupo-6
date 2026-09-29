# programacao-de-sistemas-grupo-6

# Simulador SIC/XE

Projeto de um simulador SIC/XE, computador hipotético baseado na
arquitetura do livro: Systems Software: An Introduction to System
Programming; Leland L. Beck

## Parte I

## Estruturas de dados

| Estrutura | Descrição |
| --- | --- |
| `Memoria` | Vetor de bytes com capacidade padrão de 3 MiB. Fornece acesso validado a bytes e a palavras de três bytes em big-endian. |
| `Palavra` | Representa uma palavra de 24 bits usando um inteiro limitado por `0xFFFFFF`. Também permite interpretação com sinal em complemento de dois. |
| `Registrador` | Armazena identificador, número, largura e valor de um registrador. Usa `long` e uma máscara de bits para suportar registradores de 24 e 48 bits. |
| `Registradores` | Contém A, X, L, B, S, T, F, PC e SW. Permite consulta, limpeza e captura/restauração do estado dos registradores. |
| `SW` | Estende `Registrador` e representa a palavra de estado. Seus dois bits menos significativos codificam IGUAL, MENOR ou MAIOR. |
| `Opcode` | Enumeração que associa as 38 instruções exigidas aos respectivos códigos de máquina e formatos base. |
| `Instrucao` | Registro imutável com codigoOperacao decodificado, formato, endereços, registradores operandos, indicadores de endereçamento e campo codificado. |
| `ImagemPrograma` | Armazena as entradas iniciais do programa, o endereço de entrada, o endereço de parada e o conjunto de endereços válidos de instrução. |
| `ImagemPrograma.Entrada` | Registro com endereço, vetor de bytes copiado defensivamente e indicador de entrada executável. |

## Funções principais

| Função | Responsabilidade |
| --- | --- |
| `Memoria.lerByte()` / `escreverByte()` | Lê ou escreve um byte físico depois de validar o endereço. |
| `Memoria.lerPalavra()` / `escreverPalavra()` | Lê ou escreve uma palavra completa de 24 bits. |
| `Registradores.capturarEstado()` / `restaurarEstado()` | Salva e restaura os valores dos registradores quando ocorre um erro de execução. |
| `SW.comparar()` | Atualiza o código de condição a partir de uma comparação. |
| `Decodificador.decodificar()` | Interpreta os bytes da instrução, extrai operandos e indicadores e calcula o endereço da próxima instrução. |
| `ExecutorInstrucoes.calcularEnderecoEfetivo()` | Calcula o endereço efetivo, incluindo endereçamento relativo, indexado e indireto. |
| `ExecutorInstrucoes.executar()` | Executa a operação aritmética, lógica, de transferência, comparação ou desvio da instrução decodificada. |
| `CPU.executarPasso()` | Executa um ciclo de busca, decodificação e execução e contabiliza as instruções concluídas. |
| `CarregadorPrograma.analisar()` | Valida uma listagem hexadecimal e cria a imagem inicial do programa. |
| `ImagemPrograma.criarCPU()` | Cria uma nova máquina com a memória inicial do programa e o PC configurado. |
| `PainelSimulador.carregarFonte()` | Carrega ou reinicia o programa exibido no editor. |
| `PainelSimulador.executarPasso()` / `atualizar()` | Executa uma instrução e atualiza a exibição do estado da máquina. |
| `PainelSimulador.executarTudo()` / `executarLote()` | Executa automaticamente em lotes limitados, mantendo a interface responsiva. |
| `Main.main()` | Inicializa a aplicação Swing. |

## Estratégias de implementação

### Separação de responsabilidades

A memória e os registradores representam o estado da máquina. O decodificador interpreta a codificação das instruções, enquanto o executor implementa o comportamento de cada instrução. A CPU coordena esses componentes. A interface solicita operações e apresenta os resultados.

### Endereçamento de memória

A especificação do trabalho define endereçamento por palavra, mas descreve instruções de tamanho variável usando notação por bytes. Esta implementação adota endereços lógicos por palavra: o endereço `a` corresponde ao deslocamento físico de bytes `3 * a`.

Cada instrução começa no limite de uma palavra. Os formatos 2 e 3 ocupam uma palavra; o formato 4 ocupa duas palavras, com bytes finais não utilizados reservados como preenchimento. PC, B, X e os cálculos de endereço usam unidades de palavra. `LDCH` e `STCH` acessam o primeiro byte da palavra endereçada.

### Execução das instruções

O simulador implementa as 38 instruções não destacadas em vermelho no enunciado. Instruções de ponto flutuante e de dispositivos marcadas em vermelho são rejeitadas; F permanece disponível como armazenamento de 48 bits.

O decodificador suporta os formatos 2, 3 e 4, além da compatibilidade SIC. São aceitos os modos imediato, simples, indireto, indexado, relativo à base e relativo ao PC. Deslocamentos relativos ao PC usam interpretação com sinal de 12 bits.

As operações aritméticas usam operandos de 24 bits com sinal; as máscaras dos registradores preservam os 24 bits menos significativos do resultado. Divisão por zero é rejeitada. O PC avança antes da execução para que o endereçamento relativo e `JSUB` usem o endereço da próxima instrução.

### Validação e tratamento de erros

As operações de memória validam todo o intervalo antes da escrita. Opcodes inválidos, combinações inválidas de indicadores, entradas de programa sobrepostas e endereços de execução inválidos são rejeitados.

Se a execução falhar, a CPU restaura os valores dos registradores e não incrementa o contador de ciclos. A interface exibe o erro e exige um novo carregamento antes de continuar a execução.

### Entrada do programa e conclusão

O carregador aceita os comandos `ENTRY`, `STOP`, `CODE` e `WORD`. Endereços e valores são hexadecimais, e `;` inicia um comentário.

O carregador lê diretamente código de máquina; ele não funciona como montador. `STOP` define um limite externo de conclusão, e não uma instrução adicional da máquina.

### Controles de execução

**Passo** executa uma instrução. **Executar** continua automaticamente a partir do estado atual da máquina até `STOP` ou até ocorrer um erro de execução. Durante a execução, o mesmo botão passa a ser **Pausar**, permitindo retornar ao modo manual. **Carregar / reiniciar** interrompe a execução e recarrega o conteúdo do editor.

Um temporizador Swing executa lotes de até 100 instruções e devolve o controle à linha de execução de eventos entre os lotes. As tabelas de registradores e memória são atualizadas depois de cada lote. O botão **Passo** fica desabilitado durante a execução contínua. Cada sessão de **Executar** pausa após 100.000 instruções para impedir que um laço infinito acidental continue indefinidamente; depois disso, a execução pode ser inspecionada ou retomada.

### Decisões sobre a especificação

Todos os nove registradores listados no trabalho são implementados. `CLEAR` usa o codigoOperacao `B4`, preservando `04` para `LDX`. O formato 2 usa um codigoOperacao de 8 bits seguido por dois campos de operandos de 4 bits. Essas decisões resolvem inconsistências presentes na especificação fornecida.

## Como executar

Requer Java 17 ou superior.

```sh
java -jar dist/sic-xe-simulator.jar
```

No Windows, `run.bat` inicia a aplicação. Para recompilar, é necessário ter um JDK completo instalado:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

Extraia completamente o arquivo ZIP antes de abrir `run.bat`. Na interface, use **Carregar / reiniciar** para recomeçar, **Passo** para avançar manualmente ou **Executar** para executar o programa automaticamente.

O exemplo incluído calcula `1 + 2 + 3 + 4 + 5`. Depois de 19 passos, A e o endereço de memória `000100` contêm `00000F`, equivalente a 15 em decimal.

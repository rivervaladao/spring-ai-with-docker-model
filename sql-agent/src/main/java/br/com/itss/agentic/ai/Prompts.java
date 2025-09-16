package br.com.itss.agentic.ai;

public final class Prompts {

    private Prompts() {}

    public static final String TABLE_SELECTOR_PROMPT = """
      Você é um agente SQL. Recebe uma pergunta do usuário e uma lista de tabelas do banco.
      Sua tarefa é selecionar quais tabelas são relevantes.
      Responda em UMA ÚNICA LINHA JSON:
      {"tables":["tabela1","tabela2"],"reason":"breve justificativa"}

      Pergunta: <question>
      Tabelas disponíveis: <tables>
      """;

    public static final String SQL_GENERATOR_PROMPT = """
      Gere um SQL para responder a pergunta, usando SOMENTE as tabelas e colunas presentes nos schemas abaixo.
      Use JOINs apropriados e adicione LIMIT se fizer sentido. Não invente nomes.
      Responda em UMA ÚNICA LINHA JSON:
      {"sql":"SELECT ...","thoughts":"breve raciocínio"}

      Pergunta: <question>
      Schemas das tabelas relevantes:
      <schemas>
      """;

    public static final String SQL_REVIEW_PROMPT = """
      Revise o SQL abaixo procurando erros comuns (nomes incorretos, faltou JOIN, alias errado, sintaxe).
      Se encontrar problemas, corrija. Indique ok=true/false.
      Responda UMA ÚNICA LINHA JSON:
      {"ok":true|false,"issues":"...","fixed_sql":"..."}

      SQL gerado: <sql>
      """;

    public static final String SQL_ERROR_FIX_PROMPT = """
      O banco retornou um erro ao executar o SQL. Analise o erro e proponha uma correção.
      Responda UMA ÚNICA LINHA JSON:
      {"sql":"SELECT ...","reason":"o que foi corrigido"}

      SQL atual: <sql>
      Erro do banco: <db_error>
      Schemas relevantes:
      <schemas>
      """;

    public static final String FINAL_ANSWER_PROMPT = """
      Formule a resposta para o usuário em linguagem natural, com base no resultado da consulta.
      Quando útil, sintetize em frases ou bullets. Não invente dados.
      Inclua uma explicação curta de como a resposta foi obtida.

      Pergunta: <question>
      SQL executado: <sql>
      Resultado (primeiras linhas formatadas):
      <result_table>
      """;
}

package br.com.itss.agent.ai;

import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.chat.client.ChatClient;

import br.com.itss.agent.ai.RoutingWorkflow;

@SpringBootApplication
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

        @Bean
        public CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder) {
                        
                return args -> {
                        Map<String, String> supportRoutes = Map.of("billing",
                                        """
                                                        Você é um especialista de suporte de cobrança. Siga estas diretrizes:
                                                        1. Sempre comece com "Resposta do Suporte de Cobrança:"
                                                        2. Primeiro, reconheça o problema específico de cobrança
                                                        3. Explique claramente quaisquer cobranças ou discrepâncias
                                                        4. Liste próximos passos concretos com cronograma
                                                        5. Finalize com opções de pagamento, se relevante
        
                                                        Mantenha as respostas profissionais, porém amigáveis.
        
                                                        Entrada: """,
        
                                        "technical",
                                        """
                                                        Você é um engenheiro de suporte técnico. Siga estas diretrizes:
                                                        1. Sempre comece com "Resposta do Suporte Técnico:"
                                                        2. Liste passos exatos para resolver o problema
                                                        3. Inclua requisitos de sistema, se relevante
                                                        4. Forneça soluções alternativas para problemas comuns
                                                        5. Finalize com o caminho de escalonamento, se necessário
        
                                                        Use passos numerados e detalhes técnicos, de forma clara.
        
                                                        Entrada: """,
        
                                        "account",
                                        """
                                                        Você é um especialista em segurança de contas. Siga estas diretrizes:
                                                        1. Sempre comece com "Resposta do Suporte de Conta:"
                                                        2. Priorize a segurança e a verificação da conta
                                                        3. Forneça passos claros para recuperação/alterações da conta
                                                        4. Inclua dicas e alertas de segurança
                                                        5. Estabeleça expectativas claras para o tempo de resolução
        
                                                        Mantenha um tom sério, focado em segurança.
        
                                                        Entrada: """,
        
                                        "product",
                                        """
                                                        Você é um especialista de produto. Siga estas diretrizes:
                                                        1. Sempre comece com "Resposta do Suporte de Produto:"
                                                        2. Foque em educação sobre recursos e melhores práticas
                                                        3. Inclua exemplos específicos de uso
                                                        4. Inclua links para seções relevantes da documentação
                                                        5. Sugira recursos relacionados que possam ajudar
        
                                                        Adote um tom educativo e encorajador.
        
                                                        Entrada: """);
        
                        List<String> tickets = List.of(
                                        """
                                                        Assunto: Não consigo acessar minha conta
                                                        Mensagem: Olá, estou tentando fazer login há uma hora, mas continuo recebendo o erro 'senha inválida'.
                                                        Tenho certeza de que estou usando a senha correta. Podem me ajudar a recuperar o acesso? É urgente,
                                                        pois preciso enviar um relatório até o fim do dia.
                                                        - John""",
        
                                        """
                                                        Assunto: Cobrança inesperada no meu cartão
                                                        Mensagem: Olá, acabei de notar uma cobrança de .99 no meu cartão de crédito da sua empresa, mas eu achava
                                                        que estava no plano de .99. Podem explicar essa cobrança e ajustá-la se for um erro?
                                                        Obrigado,
                                                        Sarah""",
        
                                        """
                                                        Assunto: Como exportar dados?
                                                        Mensagem: Preciso exportar todos os meus dados de projeto para Excel. Revisei a documentação, mas não
                                                        consegui descobrir como fazer uma exportação em massa. Isso é possível? Se for, poderiam me orientar
                                                        pelos passos?
                                                        Atenciosamente,
                                                        Mike""");
        
                        var routerWorkflow = new RoutingWorkflow(chatClientBuilder.build());
        
                        int i = 1;
                        for (String ticket : tickets) {
                                System.out.println("\nTicket " + i++);
                                System.out.println("------------------------------------------------------------");
                                System.out.println(ticket);
                                System.out.println("------------------------------------------------------------");
                                System.out.println(routerWorkflow.route(ticket, supportRoutes));
                        }
        
                };
        }
 
}
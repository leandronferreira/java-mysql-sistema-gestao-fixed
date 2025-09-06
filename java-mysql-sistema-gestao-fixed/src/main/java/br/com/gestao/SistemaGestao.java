
package br.com.gestao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Scanner;

class Usuario {
    public int id;
    public String nome;
    public String email;
    public String login;
    public String senha;
    public String perfil;
    public Usuario(String nome, String email, String login, String senha, String perfil) {
        this.nome = nome; this.email = email; this.login = login; this.senha = senha; this.perfil = perfil;
    }
    public Usuario() {}
}

class Projeto {
    public int id;
    public String nome;
    public String descricao;
    public LocalDate dataInicio;
    public LocalDate dataFim;
    public String status;
    public Integer gerenteId;
    public Projeto(String nome, String descricao, LocalDate dataInicio, LocalDate dataFim, String status, Integer gerenteId) {
        this.nome = nome; this.descricao = descricao; this.dataInicio = dataInicio; this.dataFim = dataFim; this.status = status; this.gerenteId = gerenteId;
    }
    public Projeto() {}
}

public class SistemaGestao {
    private static String URL; private static String USUARIO; private static String SENHA;
    private static final Scanner scanner = new Scanner(System.in);
    private static Usuario usuarioLogado = null;
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void main(String[] args) {
        System.out.println("=== Sistema de Gestão de Projetos ===");
        carregarProps();
        criarBanco();
        while (true) { if (usuarioLogado == null) { menuLogin(); } else { menuPrincipal(); } }
    }

    private static void carregarProps() {
        Properties p = new Properties();
        try (InputStream in = SistemaGestao.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) throw new IOException("application.properties não encontrado");
            p.load(in);
            URL = p.getProperty("db.url");
            USUARIO = p.getProperty("db.user");
            SENHA = p.getProperty("db.pass");
        } catch (IOException e) { throw new RuntimeException("Falha carregando configurações: " + e.getMessage(), e); }
    }

    public static void criarBanco() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", USUARIO, SENHA);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS gestao_projetos");
            System.out.println("✓ Banco verificado/criado!");
            stmt.executeUpdate("USE gestao_projetos");
            String tabelaUsuarios =
                "CREATE TABLE IF NOT EXISTS usuarios (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "nome VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) UNIQUE NOT NULL, " +
                "login VARCHAR(50) UNIQUE NOT NULL, " +
                "senha VARCHAR(60) NOT NULL, " +
                "perfil ENUM('ADMIN','GERENTE','COLABORADOR') NOT NULL" +
                ") ENGINE=InnoDB";
            stmt.executeUpdate(tabelaUsuarios);
            String tabelaProjetos =
                "CREATE TABLE IF NOT EXISTS projetos (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "nome VARCHAR(100) NOT NULL, " +
                "descricao TEXT, " +
                "data_inicio DATE, " +
                "data_fim DATE, " +
                "status ENUM('PLANEJAMENTO','ANDAMENTO','CONCLUIDO') NOT NULL, " +
                "gerente_id INT, " +
                "CONSTRAINT fk_proj_user FOREIGN KEY (gerente_id) REFERENCES usuarios(id) " +
                "ON UPDATE CASCADE ON DELETE SET NULL" +
                ") ENGINE=InnoDB";
            stmt.executeUpdate(tabelaProjetos);
            System.out.println("✓ Tabelas ok!");
            criarAdminPadrao();
        } catch (SQLException e) { System.out.println("Erro ao criar banco: " + e.getMessage()); }
    }

    public static void criarAdminPadrao() {
        String verificar = "SELECT COUNT(*) FROM usuarios WHERE perfil = 'ADMIN'";
        try (Connection conn = DriverManager.getConnection(URL, USUARIO, SENHA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(verificar)) {
            rs.next();
            if (rs.getInt(1) == 0) {
                String inserir = "INSERT INTO usuarios (nome, email, login, senha, perfil) " +
                                 "VALUES ('Administrador', 'admin@sistema.com', 'admin', '123', 'ADMIN')";
                stmt.executeUpdate(inserir);
                System.out.println("✓ Usuário admin criado! Login: admin | Senha: 123");
            }
        } catch (SQLException e) { System.out.println("Erro: " + e.getMessage()); }
    }

    public static void menuLogin() {
        System.out.println("\\n=== LOGIN ===");
        System.out.print("Login: ");
        String login = scanner.nextLine();
        System.out.print("Senha: ");
        String senha = scanner.nextLine();
        usuarioLogado = fazerLogin(login, senha);
        if (usuarioLogado == null) { System.out.println("❌ Login ou senha incorretos!"); }
        else { System.out.println("✓ Bem-vindo, " + usuarioLogado.nome + "!"); }
    }

    public static Usuario fazerLogin(String login, String senha) {
        String sql = "SELECT * FROM usuarios WHERE login = ? AND senha = ?";
        try (Connection conn = DriverManager.getConnection(URL, USUARIO, SENHA);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login); pstmt.setString(2, senha);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Usuario user = new Usuario();
                    user.id = rs.getInt("id"); user.nome = rs.getString("nome");
                    user.email = rs.getString("email"); user.login = rs.getString("login");
                    user.perfil = rs.getString("perfil");
                    return user;
                }
            }
        } catch (SQLException e) { System.out.println("Erro no login: " + e.getMessage()); }
        return null;
    }

    public static void menuPrincipal() {
        System.out.println("\\n=== MENU PRINCIPAL ===");
        System.out.println("Usuário: " + usuarioLogado.nome + " (" + usuarioLogado.perfil + ")");
        System.out.println("1. Gerenciar Usuários");
        System.out.println("2. Gerenciar Projetos");
        System.out.println("3. Relatórios");
        System.out.println("4. Logout");
        System.out.print("Escolha uma opção: ");
        String opcao = scanner.nextLine();
        switch (opcao) {
            case "1":
                if ("ADMIN".equals(usuarioLogado.perfil)) menuUsuarios();
                else System.out.println("❌ Acesso negado! Apenas administradores.");
                break;
            case "2": menuProjetos(); break;
            case "3": menuRelatorios(); break;
            case "4": usuarioLogado = null; System.out.println("✓ Logout realizado!"); break;
            default: System.out.println("❌ Opção inválida!");
        }
    }

    public static void menuUsuarios() {
        System.out.println("\\n=== GERENCIAR USUÁRIOS ===");
        System.out.println("1. Cadastrar usuário");
        System.out.println("2. Listar usuários");
        System.out.println("3. Voltar");
        System.out.print("Escolha uma opção: ");
        String opcao = scanner.nextLine();
        switch (opcao) {
            case "1": cadastrarUsuario(); break;
            case "2": listarUsuarios(); break;
            case "3": return;
            default: System.out.println("❌ Opção inválida!");
        }
    }

    public static void cadastrarUsuario() {
        System.out.println("\\n=== CADASTRAR USUÁRIO ===");
        System.out.print("Nome completo: "); String nome = scanner.nextLine();
        System.out.print("E-mail: "); String email = scanner.nextLine();
        System.out.print("Login: "); String login = scanner.nextLine();
        System.out.print("Senha: "); String senha = scanner.nextLine();
        System.out.println("Perfil:\n1. ADMIN\n2. GERENTE\n3. COLABORADOR\nEscolha: ");
        String escolhaPerfil = scanner.nextLine();
        String perfil;
        if ("1".equals(escolhaPerfil)) perfil = "ADMIN";
        else if ("2".equals(escolhaPerfil)) perfil = "GERENTE";
        else if ("3".equals(escolhaPerfil)) perfil = "COLABORADOR";
        else { System.out.println("❌ Perfil inválido!"); return; }

        String sql = "INSERT INTO usuarios (nome, email, login, senha, perfil) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USUARIO, SENHA);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nome); pstmt.setString(2, email); pstmt.setString(3, login);
            pstmt.setString(4, senha); pstmt.setString(5, perfil);
            int resultado = pstmt.executeUpdate();
            if (resultado > 0) System.out.println("✓ Usuário cadastrado com sucesso!");
        } catch (SQLException e) { System.out.println("❌ Erro ao cadastrar: " + e.getMessage()); }
    }

    public static void listarUsuarios() {
        System.out.println("\\n=== LISTA DE USUÁRIOS ===");
        String sql = "SELECT * FROM usuarios ORDER BY nome";
        try (Connection conn = DriverManager.getConnection(URL, USUARIO, SENHA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.printf("%-5s %-20s %-25s %-15s %-12s%n", "ID", "NOME", "EMAIL", "LOGIN", "PERFIL");
            System.out.println("-----------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-5d %-20s %-25s %-15s %-12s%n",
                        rs.getInt("id"), rs.getString("nome"), rs.getString("email"),
                        rs.getString("login"), rs.getString("perfil"));
            }
        } catch (SQLException e) { System.out.println("❌ Erro ao listar usuários: " + e.getMessage()); }
    }

    public static void menuProjetos() {
        System.out.println("\\n=== GERENCIAR PROJETOS ===");
        System.out.println("1. Cadastrar projeto\n2. Listar projetos\n3. Voltar\nEscolha: ");
        String opcao = scanner.nextLine();
        switch (opcao) {
            case "1": cadastrarProjeto(); break;
            case "2": listarProjetos(); break;
            case "3": return;
            default: System.out.println("❌ Opção inválida!");
        }
    }

    public static void cadastrarProjeto() {
        System.out.println("\\n=== CADASTRAR PROJETO ===");
        System.out.print("Nome do projeto: "); String nome = scanner.nextLine();
        System.out.print("Descrição: "); String descricao = scanner.nextLine();
        System.out.print("Data de início (dd/mm/yyyy): "); String dataInicioStr = scanner.nextLine();
        System.out.print("Data de término prevista (dd/mm/yyyy): "); String dataFimStr = scanner.nextLine();
        System.out.println("Status:\n1. PLANEJAMENTO\n2. ANDAMENTO\n3. CONCLUIDO\nEscolha: ");
        String escolhaStatus = scanner.nextLine();
        String status;
        if ("1".equals(escolhaStatus)) status = "PLANEJAMENTO";
        else if ("2".equals(escolhaStatus)) status = "ANDAMENTO";
        else if ("3".equals(escolhaStatus)) status = "CONCLUIDO";
        else { System.out.println("❌ Status inválido!"); return; }
        listarGerentes();
        System.out.print("ID do gerente responsável: ");
        Integer gerenteId = Integer.parseInt(scanner.nextLine());
        try {
            LocalDate dataInicio = LocalDate.parse(dataInicioStr, BR);
            LocalDate dataFim = LocalDate.parse(dataFimStr, BR);
            String sql = "INSERT INTO projetos (nome, descricao, data_inicio, data_fim, status, gerente_id) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(URL, USUARIO, SENHA);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nome); pstmt.setString(2, descricao);
                pstmt.setDate(3, java.sql.Date.valueOf(dataInicio));
                pstmt.setDate(4, java.sql.Date.valueOf(dataFim));
                pstmt.setString(5, status); pstmt.setObject(6, gerenteId);
                int resultado = pstmt.executeUpdate();
                if (resultado > 0) System.out.println("✓ Projeto cadastrado com sucesso!");
            }
        } catch (Exception e) { System.out.println("❌ Erro ao cadastrar projeto: " + e.getMessage()); }
    }

    public static void listarGerentes() {
        System.out.println("\\n=== GERENTES DISPONÍVEIS ===");
        String sql = "SELECT id, nome FROM usuarios WHERE perfil IN ('ADMIN','GERENTE') ORDER BY nome";
        try (Connection conn = DriverManager.getConnection(URL, USUARIO, SENHA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) { System.out.println(rs.getInt("id") + " - " + rs.getString("nome")); }
        } catch (SQLException e) { System.out.println("❌ Erro: " + e.getMessage()); }
    }

    public static void listarProjetos() {
        System.out.println("\\n=== LISTA DE PROJETOS ===");
        String sql = "SELECT p.id, p.nome, DATE_FORMAT(p.data_inicio, '%d/%m/%Y') AS di, " +
                     "DATE_FORMAT(p.data_fim, '%d/%m/%Y') AS df, p.status, u.nome AS nome_gerente " +
                     "FROM projetos p LEFT JOIN usuarios u ON p.gerente_id = u.id " +
                     "ORDER BY p.nome";
        try (Connection conn = DriverManager.getConnection(URL, USUARIO, SENHA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.printf("%-5s %-22s %-12s %-12s %-15s %-20s%n", "ID", "PROJETO", "INÍCIO", "FIM", "STATUS", "GERENTE");
            System.out.println("--------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-5d %-22s %-12s %-12s %-15s %-20s%n",
                        rs.getInt("id"), rs.getString("nome"), rs.getString("di"), rs.getString("df"),
                        rs.getString("status"), rs.getString("nome_gerente"));
            }
        } catch (SQLException e) { System.out.println("❌ Erro ao listar projetos: " + e.getMessage()); }
    }

    public static void menuRelatorios() {
        System.out.println("\\n=== RELATÓRIOS ===");
        System.out.println("1. Resumo do sistema\n2. Projetos em andamento\n3. Voltar\nEscolha: ");
        String opcao = scanner.nextLine();
        switch (opcao) {
            case "1": relatorioResumo(); break;
            case "2": projetosAndamento(); break;
            case "3": return;
            default: System.out.println("❌ Opção inválida!");
        }
    }

    public static void relatorioResumo() {
        System.out.println("\\n=== RESUMO DO SISTEMA ===");
        try (Connection conn = DriverManager.getConnection(URL, USUARIO, SENHA);
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) FROM usuarios")) {
                rs1.next(); System.out.println("Total de usuários: " + rs1.getInt(1));
            }
            try (ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) FROM projetos")) {
                rs2.next(); System.out.println("Total de projetos: " + rs2.getInt(1));
            }
            try (ResultSet rs3 = stmt.executeQuery("SELECT status, COUNT(*) FROM projetos GROUP BY status")) {
                System.out.println("\\nProjetos por status:");
                while (rs3.next()) { System.out.println("- " + rs3.getString(1) + ": " + rs3.getInt(2)); }
            }
        } catch (SQLException e) { System.out.println("❌ Erro no relatório: " + e.getMessage()); }
    }

    public static void projetosAndamento() {
        System.out.println("\\n=== PROJETOS EM ANDAMENTO ===");
        String sql = "SELECT p.nome, DATE_FORMAT(p.data_fim, '%d/%m/%Y') AS df, u.nome as gerente " +
                     "FROM projetos p LEFT JOIN usuarios u ON p.gerente_id = u.id " +
                     "WHERE p.status = 'ANDAMENTO' ORDER BY p.data_fim";
        try (Connection conn = DriverManager.getConnection(URL, USUARIO, SENHA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("• " + rs.getString("nome") + " | Fim: " + rs.getString("df") + " | Gerente: " + rs.getString("gerente"));
            }
        } catch (SQLException e) { System.out.println("❌ Erro: " + e.getMessage()); }
    }
}

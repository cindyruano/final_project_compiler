import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int pos;
    private ValidationResult result;

    public SelectStatement parse(List<Token> tokens, ValidationResult result) {
        this.tokens = tokens;
        this.pos = 0;
        this.result = result;
        SelectStatement statement = new SelectStatement();
        expect(TokenType.SELECT, "SYNTACTIC_EXPECTED_SELECT");
        parseColumns(statement);
        expect(TokenType.FROM, "SYNTACTIC_EXPECTED_FROM");
        Token table = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_TABLE");
        if (table != null) statement.table = table.lexeme;

        if (match(TokenType.WHERE)) {
            statement.where = parseWhereChain();
        }

        if (check(TokenType.SEMICOLON)) advance();
        if (!check(TokenType.EOF)) {
            result.diagnostics.add(new Diagnostic("SYNTACTIC_UNEXPECTED_TOKEN", "Token inesperado: " + current().lexeme, current().span));
        }
        return statement;
    }

    private void parseColumns(SelectStatement statement) {
        if (match(TokenType.STAR)) {
            statement.columns.add("*");
            return;
        }
        Token first = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_COLUMN");
        if (first != null) statement.columns.add(first.lexeme);
        while (match(TokenType.COMMA)) {
            Token next = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_COLUMN");
            if (next != null) statement.columns.add(next.lexeme);
        }
    }

    private Token expect(TokenType type, String code) {
        if (check(type)) return advance();
        result.diagnostics.add(new Diagnostic(code, "Se esperaba " + type + " y se encontró " + current().type, current().span));
        return null;
    }

    private boolean match(TokenType type) { if (check(type)) { advance(); return true; } return false; }
    private boolean check(TokenType type) { return current().type == type; }
    private Token current() { return tokens.get(pos); }
    private Token advance() { return tokens.get(pos++); }

    private ConditionChain parseWhereChain() {
        ConditionChain chain = new ConditionChain();
        WhereCondition first = parseWhereCondition();
        if (first != null) {
            chain.conditions.add(first);
        }
        while (match(TokenType.AND) || match(TokenType.OR)) {
            String connector = tokens.get(pos - 1).lexeme.toUpperCase();
            chain.connectors.add(connector);
            WhereCondition next = parseWhereCondition();
            if (next != null) {
                chain.conditions.add(next);
            }
        }
        return chain;
    }

    private WhereCondition parseWhereCondition() {
        Token columnToken = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_WHERE_OPERAND");
        if (columnToken == null) return null;

        Token operatorToken = null;
        if (match(TokenType.EQUAL)) operatorToken = tokens.get(pos - 1);
        else if (match(TokenType.GREATER)) operatorToken = tokens.get(pos - 1);
        else if (match(TokenType.LESS)) operatorToken = tokens.get(pos - 1);
        else if (match(TokenType.GREATER_EQUAL)) operatorToken = tokens.get(pos - 1);
        else if (match(TokenType.LESS_EQUAL)) operatorToken = tokens.get(pos - 1);
        else if (match(TokenType.NOT_EQUAL)) operatorToken = tokens.get(pos - 1);
        else {
            result.diagnostics.add(new Diagnostic("SYNTACTIC_EXPECTED_WHERE_OPERAND", "Se esperaba operador de comparaci\u00f3n", current().span));
            return null;
        }

        Token literalToken = null;
        LiteralType literalType = LiteralType.UNKNOWN;
        if (match(TokenType.NUMBER)) {
            literalToken = tokens.get(pos - 1);
            literalType = LiteralType.NUMBER;
        } else if (match(TokenType.STRING)) {
            literalToken = tokens.get(pos - 1);
            literalType = LiteralType.STRING;
        } else if (match(TokenType.TRUE) || match(TokenType.FALSE)) {
            literalToken = tokens.get(pos - 1);
            literalType = LiteralType.BOOLEAN;
        } else {
            result.diagnostics.add(new Diagnostic("SYNTACTIC_EXPECTED_WHERE_OPERAND", "Se esperaba literal despu\u00e9s del operador", current().span));
            return null;
        }

        return new WhereCondition(
            columnToken.lexeme,
            operatorToken.lexeme,
            literalToken.lexeme,
            literalType,
            columnToken.span,
            operatorToken.span,
            literalToken.span
        );
    }
}

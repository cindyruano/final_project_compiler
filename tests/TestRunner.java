import java.util.List;

public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        run("valid SELECT without WHERE", new TestCase() { public void run() { validSelectWithoutWhere(); }});
        run("unknown projection column", new TestCase() { public void run() { unknownProjectionColumn(); }});
        run("TODO WHERE AST and trace", new TestCase() { public void run() { whereChainAstAndTrace(); }});
        run("TODO WHERE unknown column diagnostic", new TestCase() { public void run() { whereUnknownColumn(); }});
        run("TODO WHERE type mismatch diagnostic", new TestCase() { public void run() { whereTypeMismatch(); }});
        run("TODO WHERE missing operand diagnostic", new TestCase() { public void run() { whereMissingOperand(); }});
        
        System.out.println("\n-------------------------------------------");
        System.out.println("Resultado Oficial -> Passed: " + passed + " Failed: " + failed);
        System.out.println("-------------------------------------------\n");

        // === MIS PRUEBAS PERSONALIZADAS MANUALES ===
        System.out.println("Ejecutando pruebas manuales de laboratorio...");
        miPruebaPersonalizadaValida();
        miPruebaPersonalizadaError();

        if (failed > 0) System.exit(1);
    }

    private static void miPruebaPersonalizadaValida() {
        System.out.println("\n[Prueba Manual 1] Query 100% Válida con WHERE compuesto:");
        String query = "SELECT nombre FROM productos WHERE precio < 500 OR nombre = 'Laptop';";
        System.out.println("Ejecutando: " + query);
        
        ValidationResult r = new CompilerService().validate(query);
        System.out.println("¿Es válida sintáctica y semánticamente?: " + r.valid);
        System.out.println("Trazas generadas por el validador:");
        for (int i = 0; i < r.traces.size(); i++) {
            System.out.println("  -> " + r.traces.get(i));
        }
    }

    private static void miPruebaPersonalizadaError() {
        System.out.println("\n[Prueba Manual 2] Query Inválida (Type Mismatch intencional):");
        String query = "SELECT activo FROM usuarios WHERE activo = 12345;";
        System.out.println("Ejecutando: " + query);
        
        ValidationResult r = new CompilerService().validate(query);
        System.out.println("¿Es válida?: " + r.valid);
        System.out.println("Errores/Diagnósticos detectados:");
        if (r.diagnostics.isEmpty()) {
            System.out.println("  No se detectaron errores.");
        } else {
            for (int i = 0; i < r.diagnostics.size(); i++) {
                System.out.println("  [ERROR] Código: " + r.diagnostics.get(i).code + " | Detalle: " + r.diagnostics.get(i).message);
            }
        }
    }

    private static void validSelectWithoutWhere() {
        ValidationResult r = new CompilerService().validate("SELECT nombre, edad FROM usuarios;");
        assertTrue(r.valid, "Debe ser válido: " + diagnostics(r));
        assertEquals("usuarios", r.ast.table, "Tabla AST");
        assertEquals(2, r.ast.columns.size(), "Cantidad columnas");
    }

    private static void unknownProjectionColumn() {
        ValidationResult r = new CompilerService().validate("SELECT salario FROM usuarios;");
        assertTrue(hasDiagnostic(r, "SEMANTIC_UNKNOWN_COLUMN"), "Debe reportar columna desconocida");
    }

    private static void whereChainAstAndTrace() {
        ValidationResult r = new CompilerService().validate("SELECT nombre, edad FROM usuarios WHERE edad >= 18 AND activo = true;");
        assertTrue(r.valid, "WHERE válido no debe producir errores: " + diagnostics(r));
        assertTrue(r.ast.where != null, "AST debe contener WHERE");
        assertEquals(2, r.ast.where.conditions.size(), "Cantidad de condiciones WHERE");
        assertEquals("AND", r.ast.where.connectors.get(0), "Conector WHERE");
        assertTrue(hasTrace(r, "TRACE|WHERE_TYPE_CHECK|1:41|edad|>=|NUMBER"), "Trace exacto para edad");
        assertTrue(hasTrace(r, "TRACE|WHERE_TYPE_CHECK|1:56|activo|=|BOOLEAN"), "Trace exacto para activo");
    }

    private static void whereUnknownColumn() {
        ValidationResult r = new CompilerService().validate("SELECT nombre FROM usuarios WHERE salario > 100;");
        assertTrue(hasDiagnostic(r, "SEMANTIC_UNKNOWN_WHERE_COLUMN"), "Debe reportar SEMANTIC_UNKNOWN_WHERE_COLUMN");
    }

    private static void whereTypeMismatch() {
        ValidationResult r = new CompilerService().validate("SELECT nombre FROM usuarios WHERE edad = 'mayor';");
        assertTrue(hasDiagnostic(r, "SEMANTIC_TYPE_MISMATCH"), "Debe reportar SEMANTIC_TYPE_MISMATCH");
        assertTrue(hasTrace(r, "TRACE|WHERE_TYPE_CHECK|1:35|edad|=|STRING"), "Debe emitir trace aunque haya mismatch");
    }

    private static void whereMissingOperand() {
        ValidationResult r = new CompilerService().validate("SELECT nombre FROM usuarios WHERE edad >= ;");
        assertTrue(hasDiagnostic(r, "SYNTACTIC_EXPECTED_WHERE_OPERAND"), "Debe reportar operando faltante");
    }

    private static boolean hasDiagnostic(ValidationResult r, String code) {
        for (int i = 0; i < r.diagnostics.size(); i++) if (r.diagnostics.get(i).code.equals(code)) return true;
        return false;
    }

    private static boolean hasTrace(ValidationResult r, String trace) {
        for (int i = 0; i < r.traces.size(); i++) if (r.traces.get(i).equals(trace)) return true;
        return false;
    }

    private static String diagnostics(ValidationResult r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r.diagnostics.size(); i++) sb.append(r.diagnostics.get(i).toString()).append("\n");
        return sb.toString();
    }

    private static void run(String name, TestCase test) {
        try { test.run(); passed++; System.out.println("PASS " + name); }
        catch (Throwable t) { failed++; System.out.println("FAIL " + name + " -> " + t.getMessage()); }
    }

    private static void assertTrue(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
    }
    private interface TestCase { void run(); }
}
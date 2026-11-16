package com.mnight.cppsqript.parser;

import com.mnight.cppsqript.recipe.RecipeScriptManager;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CppScriptLoader {

    public static void loadScriptFile(File scriptFile) {
        try {
            String code = Files.readString(scriptFile.toPath());
            CharStream input = CharStreams.fromString(code);
            CPPRecipeLexer lexer = new CPPRecipeLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CPPRecipeParser parser = new CPPRecipeParser(tokens);

            CPPRecipeParser.ScriptContext tree = parser.script();
            Visitor visitor = new Visitor();
            visitor.visit(tree);

        } catch (IOException e) {
            System.err.println("[CppSqript] Failed to load script: " + scriptFile.getName());
            e.printStackTrace();
        }
    }

    public static int loadAllScripts(Path rootDirectory) {
        if (!Files.exists(rootDirectory)) {
            return 0;
        }

        int loadedCount = 0;
        try (Stream<Path> paths = Files.walk(rootDirectory)) {
            List<Path> cppFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".cpp"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();

            for (Path filePath : cppFiles) {
                System.out.println("[CppSqript] Loading script: " + rootDirectory.relativize(filePath));
                loadScriptFile(filePath.toFile());
                loadedCount++;
            }
        } catch (IOException e) {
            System.err.println("[CppSqript] Error scanning script directory: " + rootDirectory);
            e.printStackTrace();
        }

        return loadedCount;
    }

    private static class Visitor extends CPPRecipeBaseVisitor<Void> {
        // เก็บตัวแปรที่ประกาศในไฟล์สคริปต์ (Symbol Table)
        private final Map<String, Object> variables = new HashMap<>();

        @Override
        public Void visitVarDecl(CPPRecipeParser.VarDeclContext ctx) {
            String varName = ctx.ID().getText();
            Object value = evaluateExpr(ctx.expr());
            variables.put(varName, value);
            return null;
        }

        @Override
        public Void visitRemoveStatement(CPPRecipeParser.RemoveStatementContext ctx) {
            String itemId = resolveString(ctx.expr());
            RecipeScriptManager.removeRecipe(itemId);
            return null;
        }

        @Override
        public Void visitShapelessStatement(CPPRecipeParser.ShapelessStatementContext ctx) {
            String resultId = resolveString(ctx.expr(0));
            int count = resolveInt(ctx.expr(1));

            List<String> inputs = new ArrayList<>();
            for (var exprNode : ctx.exprList().expr()) {
                inputs.add(resolveString(exprNode));
            }

            RecipeScriptManager.addShapelessRecipe(resultId, count, inputs);
            return null;
        }

        @Override
        public Void visitShapedStatement(CPPRecipeParser.ShapedStatementContext ctx) {
            String resultId = resolveString(ctx.expr(0));
            int count = resolveInt(ctx.expr(1));

            List<String> patternRows = new ArrayList<>();
            for (var exprNode : ctx.exprList().expr()) {
                patternRows.add(resolveString(exprNode));
            }

            Map<Character, String> keyMap = new HashMap<>();
            for (var pairCtx : ctx.charMap().charPair()) {
                String rawChar = pairCtx.CHAR().getText();
                char symbol = rawChar.charAt(1);
                String itemId = resolveString(pairCtx.expr());
                keyMap.put(symbol, itemId);
            }

            RecipeScriptManager.addShapedRecipe(resultId, count, patternRows, keyMap);
            return null;
        }

        // ประเมินค่า Expression (แปลง STRING, INT หรือดึงค่าจากตัวแปร ID)
        private Object evaluateExpr(CPPRecipeParser.ExprContext ctx) {
            if (ctx.STRING() != null) {
                return stripQuotes(ctx.STRING().getText());
            } else if (ctx.INT() != null) {
                return Integer.parseInt(ctx.INT().getText());
            } else if (ctx.ID() != null) {
                String varName = ctx.ID().getText();
                if (!variables.containsKey(varName)) {
                    throw new RuntimeException("[CppSqript] Undefined variable: " + varName);
                }
                return variables.get(varName);
            }
            return null;
        }

        private String resolveString(CPPRecipeParser.ExprContext ctx) {
            Object val = evaluateExpr(ctx);
            return String.valueOf(val);
        }

        private int resolveInt(CPPRecipeParser.ExprContext ctx) {
            Object val = evaluateExpr(ctx);
            if (val instanceof Integer i) {
                return i;
            }
            return Integer.parseInt(String.valueOf(val));
        }

        private String stripQuotes(String raw) {
            String unquoted = raw.substring(1, raw.length() - 1);
            return unquoted.replace("\\\"", "\"");
        }
    }
}
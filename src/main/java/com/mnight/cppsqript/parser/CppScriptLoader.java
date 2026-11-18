package com.mnight.cppsqript.parser;

import com.mnight.cppsqript.api.CppSqriptAPI;
import com.mnight.cppsqript.api.ScriptCommandHandler;
import org.antlr.v4.runtime.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class CppScriptLoader {

    public static void loadScriptFile(File scriptFile, String relativeFilePath) {
        try {
            String code = Files.readString(scriptFile.toPath());
            CharStream input = CharStreams.fromString(code);

            CPPRecipeLexer lexer = new CPPRecipeLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CPPRecipeParser parser = new CPPRecipeParser(tokens);

            BaseErrorListener customErrorListener = new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                        int line, int charPositionInLine, String msg, RecognitionException e) {
                    ScriptErrorTracker.reportError(relativeFilePath, line, charPositionInLine, msg);
                }
            };

            lexer.removeErrorListeners();
            lexer.addErrorListener(customErrorListener);
            parser.removeErrorListeners();
            parser.addErrorListener(customErrorListener);

            CPPRecipeParser.ScriptContext tree = parser.script();

            Visitor visitor = new Visitor(relativeFilePath);
            visitor.visit(tree);

        } catch (IOException e) {
            ScriptErrorTracker.reportRuntimeError(relativeFilePath, 0, "Failed to read file: " + e.getMessage());
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
                String relativePath = rootDirectory.relativize(filePath).toString().replace("\\", "/");
                System.out.println("[CppSqript] Loading script: " + relativePath);
                loadScriptFile(filePath.toFile(), relativePath);
                loadedCount++;
            }
        } catch (IOException e) {
            System.err.println("[CppSqript] Error scanning script directory: " + rootDirectory);
            e.printStackTrace();
        }

        return loadedCount;
    }

    private static class Visitor extends CPPRecipeBaseVisitor<Object> {
        private final String relativeFilePath;
        private final Map<String, Object> variables = new HashMap<>();

        public Visitor(String relativeFilePath) {
            this.relativeFilePath = relativeFilePath;
        }

        @Override
        public Object visitVarDecl(CPPRecipeParser.VarDeclContext ctx) {
            try {
                String varName = ctx.ID().getText();
                Object value = visit(ctx.expr());
                variables.put(varName, value);
            } catch (Exception e) {
                ScriptErrorTracker.reportRuntimeError(relativeFilePath, ctx.getStart().getLine(), e.getMessage());
            }
            return null;
        }

        @Override
        public Object visitMethodCall(CPPRecipeParser.MethodCallContext ctx) {
            int line = ctx.getStart().getLine();
            try {
                // If called as "event.command(...)", ID(0) is "event" and ID(1) is "command".
                // If called globally as "command(...)", ID(0) is "command".
                List<org.antlr.v4.runtime.tree.TerminalNode> ids = ctx.ID();
                String commandName = (ids.size() == 2) ? ids.get(1).getText() : ids.get(0).getText();

                ScriptCommandHandler handler = CppSqriptAPI.getCommand(commandName);
                if (handler == null) {
                    ScriptErrorTracker.reportRuntimeError(relativeFilePath, line,
                            "Unknown command '" + commandName + "'. Check if a required addon is missing.");
                    return null;
                }

                // Evaluate all arguments into Java Objects
                List<Object> evaluatedArgs = new ArrayList<>();
                if (ctx.argList() != null) {
                    for (CPPRecipeParser.ExprContext exprCtx : ctx.argList().expr()) {
                        evaluatedArgs.add(visit(exprCtx));
                    }
                }

                // Execute the registered command handler (built-in or addon)
                handler.execute(evaluatedArgs);

            } catch (IndexOutOfBoundsException e) {
                ScriptErrorTracker.reportRuntimeError(relativeFilePath, line,
                        "Insufficient arguments provided for command call.");
            } catch (ClassCastException e) {
                ScriptErrorTracker.reportRuntimeError(relativeFilePath, line,
                        "Invalid argument type: " + e.getMessage());
            } catch (Exception e) {
                ScriptErrorTracker.reportRuntimeError(relativeFilePath, line,
                        "Command execution failed: " + e.getMessage());
            }
            return null;
        }

        // --- Expression Evaluators (Labeled Alternatives from CPPRecipe.g4) ---

        @Override
        public Object visitStringExpr(CPPRecipeParser.StringExprContext ctx) {
            return stripQuotes(ctx.STRING().getText());
        }

        @Override
        public Object visitIntExpr(CPPRecipeParser.IntExprContext ctx) {
            return Integer.parseInt(ctx.INT().getText());
        }

        @Override
        public Object visitFloatExpr(CPPRecipeParser.FloatExprContext ctx) {
            String raw = ctx.FLOAT().getText();
            if (raw.endsWith("f") || raw.endsWith("F")) {
                raw = raw.substring(0, raw.length() - 1);
            }
            return Float.parseFloat(raw);
        }

        @Override
        public Object visitCharExpr(CPPRecipeParser.CharExprContext ctx) {
            return ctx.CHAR().getText().charAt(1);
        }

        @Override
        public Object visitVarExpr(CPPRecipeParser.VarExprContext ctx) {
            String varName = ctx.ID().getText();
            if (!variables.containsKey(varName)) {
                throw new RuntimeException("Undefined variable '" + varName + "'");
            }
            return variables.get(varName);
        }

        @Override
        public Object visitArrayExpr(CPPRecipeParser.ArrayExprContext ctx) {
            List<Object> list = new ArrayList<>();
            for (CPPRecipeParser.ExprContext exprCtx : ctx.expr()) {
                list.add(visit(exprCtx));
            }
            return list;
        }

        @Override
        public Object visitPairExpr(CPPRecipeParser.PairExprContext ctx) {
            char key = ctx.CHAR().getText().charAt(1);
            Object value = visit(ctx.expr());
            return Map.entry(key, value);
        }

        @Override
        public Object visitRawStringExpr(CPPRecipeParser.RawStringExprContext ctx) {
            String raw = ctx.RAW_STRING().getText();
            // ตัด 'R"(' (3 ตัวแรก) และ ')"' (2 ตัวท้าย) ออก
            return raw.substring(3, raw.length() - 2);
        }

        private String stripQuotes(String raw) {
            String unquoted = raw.substring(1, raw.length() - 1);
            return unquoted.replace("\\\"", "\"");
        }
    }
}
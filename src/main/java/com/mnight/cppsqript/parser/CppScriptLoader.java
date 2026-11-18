package com.mnight.cppsqript.parser;

import com.mnight.cppsqript.recipe.RecipeScriptManager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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

    private static class Visitor extends CPPRecipeBaseVisitor<Void> {
        private final String relativeFilePath;
        private final Map<String, Object> variables = new HashMap<>();

        public Visitor(String relativeFilePath) {
            this.relativeFilePath = relativeFilePath;
        }

        @Override
        public Void visitVarDecl(CPPRecipeParser.VarDeclContext ctx) {
            try {
                String varName = ctx.ID().getText();
                Object value = evaluateExpr(ctx.expr());
                variables.put(varName, value);
            } catch (Exception e) {
                ScriptErrorTracker.reportRuntimeError(relativeFilePath, ctx.getStart().getLine(), e.getMessage());
            }
            return null;
        }

        @Override
        public Void visitRemoveStatement(CPPRecipeParser.RemoveStatementContext ctx) {
            try {
                String itemId = resolveString(ctx.expr());
                RecipeScriptManager.removeRecipe(itemId);
            } catch (Exception e) {
                ScriptErrorTracker.reportRuntimeError(relativeFilePath, ctx.getStart().getLine(), e.getMessage());
            }
            return null;
        }

        @Override
        public Void visitShapelessStatement(CPPRecipeParser.ShapelessStatementContext ctx) {
            try {
                String resultId = resolveString(ctx.expr(0));
                int count = resolveInt(ctx.expr(1));

                List<String> inputs = new ArrayList<>();
                for (var exprNode : ctx.exprList().expr()) {
                    inputs.add(resolveString(exprNode));
                }

                RecipeScriptManager.addShapelessRecipe(resultId, count, inputs);
            } catch (Exception e) {
                ScriptErrorTracker.reportRuntimeError(relativeFilePath, ctx.getStart().getLine(), e.getMessage());
            }
            return null;
        }

        @Override
        public Void visitShapedStatement(CPPRecipeParser.ShapedStatementContext ctx) {
            try {
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
            } catch (Exception e) {
                ScriptErrorTracker.reportRuntimeError(relativeFilePath, ctx.getStart().getLine(), e.getMessage());
            }
            return null;
        }

        private Object evaluateExpr(CPPRecipeParser.ExprContext ctx) {
            if (ctx.STRING() != null) {
                return stripQuotes(ctx.STRING().getText());
            } else if (ctx.INT() != null) {
                return Integer.parseInt(ctx.INT().getText());
            } else if (ctx.ID() != null) {
                String varName = ctx.ID().getText();
                if (!variables.containsKey(varName)) {
                    throw new RuntimeException("Undefined variable '" + varName + "'");
                }
                return variables.get(varName);
            }
            return null;
        }

        private String resolveString(CPPRecipeParser.ExprContext ctx) {
            return String.valueOf(evaluateExpr(ctx));
        }

        private int resolveInt(CPPRecipeParser.ExprContext ctx) {
            Object val = evaluateExpr(ctx);
            if (val instanceof Integer i) {
                return i;
            }
            try {
                return Integer.parseInt(String.valueOf(val));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Value '" + val + "' is not a valid integer (Expected INT)");
            }
        }

        private String stripQuotes(String raw) {
            String unquoted = raw.substring(1, raw.length() - 1);
            return unquoted.replace("\\\"", "\"");
        }
    }

    public static Item validateAndGetItem(String itemId, String context){
        try {
            ResourceLocation loc = ResourceLocation.parse(itemId);
            if (!BuiltInRegistries.ITEM.containsKey(loc)) {
                ScriptErrorTracker.reportWarning(context, "Unknown item ID '" + itemId + "' (item does not exist in registry).");
                return null;
            }
            Item item = BuiltInRegistries.ITEM.get(loc);
            if (item == Items.AIR) {
                ScriptErrorTracker.reportWarning(context, "Item '" + itemId + "' resolved to minecraft:air.");
                return null;
            }
            return item;
        } catch (Exception e) {
            ScriptErrorTracker.reportWarning(context, "Invalid ResourceLocation format '" + itemId + "': " + e.getMessage());
            return null;
        }
    }
}
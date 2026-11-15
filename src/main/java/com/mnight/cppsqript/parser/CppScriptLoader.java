package com.mnight.cppsqript.parser;

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

import com.mnight.cppsqript.recipe.RecipeScriptManager;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class CppScriptLoader {
    public static void loadScriptFile(File scriptFile){
        try{
            String code = Files.readString(scriptFile.toPath());
            CharStream input = CharStreams.fromString(code);
            CPPRecipeLexer laxer = new CPPRecipeLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(laxer);
            CPPRecipeParser parser = new CPPRecipeParser(tokens);

            CPPRecipeParser.ScriptContext tree = parser.script();
            Visitor visitor = new Visitor();
            visitor.visit(tree);
        } catch (IOException e){
            System.err.println("[CppSqript] Failed to load script: " + scriptFile.getName());
            e.printStackTrace();
        }
    }

    public static int loadAllScripts(Path rootDirectory){
        if (!Files.exists(rootDirectory)) {
            return 0;
        }
        
        int loadedCount = 0;
        try (Stream<Path> paths = Files.walk(rootDirectory)){
            List<Path> cppFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".cpp"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();

            for (Path filePath : cppFiles){
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

    private static class Visitor extends CPPRecipeBaseVisitor<Void>{

        @Override 
        public Void visitRemoveStatement(CPPRecipeParser.RemoveStatementContext ctx){
            String rawItem = ctx.STRING().getText();
            String itemId = stripQuotes(rawItem);

            RecipeScriptManager.removeRecipe(itemId);
            return null;
        }

        @Override 
        public Void visitShapelessStatement(CPPRecipeParser.ShapelessStatementContext ctx){
            String resultId = stripQuotes(ctx.STRING().getText());
            int count = Integer.parseInt(ctx.INT().getText());

            List<String> inputs = new ArrayList<>();
            for (var strNode : ctx.stringList().STRING()){
                inputs.add(stripQuotes(strNode.getText()));
            }

            RecipeScriptManager.addShapelessRecipe(resultId, count, inputs);
            return null;
        }

        @Override 
        public Void visitShapedStatement(CPPRecipeParser.ShapedStatementContext ctx){
            String resultId = stripQuotes(ctx.STRING().getText());
            int count = Integer.parseInt(ctx.INT().getText());

            List<String> pattenRows = new ArrayList<>();
            for(var strNode : ctx.stringList().STRING()){
                pattenRows.add(stripQuotes(strNode.getText()));
            }

            Map<Character, String> keyMap = new HashMap<>();
            for(var pairCtx : ctx.charMap().charPair()){
                String rawChar = pairCtx.CHAR().getText();
                char symbol = rawChar.charAt(1);
                String itemId = stripQuotes(pairCtx.STRING().getText());
                keyMap.put(symbol, itemId);
            }

            RecipeScriptManager.addShapedRecipe(resultId, count, pattenRows, keyMap);
            return null;
        }

        private String stripQuotes(String raw) {
            String unquoted = raw.substring(1, raw.length() - 1);
            return unquoted.replace("\\\"", "\"");
        }
    }
}

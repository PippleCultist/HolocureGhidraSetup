import java.util.*;
import java.util.regex.*;
import java.io.*;
public class PostProcess
{
	static PrintWriter out;
	static HashSet<String> removeLocalVarSet;
	public static void main(String args[]) throws Exception
	{
		out = new PrintWriter(new FileWriter(new File("output.txt")));
		BufferedReader f = new BufferedReader(new FileReader("input.txt"));
		String programText = "";
		while (f.ready())
		{
			programText += f.readLine() + "\n";
		}
		removeLocalVarSet = new HashSet<String>();
		programText = cleanupCopyRValue(programText);
		programText = cleanupFreeRValue(programText);
		programText = cleanupArrayAccess(programText);
		programText = cleanupGlobalVars(programText);
		programText = cleanupUnusedLocalVars(programText);
		out.println(programText);
		out.close();
	}

	public static String cleanupFreeRValue(String input)
	{
		Pattern localVariablePattern = Pattern.compile("local_[0-9a-f]+");
		Pattern freeRValuePattern = Pattern.compile("[ \t]*if \\(\\(1 << \\(\\(byte\\)\\w+ & 0x1f\\) & 0x46U\\) != 0\\) \\{\\s*FREE_RValue__Pre\\(&\\w+\\);\\s*\\}\n");
		Matcher programTextMatcher = freeRValuePattern.matcher(input);
		
		while (programTextMatcher.find())
		{
			String match = programTextMatcher.group();
			Matcher localMatcher = localVariablePattern.matcher(match);
			localMatcher.find();
			removeLocalVarSet.add(localMatcher.group());
		}
		return programTextMatcher.replaceAll("");
	}

	public static String cleanupArrayAccess(String input)
	{
		Pattern arrayAccessPattern = Pattern.compile("if \\(\\(\\(\\*\\(uint \\*\\)\\(.*? \\+ 0xc\\) & 0xffffff\\) == 2\\) && \\(\\*\\w+ != 0\\)\\) \\{.*?YYError\\(\"trying.*?\\}\n", Pattern.DOTALL);
		Pattern arrayGetPattern = Pattern.compile("\\w+ = .*?Array_GetEntry.*?;\n");
		String result = input;
		Matcher arrayAccessMatcher = arrayAccessPattern.matcher(result);
		while (arrayAccessMatcher.find())
		{
			String match = arrayAccessMatcher.group();
			Matcher arrayGetMatcher = arrayGetPattern.matcher(match);
			arrayGetMatcher.find();
			result = arrayAccessMatcher.replaceFirst(arrayGetMatcher.group());
			arrayAccessMatcher = arrayAccessPattern.matcher(result);
		}
		return result;
	}

	public static String cleanupCopyRValue(String input)
	{
		Pattern copyRValuePattern = Pattern.compile("local.*?;\n.*?local.*?;\n\\s+if \\(\\(1 << .*?& 0x46U\\) == 0(?s).*?local\\w+ = .*?COPY_RValue_do__Post.*?(?-s)\\}\n");
		Pattern localVariablePattern = Pattern.compile("local\\w+ = .*?;\n");
		String result = input;
		Matcher copyRValueMatcher = copyRValuePattern.matcher(result);
		while (copyRValueMatcher.find())
		{
			String match = copyRValueMatcher.group();
			Matcher localVariableMatcher = localVariablePattern.matcher(match);
			localVariableMatcher.find();
			removeLocalVarSet.add(localVariableMatcher.group());
			localVariableMatcher.find();
			removeLocalVarSet.add(localVariableMatcher.group());
			localVariableMatcher.find();
			result = copyRValueMatcher.replaceFirst(localVariableMatcher.group());
			copyRValueMatcher = copyRValuePattern.matcher(result);
		}
		return result;
	}

	public static String cleanupGlobalVars(String input)
	{
		String result = input;
		Pattern globalVariablePattern = Pattern.compile("(\\(.*?\\))?\\(\\*\\*\\(code \\*\\*\\)\\(\\*global \\+ 8\\)\\).*?;");
		Matcher globalVariableMatcher = globalVariablePattern.matcher(result);
		while (globalVariableMatcher.find())
		{
			String match = globalVariableMatcher.group();
			// out.println(match);
			Pattern variableNamePattern = Pattern.compile("(?<=\\(global,)\\w+(?=\\))");
			Matcher variableNameMatcher = variableNamePattern.matcher(match);
			variableNameMatcher.find();
			result = globalVariableMatcher.replaceFirst("global." + variableNameMatcher.group());
			globalVariableMatcher = globalVariablePattern.matcher(result);
		}
		return result;
	}

	public static String cleanupUnusedLocalVars(String input)
	{
		String result = input;
		for (String localVar : removeLocalVarSet)
		{
			Pattern removeVarAssignPattern = Pattern.compile("[ \t]*" + localVar + " = \\w+;\n");
			Matcher removeVarAssignMatcher = removeVarAssignPattern.matcher(result);
			result = removeVarAssignMatcher.replaceAll("");
			Pattern removeVarDeclarePattern = Pattern.compile("[ \t]*undefined. " + localVar + ";\n");
			Matcher removeVarDeclareMatcher = removeVarDeclarePattern.matcher(result);
			result = removeVarDeclareMatcher.replaceAll("");
		}
		return result;
	}
}
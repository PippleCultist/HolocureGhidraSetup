import java.util.*;
import java.util.regex.*;
import java.io.*;
public class SimplifyCode
{
	public static void main(String args[]) throws Exception
	{
		PrintWriter out = new PrintWriter(new FileWriter(new File("output.txt")));
		BufferedReader f = new BufferedReader(new FileReader("input.txt"));
		ArrayList<String> codeText = new ArrayList<String>();
		while (f.ready())
		{
			String curInstructionText = f.readLine();
			if (curInstructionText.matches("\\s*/\\*.*\\*/")) // comment
			{
				continue;
			}
			codeText.add(curInstructionText);
		}
		CodeBlock curFunction = new CodeBlock();
		out.println("lines parsed: " + curFunction.parseCode(codeText, 0));
		recurseInstructions(curFunction);
		out.println(curFunction);
		out.close();
	}

	public static void recurseInstructions(CodeBlock curFunction)
	{
		for (int i = curFunction.instructionList.size() - 1; i >= 0; i--)
		{
			Instruction curInstruction = curFunction.instructionList.get(i);
			switch (curInstruction.instructionType)
			{
				case IF:
				{
					CodeBlock codeBlockOne = curInstruction.codeBlockOne;
					boolean isInstructionModified = false;
					if (codeBlockOne.instructionList.size() == 1)
					{
						Instruction codeBlockOneInstruction = codeBlockOne.instructionList.get(0);
						if (codeBlockOneInstruction.instructionText.matches("\\s*FREE_RValue__Pre.*;"))
						{
							curFunction.instructionList.remove(i);
							isInstructionModified = true;
						}
					}
					if (!isInstructionModified)
					{
						recurseInstructions(curInstruction.codeBlockOne);
					}
					break;
				}
				case IFELSE:
				{
					CodeBlock codeBlockOne = curInstruction.codeBlockOne;
					CodeBlock codeBlockTwo = curInstruction.codeBlockTwo;
					boolean isInstructionModified = false;
					if (codeBlockOne.instructionList.size() == 1 && codeBlockTwo.instructionList.size() == 1)
					{
						Instruction codeBlockTwoInstruction = codeBlockTwo.instructionList.get(0);
						if (codeBlockTwoInstruction.instructionText.matches("\\s*COPY_RValue_do__Post.*;"))
						{
							Instruction setInstruction = codeBlockOne.instructionList.get(0);
							setInstruction.instructionText = setInstruction.instructionText.substring(2);
							curFunction.instructionList.set(i, setInstruction);
							isInstructionModified = true;
						}
					}
					if (codeBlockOne.instructionList.size() == 3)
					{
						Instruction codeBlockTwoInstruction = codeBlockTwo.instructionList.get(0);
						if (codeBlockTwoInstruction.instructionText.matches("\\s*YYError\\(\"trying to index variable that is not an array\"\\);"))
						{
							Instruction arrayBoundCheckIfInstruction = codeBlockOne.instructionList.get(2);
							CodeBlock arrayBoundCheckIfCodeBlockTwo = arrayBoundCheckIfInstruction.codeBlockTwo;
							Instruction arrayBoundCheckIfArrayGetInstruction = arrayBoundCheckIfCodeBlockTwo.instructionList.get(0);
							arrayBoundCheckIfArrayGetInstruction.instructionText = arrayBoundCheckIfArrayGetInstruction.instructionText.substring(4);
							curFunction.instructionList.set(i, arrayBoundCheckIfArrayGetInstruction);
							isInstructionModified = true;
						}
					}
					if (!isInstructionModified)
					{
						recurseInstructions(curInstruction.codeBlockOne);
						recurseInstructions(curInstruction.codeBlockTwo);
					}
					break;
				}
				case WHILE:
				{
					recurseInstructions(curInstruction.codeBlockOne);
					break;
				}
			}
		}
	}
}

class CodeBlock
{
	String functionName;
	HashSet<String> localVarNames;
	ArrayList<Instruction> instructionList;

	public CodeBlock()
	{
		functionName = "";
		instructionList = new ArrayList<Instruction>();
	}

	public int parseCode(ArrayList<String> codeText, int curCodePos) throws Exception
	{
		if (curCodePos == 0)
		{
			localVarNames = new HashSet<String>();
			curCodePos++;
			functionName = codeText.get(curCodePos++);
			char lastChar = functionName.charAt(functionName.length() - 1); 
			while (lastChar != ')')
			{
				functionName += "\n" + codeText.get(curCodePos++);
				lastChar = functionName.charAt(functionName.length() - 1); 
			}
			curCodePos += 2;
			ArrayList<String> localVarNames = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(codeText.get(curCodePos++), " *;");
			while (st.hasMoreTokens())
			{
				st.nextToken();
				localVarNames.add(st.nextToken());
				st = new StringTokenizer(codeText.get(curCodePos++), " *;");
			}
		}
		
		while (curCodePos < codeText.size())
		{
			String instructionText = codeText.get(curCodePos);
			if (instructionText.matches("\\s*}"))
			{
				curCodePos++;
				break;
			}
			Instruction curInstruction = new Instruction();
			curCodePos = curInstruction.parseCode(codeText, curCodePos);
			instructionList.add(curInstruction);
		}
		return curCodePos;
	}

	public int parseWhile(ArrayList<String> codeText, int curCodePos) throws Exception
	{
		while (curCodePos < codeText.size())
		{
			String instructionText = codeText.get(curCodePos);
			if (instructionText.matches("\\s*} while.*;"))
			{
				curCodePos++;
				return curCodePos;
			}
			Instruction curInstruction = new Instruction();
			curCodePos = curInstruction.parseCode(codeText, curCodePos);
			instructionList.add(curInstruction);
		}
		throw new Exception("DIDN'T PARSE END OF WHILE");
	}

	public String toString()
	{
		return formatCode(0);
	}

	public String formatCode(int indentLevel)
	{
		String result = "";
		if (!functionName.isEmpty())
		{
			result += functionName + "\n";
		}
		String strIndent = "";
		for (int i = 0; i < indentLevel; i++)
		{
			strIndent += "  ";
		}
		if (indentLevel == 0)
		{
			result += strIndent + "{\n";
		}
		for (int i = 0; i < instructionList.size(); i++)
		{
			result += instructionList.get(i).formatCode(indentLevel + 1);
		}
		result += strIndent + "}\n";
		return result;
	}
}

class Instruction
{
	enum InstructionType
	{
		STATEMENT,
		IF,
		IFELSE,
		WHILE,
		LABEL
	}
	String instructionText;
	CodeBlock codeBlockOne;
	CodeBlock codeBlockTwo;
	InstructionType instructionType;
	public int parseCode(ArrayList<String> codeText, int curCodePos) throws Exception
	{
		String curInstructionText = codeText.get(curCodePos++);
		char lastChar = curInstructionText.charAt(curInstructionText.length() - 1);
		// If the decompiler put the instruction on multiple lines, combine it together
		while (lastChar != ';' && lastChar != '{' && lastChar != ':')
		{
			curInstructionText += "\n" + codeText.get(curCodePos++);
			lastChar = curInstructionText.charAt(curInstructionText.length() - 1);
		}
		if (lastChar == ';')
		{
			instructionText = curInstructionText;
			instructionType = InstructionType.STATEMENT;
		}
		else if (lastChar == '{')
		{
			if (curInstructionText.matches("\\s*if.*\\{"))
			{
				instructionText = curInstructionText;
				codeBlockOne = new CodeBlock();	
				curCodePos = codeBlockOne.parseCode(codeText, curCodePos);
				instructionType = InstructionType.IF;
				if (codeText.get(curCodePos).matches("\\s*else \\{"))
				{
					curCodePos++;
					codeBlockTwo = new CodeBlock();
					curCodePos = codeBlockTwo.parseCode(codeText, curCodePos);
					instructionType = InstructionType.IFELSE;
				}
			}
			else if (curInstructionText.matches("\\s*do \\{"))
			{
				codeBlockOne = new CodeBlock();	
				int newCodePos = codeBlockOne.parseWhile(codeText, curCodePos);
				instructionText = codeText.get(newCodePos - 1).replaceFirst("} ", "");
				curCodePos = newCodePos;
				instructionType = InstructionType.WHILE;
			}
			else
			{
				throw new Exception("UNKNOWN CODE BLOCK START ERROR");
			}
		}
		else if (lastChar == ':')
		{
			instructionText = curInstructionText;
			instructionType = InstructionType.LABEL;
		}
		else
		{
			throw new Exception("UNKNOWN INSTRUCTION TYPE ERROR");
		}
		return curCodePos;
	}

	public String formatCode(int indentLevel)
	{
		String result = "";
		switch (instructionType)
		{
			case STATEMENT:
			case LABEL:
			{
				result += instructionText + "\n";
				break;
			}
			case IF:
			{
				result += instructionText + "\n";
				result += codeBlockOne.formatCode(indentLevel);
				break;
			}
			case IFELSE:
			{
				result += instructionText + "\n";
				result += codeBlockOne.formatCode(indentLevel);
				for (int i = 0; i < indentLevel; i++)
				{
					result += "  ";
				}
				result += "else {\n";
				result += codeBlockTwo.formatCode(indentLevel);
				break;
			}
			case WHILE:
			{
				for (int i = 0; i < indentLevel; i++)
				{
					result += "  ";
				}
				result += "do {\n";
				result += codeBlockOne.formatCode(indentLevel);
				result += instructionText + "\n";
				break;
			}
		}

		return result;
	}
}
//TODO write a description for this script
//@author 
//@category Functions
//@keybinding 
//@menupath 
//@toolbar 

import java.io.*;
import java.util.*;
import java.util.regex.*;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.mem.*;
import ghidra.program.model.lang.*;
import ghidra.program.model.pcode.*;
import ghidra.program.model.util.*;
import ghidra.program.model.reloc.*;
import ghidra.program.model.data.*;
import ghidra.program.model.block.*;
import ghidra.program.model.symbol.*;
import ghidra.program.model.scalar.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;

public class HolocureSetup extends GhidraScript {

    public void run() throws Exception {
		Listing listing = currentProgram.getListing();
//		File outputFile = askFile("Please Select Output File", "Choose");
//		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
		FunctionIterator iter = listing.getFunctions(true);
		int count = 0;
		while (iter.hasNext() && !monitor.isCancelled()) {
			Function f = iter.next();
			Instruction curInstruction = getFirstInstruction(f);
			if (f.getName().equals("InitJavaScriptFunctions"))
			{
				Address prevAddress = null;
				for (int i = 0; i < 100; i++)
				{
					byte[] parsedBytesArr = curInstruction.getParsedBytes();
					if (parsedBytesArr[0] == (byte)0x48 && parsedBytesArr[1] == (byte)0x8d && parsedBytesArr.length == 7)
					{
						Address curAddress = curInstruction.getAddress();
						int offset = (int)((parsedBytesArr[3] & 0xFFL) | ((parsedBytesArr[4] & 0xFFL) << 8) | ((parsedBytesArr[5] & 0xFFL) << 16) | ((parsedBytesArr[6] & 0xFFL) << 24));
						Address newAddress = curAddress.add(offset + 7);
						Data curData = getDataAt(newAddress);
						if (curData != null && curData.hasStringValue())
						{
							String name = (String)curData.getValue();
							if (name.equals("@@Global@@"))
							{
								Instruction setGlobalInstruction = getInstructionAt(prevAddress.add(7));
								parsedBytesArr = setGlobalInstruction.getParsedBytes();
								int globalOffset = (int)((parsedBytesArr[3] & 0xFFL) | ((parsedBytesArr[4] & 0xFFL) << 8) | ((parsedBytesArr[5] & 0xFFL) << 16) | ((parsedBytesArr[6] & 0xFFL) << 24));
								Symbol globalSymbol = getSymbolAt(prevAddress.add(7 + globalOffset + 7));
								globalSymbol.setName("global", SourceType.USER_DEFINED);
							}
						}
						prevAddress = newAddress;
					}
					curInstruction = curInstruction.getNext();
				}
				
				continue;
			}
			for (int i = 0; i < 40; i++)
			{
				byte[] parsedBytesArr = curInstruction.getParsedBytes();
				if (parsedBytesArr[0] == (byte)0x48 && parsedBytesArr[1] == (byte)0x8d && parsedBytesArr.length >= 7)
				{
					curInstruction = curInstruction.getNext();
					byte[] nextParsedBytesArr = curInstruction.getParsedBytes();
					if (nextParsedBytesArr[0] == (byte)0x48 && nextParsedBytesArr[1] == (byte)0x89)
					{
						Address curAddress = curInstruction.getAddress();
						int offset = (int)((parsedBytesArr[3] & 0xFFL) | ((parsedBytesArr[4] & 0xFFL) << 8) | ((parsedBytesArr[5] & 0xFFL) << 16) | ((parsedBytesArr[6] & 0xFFL) << 24));
						Data curData = getDataAt(curAddress.getNewAddress(offset + curAddress.getOffset()));
						if (curData != null)
						{
							if (curData.hasStringValue())
							{
								String name = (String)curData.getValue();
								if (name.startsWith("gml_"))
								{
									f.setName(name, SourceType.USER_DEFINED);
									if (name.startsWith("gml_Object"))
									{
										Parameter[] paramArr = f.getParameters();
										if (paramArr.length >= 2)
										{
											paramArr[0].setName("Self", SourceType.USER_DEFINED);
											paramArr[1].setName("Other", SourceType.USER_DEFINED);
										}
									}
									else if (name.startsWith("gml_Script"))
									{
										Parameter[] paramArr = f.getParameters();
										if (paramArr.length >= 2)
										{
											paramArr[0].setName("Self", SourceType.USER_DEFINED);
											paramArr[1].setName("Other", SourceType.USER_DEFINED);
										}
										if (paramArr.length >= 3)
										{
											paramArr[2].setName("ReturnValue", SourceType.USER_DEFINED);
										}
										if (paramArr.length >= 5)
										{
											paramArr[3].setName("NumArgs", SourceType.USER_DEFINED);
											paramArr[4].setName("Args", SourceType.USER_DEFINED);
										}
									}
//									out.println(f.getParameterCount() + " " + name);
								}
							}
						}
						break;
					}
				}
				else
				{
					curInstruction = curInstruction.getNext();
				}
			}
			count++;
		}

//		out.println(currentProgram.getMinAddress().getOffset() + " " + currentProgram.getMaxAddress().getOffset());
		Pattern identifierPattern = Pattern.compile("[a-zA-Z_0-9]*");
		SymbolIterator symbolIter = currentProgram.getSymbolTable().getAllSymbols(true);
		int symbolCount = 0;
		while (symbolIter.hasNext() && !monitor.isCancelled()) {
			Symbol s = symbolIter.next();
			Address startAddr = s.getAddress();
			long startAddrOffset = startAddr.getOffset();
			if (startAddrOffset >= currentProgram.getMinAddress().getOffset() && startAddrOffset < currentProgram.getMaxAddress().getOffset())
			{
				try
				{
					byte[] byteArr = getBytes(startAddr, 4);
					if (byteArr[0] == (byte)0xFF && byteArr[1] == (byte)0xFF && byteArr[2] == (byte)0xFF && byteArr[3] == (byte)0xFF)
					{
						Data curData = getDataAt(startAddr.subtract(8));
						if (curData != null && curData.isPointer())
						{
							Address ptrAddress = startAddr.getAddress(curData.getValue().toString());
							Data ptrData = getDataAt(ptrAddress);
							boolean isPtrDataNull = ptrData == null;
							if (isPtrDataNull)
							{
								ptrData = createAsciiString(ptrAddress);
							}
							if (ptrData.hasStringValue())
							{
								String name = (String)ptrData.getValue();
								if (identifierPattern.matcher(name).matches())
								{
									s.setName(name, SourceType.USER_DEFINED);
								}
							}
						}
						symbolCount++;
					}
				}
				catch (MemoryAccessException e)
				{

				}
			}
		}
//		out.close();
    }

}

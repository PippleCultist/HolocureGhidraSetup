//TODO write a description for this script
//@author 
//@category Functions
//@keybinding 
//@menupath 
//@toolbar 

import java.io.*;
import java.util.*;
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
						long offset = (parsedBytesArr[3] & 0xFFL) + ((parsedBytesArr[4] & 0xFFL) << 8) + ((parsedBytesArr[5] & 0xFFL) << 16) + ((parsedBytesArr[6] & 0xFFL) << 24);
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

		SymbolIterator symbolIter = currentProgram.getSymbolTable().getAllSymbols(true);
		int symbolCount = 0;
		while (symbolIter.hasNext() && !monitor.isCancelled()) {
			Symbol s = symbolIter.next();
			Address startAddr = s.getAddress();
			if (startAddr.getOffset() > 8)
			{
				Data curData = getDataAt(startAddr.subtract(8));
				if (curData != null && curData.isPointer())
				{
					Data ptrData = getDataAt(startAddr.getAddress(curData.getValue().toString()));
					if (ptrData != null && ptrData.hasStringValue())
					{
						String name = (String)ptrData.getValue();
						if (!name.contains(" "))
						{
							s.setName(name, SourceType.USER_DEFINED);
						}
					}
				}
				symbolCount++;
			}
		}
//		out.println(symbolCount);
//		out.close();
    }

}

package mx.com.nokia.testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.csvreader.CsvWriter;

/**
 *
 * @author Adriana
 */
public class SearchMissingRR {
	public static void main(String[] args) throws IOException {
		String dirpath;
		String dirpathExcel;
		String outputFile;
		if (args.length != 0) {
			dirpath = !args[0].equals("") ? args[0]
					: "C:\\Users\\adrivera\\BRFixedMobile3\\BRFixedMobile\\Configuration\\workflow";
			dirpathExcel = args[1] != null ? args[1]
					: "C:\\Users\\adrivera\\BRFixedMobile3\\BRFixedMobile\\Input\\Book2.xls";
			outputFile = args[2] != null ? args[2]
					: "C:\\Users\\adrivera\\BRFixedMobile3\\BRFixedMobile\\Output\\MissingRR3.csv";
		} else {
			dirpath = "C:\\Users\\adrivera\\BRFixedMobile3\\BRFixedMobile\\Configuration\\workflow";
			dirpathExcel = "C:\\Users\\adrivera\\BRFixedMobile3\\BRFixedMobile\\Input\\Book2.xls";
			outputFile = "C:\\Users\\adrivera\\BRFixedMobile3\\BRFixedMobile\\Output\\MissingRR3.csv";

		}
		System.out.println("Paths: mwf: " + dirpath + " |excel : " + dirpathExcel + " |Output file: " + outputFile);

		/*
		 * JSON vars
		 */
		File f = new File(dirpath);

		Object obj;
		JSONObject jo;
		JSONObject workflow_contents;
		JSONArray childShapes;
		JSONObject properties;
		JSONObject properties2;
		String signalcfg;
		String testmodule;
		JSONArray signals;
		JSONObject elements;
		String rrcodes;
		String file;
		ArrayList <Resolution> resolutions = new ArrayList();

		/*
		 * Excel vars
		 */

		File excelFile = new File(dirpathExcel);
		FileInputStream fis = new FileInputStream(excelFile);
		Map resolutionsFile = Collections.synchronizedMap(new HashMap());

		/*
		 * Output file vars
		 */

		boolean alreadyExists = new File(outputFile).exists();

		/*
		 * .mwf analysis
		 */
		if (f.exists()) {
			String arr[] = f.list();

			int n = arr.length;
			for (int i = 0; i < n; i++) {
				file = f.list()[i];
				System.out.println("Analizing file: " + file);
				File f1 = new File(f, f.list()[i]);
				try {
					obj = new JSONParser().parse(new FileReader(f1));
					jo = (JSONObject) obj;
					workflow_contents = (JSONObject) jo.get("workflow_contents");
					childShapes = (JSONArray) workflow_contents.get("childShapes");

					for (int cont = 0; cont < childShapes.size(); cont++) {
						properties = (JSONObject) childShapes.get(cont);
						properties2 = (JSONObject) properties.get("properties");

						if (properties2.containsKey("signalcfg") && !properties2.get("signalcfg").equals("")) {
							signalcfg = (String) properties2.get("signalcfg");

							Object signalcfgObj = new JSONParser().parse(signalcfg);
							JSONObject signalcfgJSONObj = (JSONObject) signalcfgObj;
							signals = (JSONArray) signalcfgJSONObj.get("signals");
							for (int cont2 = 0; cont2 < signals.size(); cont2++) {
								elements = (JSONObject) signals.get(cont2);
								if (elements.containsKey("rrcodes")) {
									rrcodes = (String) elements.get("rrcodes");
									testmodule = (String) elements.get("testModule");
									StringTokenizer aux = new StringTokenizer(rrcodes, "resolution.");
									while (aux.hasMoreElements()) {
										String rKey = ((String) aux.nextElement()).replaceAll(",$", "");
										resolutions.add(new Resolution(rKey, file));
//										mwfFileNames.add(file);
									}
								}
							}
						}
					}
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("Directory not found");
		}

		/*
		 * Excel Analysis
		 */

		try {
			POIFSFileSystem fs = new POIFSFileSystem(fis);
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			HSSFSheet hssfSheet = wb.getSheetAt(4);
			HSSFRow hssfRow;
			int rows = hssfSheet.getPhysicalNumberOfRows();
			for (int r = 0; r <= rows; r++) {
				hssfRow = hssfSheet.getRow(r);
				if (hssfRow == null) {
					break;
				} else {
					resolutionsFile.put(
							hssfRow.getCell(1).getStringCellValue().substring(11,
									hssfRow.getCell(1).getStringCellValue().length()),
							hssfRow.getCell(0).getStringCellValue());
				}
			}
		} catch (FileNotFoundException fileNotFoundException) {
			System.out.println("Escel input file not found: " + fileNotFoundException);
		} catch (IOException ex) {
			System.out.println("File procesing IO Error: " + ex.getStackTrace());
		} finally {
			try {
				fis.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		/*
		 * Comparison
		 */

		resolutionsFile.remove("Service Operation");
		ArrayList<Resolution> diff12 = new ArrayList<Resolution>();
		Iterator it = resolutions.iterator();		 
		while (it.hasNext()) {		 
			Resolution shift = (Resolution) it.next();
			for(int interm =0; interm<resolutionsFile.size();interm++) {
				if (resolutionsFile.containsKey(shift.getRr())) {
//					K		
				}
				else {
					System.out.println(":D : " + shift.getMwf() + ":D : " + shift.getRr());
					diff12.add(shift);
					break;
				}
			}
		 
		}
		
		if (diff12.isEmpty()) {
			System.out.println("RR Match");

		} else {
			System.out.println("RR mismatch, write differences to output file");

			/*
			 * Write output file
			 */

			if (alreadyExists) {
				File missingRR = new File(outputFile);
				missingRR.delete();
			}

			try {

				CsvWriter csvOutput = new CsvWriter(new FileWriter(outputFile, true), ',');

				csvOutput.write("WF_NAME");
				//			csvOutput.write("SO_NAME");
				csvOutput.write("RR_NAME");
				csvOutput.endRecord();
				Iterator<Resolution> diffIter = diff12.iterator();
				while(diffIter.hasNext()) {
					Resolution cellTemp = diffIter.next();
					csvOutput.write(cellTemp.getMwf());
					csvOutput.write(cellTemp.getRr());
					csvOutput.endRecord();
					}
					
				csvOutput.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

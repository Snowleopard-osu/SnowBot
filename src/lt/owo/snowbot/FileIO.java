package lt.owo.snowbot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FileIO {
	
	public static Settings loadSettings(String filename) {
		Settings ret = new Settings();
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			File jsonFile = new File(filename);
			JsonNode root = mapper.readTree(jsonFile);
			
			ret.CMD_PREFIX = root.get("command_prefix").asText();
			ret.AUTH_TOKEN = root.get("bot_auth_token").asText();
			ret.OWNER_ID = root.get("owner_id").asText();
			
			JsonNode files = root.get("data_location");
			
			ret.COMM_FILE = files.get("commands").asText();
			ret.ADMIN_FILE = files.get("admins").asText();
			ret.STAT_FILE = files.get("status").asText();
			
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return ret;
	}
	
	public static HashMap<String, String> loadCommandsFromFile(String filename) {
		File file = new File(filename);
		Scanner in;
		HashMap<String, String> ret = new HashMap<String, String>();
		
		System.out.println("\nAttempting to load command list from <" + filename + ">");
		
		try {
			in = new Scanner(file, "UTF-8");
		} catch (FileNotFoundException e) {
			System.out.println("Error: File <" + filename + "> could not be found!");
			return null;
		}
		
		while (in.hasNextLine()) {
			String line = in.nextLine();
			// This means the prompt cannot have a comma in it, but the response can
			String prompt = line.substring(0, line.indexOf(","));
			String response = line.substring(line.indexOf(",") + 1);
			
			ret.put(prompt, response);
			/*if (addNewCommand(prompt, response)) {
				System.out.println("\t<" + prompt + "> loaded...");
			} else {
				System.out.println("\tError loading <" + prompt + ">...");
			}*/
		}
		
		System.out.println("Finished loading commands from file\n");
		
		in.close();
		
		return ret;
	}
	
	public static boolean saveCommandsFile(String filename, Map<String, String> commands) {
		try {
			// Use a tmp file to prevent loss of data in the event this method fails
			File file = new File(filename + ".tmp");
			
			if (file.exists()) {
				file.delete();
			}
			
			file.createNewFile();
			
		    PrintWriter printWriter = new PrintWriter(file, "UTF-8");
		    
		    for (String key : commands.keySet()) {
		    	String msg = commands.get(key);
		    	printWriter.println(key + "," + msg);
		    }
		    
		    printWriter.close();
		    
		    
		    // Making tmp replace the real file
		    File orig = new File(filename);
		    
		    if (orig.exists()) {
		    	orig.delete();
		    }
		    
		    file.renameTo(orig);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public static ArrayList<String> loadAdminsFromFile(String filename) {
		File file = new File(filename);
		Scanner in;
		ArrayList<String> admins = new ArrayList<String>();
		
		System.out.println("\nAttempting to load admin list from <" + filename + ">");
		
		try {
			in = new Scanner(file, "UTF-8");
		} catch (FileNotFoundException e) {
			System.out.println("Error: File <" + filename + "> could not be found!");
			return null;
		}
		
		while (in.hasNextLine()) {
			String line = in.nextLine();
			
			admins.add(line);
			/*if (addNewAdmin(line)) {
				System.out.println("\t<" + line + "> loaded...");
			} else {
				System.out.println("\tError loading <" + line + ">...");
			}*/
		}
		
		System.out.println("Finished loading admins from file\n");
		
		in.close();
		
		return admins;
	}
	
	public static boolean saveAdminsFile(String filename, ArrayList<String> admins) {
		try {
			// Use a tmp file to prevent loss of data in the event this method fails
			File file = new File(filename + ".tmp");
			
			if (file.exists()) {
				file.delete();
			}
			
			file.createNewFile();
			
		    PrintWriter printWriter = new PrintWriter(file, "UTF-8");
		    
		    for (String key : admins) {
		    	printWriter.println(key);
		    }
		    
		    printWriter.close();
		    
		    
		    // Making tmp replace the real file
		    File orig = new File(filename);
		    
		    if (orig.exists()) {
		    	orig.delete();
		    }
		    
		    file.renameTo(orig);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public static String loadStatusFromFile(String filename) {
		File file = new File(filename);
		Scanner in;
		
		System.out.println("\nAttempting to load status from <" + filename + ">");
		
		try {
			in = new Scanner(file, "UTF-8");
		} catch (FileNotFoundException e) {
			System.out.println("Error: File <" + filename + "> could not be found!");
			return "";
		}
		
		String stat = "";
		
		if (in.hasNextLine()) {
			stat = in.nextLine();
			
			if (stat.length() > 2) {
				System.out.println("\t<" + stat + "> loaded...");
			} else {
				System.out.println("\tError loading <" + stat + ">...");
				stat = "";
			}
		}
		
		System.out.println("Finished loading status from file\n");
		
		in.close();
		
		return stat;
	}
	
	public static boolean saveStatusFile(String filename, String status) {
		try {
			// Use a tmp file to prevent loss of data in the event this method fails
			File file = new File(filename + ".tmp");
			
			if (file.exists()) {
				file.delete();
			}
			
			file.createNewFile();
			
		    PrintWriter printWriter = new PrintWriter(file, "UTF-8");
		    
		    printWriter.println(status);
		    
		    printWriter.close();
		    
		    
		    // Making tmp replace the real file
		    File orig = new File(filename);
		    
		    if (orig.exists()) {
		    	orig.delete();
		    }
		    
		    file.renameTo(orig);
		} catch (Exception e) {
			e.printStackTrace();
			
			return false;
		}
		
		return true;
	}
	
	
}

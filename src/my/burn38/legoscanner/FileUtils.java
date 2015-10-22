package my.burn38.legoscanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {	
	public static boolean createFile(String name) {
		File file = new File(name);
		try {
			return file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	public static boolean deleteFile(String name) {
		File file = new File(name);
		return file.delete();
	}
	public static boolean isEmpty(String name) {
		File file = new File(name);
		if (!file.exists()) return false;
		if (file.isDirectory()) return false;
		if (file.length() == 0) return true;
		return false;
	}
	public static List<String> getLines(String name, List<String> comment_tags) {
		File file = new File(name);
		if (file.length() >= Math.pow(2, Math.pow(2, 5))) {
			try {
				List<String> lines = new ArrayList<String>();
				FileInputStream fis = new FileInputStream(file);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis));
				String line = null;
				while ((line = br.readLine()) != null) {
				  for (String str : comment_tags) {
					 if (!line.startsWith(str)) {
						lines.add(line);
					} 
				  }
					
				}
				br.close();	
				fis.close();
				return lines;
			} catch (IOException e) {
				e.printStackTrace();
				return new ArrayList<String>();
			}			
		} else {
			List<String> lines;
			try {
				lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
				for (int i = 0; i < lines.size(); i++) {
					for (String str : comment_tags) {
						if (lines.get(i).startsWith(str)) {
							lines.remove(i);
						}
					}
				}
				return lines;
			} catch (IOException e) {
				e.printStackTrace();
				return new ArrayList<String>();
			}
		}
	}
	public static String readLine(String name, String tag, String separator, boolean raw) {
		File file = new File(name);
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.startsWith(tag+separator)) {
					br.close();
					fis.close();
					if (raw) return line;
					else return line.split(separator, 2)[1];
				}
			}
			br.close();	
			fis.close();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static String readLine(String name, int index, String separator, boolean raw) {
		File file = new File(name);
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			int i = 0;
			while ((line = br.readLine()) != null) {
				if (index == i) {
					br.close();
					fis.close();
					if (raw) return line;
					else return line.split(separator, 2)[1];
				}
				i++;
			}
			br.close();	
			fis.close();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static int getLineIndex(String name, String tag, String separator) {
		List<String> lines = getLines(name, new ArrayList<String>());
		for (int i = 0; i < lines.size(); i++) {
			if(lines.get(i).startsWith(tag+separator)) {
				return i;
			}
		}
		return lines.size()-1;
	}
	public static boolean writeLine(String name, String tag, String value, String separator) {
		boolean result = false;
		if (!new File(name).exists()) return false;
		File file = new File(name);
		try {
			List<String> lines = getLines(name, new ArrayList<String>());
			if (lines.size() > 0) {
				int index = getLineIndex(name, tag, separator);
					lines.set(index, tag+separator+value);
				Files.write(file.toPath(), lines, Charset.defaultCharset(), StandardOpenOption.CREATE);
			} else {
				FileWriter fw = new FileWriter(file);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(tag+value);
				bw.close();
			}
			result = true;
		} catch (IOException e) {
			result = false;
			e.printStackTrace();
		}
		
		
		return result;
	}
	public static boolean writeLine(String name, int index, String tag, String value, String separator) {
		boolean result = false;
		if (!new File(name).exists()) return false;
		File file = new File(name);
		try {
			List<String> lines = getLines(name, new ArrayList<String>());
			if (lines.size() > 0) {
				if (index > lines.size()) {
					lines.add(lines.size()-1, tag+separator+value);
				} else {
					lines.set(index, tag+separator+value);
				}
				Files.write(file.toPath(), lines, Charset.defaultCharset(), StandardOpenOption.CREATE);
			} else {
				FileWriter fw = new FileWriter(file);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(tag+value);
				bw.close();
			}
			result = true;
		} catch (IOException e) {
			result = false;
			e.printStackTrace();
		}
		
		
		return result;
	}
	public static boolean addLine(String name, String tag, String value, String separator) {
		boolean result = false;
		if (!new File(name).exists()) return false;
		File file = new File(name);
		try {
			List<String> lines = getLines(name, new ArrayList<String>());
			if (lines.size() > 0) {
				int index = getLineIndex(name, tag, separator);
					lines.add(index, tag+separator+value);
				Files.write(file.toPath(), lines, Charset.defaultCharset(), StandardOpenOption.CREATE);
			} else {
				FileWriter fw = new FileWriter(file);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(tag+separator+value);
				bw.close();
			}
			result = true;
		} catch (IOException e) {
			result = false;
			e.printStackTrace();
		}
		
		
		return result;
	}
	public static boolean addLine(String name, int index, String tag, String value, String separator) {
		boolean result = false;
		if (!new File(name).exists()) return false;
		File file = new File(name);
		try {
			List<String> lines = getLines(name, new ArrayList<String>());
			if (lines.size() > 0) {
				if (index > lines.size()) {
					lines.add(lines.size()-1, tag+separator+value);
				} else {
					lines.add(index, tag+separator+value);
				}
				Files.write(file.toPath(), lines, Charset.defaultCharset(), StandardOpenOption.CREATE);
			} else {
				FileWriter fw = new FileWriter(file);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(tag+value);
				bw.close();
			}
			result = true;
		} catch (IOException e) {
			result = false;
			e.printStackTrace();
		}
		
		
		return result;
	}
	public static boolean writeComment(String name, String place, String comment_tag, String text, String mode) {
		boolean result = false;
		if (!new File(name).exists()) return false;
		File file = new File(name);
		try {
			List<String> lines = getLines(name, new ArrayList<String>());
			if (lines.size() > 0) {
				int index;
				if (place == "top") {
					int lastcomment=-1;
					for (int i = 0; i < lines.size(); i++) {
						if (i == 0) if (lines.get(0).startsWith(comment_tag)) lastcomment=0;
						else {
							if (lines.get(i).startsWith(comment_tag) && lastcomment==i-1) lastcomment=i;
						}
					}
					index = lastcomment;
				} else if (place == "bottom") {
					index = lines.size()-1;
				} else {
					index = Integer.valueOf(place);
				}
				
				if (index >= lines.size()) {
					lines.add(lines.size()-1, comment_tag+text);
				} else {
					if (mode == "overwrite") {
						lines.set(index, comment_tag+text);
					} else if (mode == "add" || (mode != "overwrite" && mode != "add")) {
						lines.add(index, comment_tag+text);
					}
					
				}
				Files.write(file.toPath(), lines, Charset.defaultCharset(), StandardOpenOption.CREATE);
			} else {
				FileWriter fw = new FileWriter(file);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(comment_tag+text);
				bw.close();
			}
			result = true;
		} catch (IOException e) {
			result = false;
			e.printStackTrace();
		}
		return result;
	}
}

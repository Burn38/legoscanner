package my.burn38.legoscanner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.WritableRaster;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.GrayFilter;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvContour;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_highgui;
import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
import com.googlecode.javacv.cpp.opencv_imgproc;

public class Main {
	static JFrame window;
	static int threshold = 238;
	static double base1cm = 55;
	static boolean should_calibrate = false, is_camera_running = false;
	static JComboBox<String> options; 
	static JSlider filter_power;
	static JButton calibrate;
	static JCheckBox contours=new JCheckBox(), shapes = new JCheckBox();
	static Thread camera_thread;
	static CanvasFrame frame;
	
	public static void main(String[] args) {
		if (!new File("database.db").exists() || empty(new File("database.db"))) setup_settings(new File("database.db"));
		base1cm = Double.parseDouble(readLine(new File("settings.conf"), "base1cm"));
		window = new JFrame();
		JPanel pan = new JPanel();
		final JButton button_start = new JButton("Start");
		final JButton calibrate = new JButton("Calibrate for measurement");
		JButton button_save = new JButton("Save settings");
		final JLabel text= new JLabel(" ");
		final JTextField margeder = new JTextField();
		margeder.setPreferredSize(new Dimension(150,30));
		margeder.setMaximumSize(new Dimension(150,30));
		margeder.setMinimumSize(new Dimension(150,30));
		margeder.setToolTipText("Error merge");
		contours.setText("Draw boundings ?");
		contours.setEnabled(false);
		
		shapes.setText("Log shapes to console ?");
		shapes.setEnabled(false);

		options = new JComboBox<String>();
		options.addItem("Ø");
		options.addItem("B&W");
		options.addItem("iB&W");
		options.addItem("SoG");
		options.addItem("T");
		
		filter_power = new JSlider();
		filter_power.setSize(2, filter_power.getHeight());
		filter_power.setEnabled(false);
		
		options.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				HashMap<String, Integer> show_filterPower = new HashMap<String, Integer>(); show_filterPower.put("B&W", 255); show_filterPower.put("iB&W", 255); show_filterPower.put("SoG", 100);
				show_filterPower.put("T",  100);
				if (show_filterPower.containsKey(options.getSelectedItem())) {
					filter_power.setEnabled(true);
					filter_power.setMaximum(show_filterPower.get(options.getSelectedItem()));
					if (filter_power.getValue() != filter_power.getMinimum() && filter_power.getValue() != filter_power.getMaximum()) {contours.setEnabled(true);shapes.setEnabled(true);}
					else {contours.setEnabled(false);contours.setSelected(false);shapes.setEnabled(false);shapes.setSelected(false);}
				} else {
					filter_power.setEnabled(false);
					if (filter_power.getValue() != filter_power.getMinimum() && filter_power.getValue() != filter_power.getMaximum()) {contours.setEnabled(true);shapes.setEnabled(true);}
					else {contours.setEnabled(false);contours.setSelected(false);shapes.setEnabled(false);shapes.setSelected(false);}
				}
			}
			
		});
		
		filter_power.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				text.setText(filter_power.getValue()+" / "+filter_power.getMaximum());
				if (filter_power.getValue() != filter_power.getMinimum() && filter_power.getValue() != filter_power.getMaximum()) {contours.setEnabled(true);shapes.setEnabled(true);}
				else {contours.setEnabled(false);contours.setSelected(false);shapes.setEnabled(false);shapes.setSelected(false);}
			}
			
		});
		
		button_start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (is_camera_running && frame != null) {
					camera_thread = null;
					is_camera_running = true;
					camera_thread();
				} else {
					camera_thread();
				}
			}
		});
		
		calibrate.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				should_calibrate = true;
			}
			
		});
		
		button_save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String mode = (String)options.getSelectedItem();
				int filterPower = filter_power.getValue();
				boolean display_contours = contours.isSelected(), log_shapes = shapes.isSelected();
				
				writeLine(new File("settings.conf"), "base1cm", Double.toString(base1cm),false);
				writeLine(new File("settings.conf"), ".onload_mode", mode,false);
				writeLine(new File("settings.conf"), ".onload_filterPower", Integer.toString(filterPower),false);
				writeLine(new File("settings.conf"), ".onload_displayContours", Boolean.toString(display_contours),false);
				writeLine(new File("settings.conf"), ".onload_logShapesToConsole", Boolean.toString(log_shapes),false);
				writeLine(new File("settings.conf"), ".onload_merge", Double.valueOf(margeder.getText()).toString(),false);
			}			
		});
		
		try{
			options.setSelectedItem(readLine(new File("settings.conf"), ".onload_mode"));
			filter_power.setValue(Integer.parseInt(readLine(new File("settings.conf"), ".onload_filterPower")));
			contours.setSelected(Boolean.parseBoolean(readLine(new File("settings.conf"), ".onload_displayContours")));
			shapes.setSelected(Boolean.parseBoolean(readLine(new File("settings.conf"), ".onload_logShapesToConsole")));
			margeder.setText(readLine(new File("settings.conf"), ".onload_merge"));
		}catch(Exception e) {}
		
		pan.add(button_start);
		pan.add(options);
		pan.add(button_save);
		pan.add(margeder);
		JPanel t = new JPanel(); t.setLayout(new BorderLayout());
		t.add(filter_power, BorderLayout.CENTER);
		t.add(text, BorderLayout.EAST);
		
		JPanel y = new JPanel();
		y.add(contours, BorderLayout.CENTER);
		y.add(shapes, BorderLayout.SOUTH);
		y.add(calibrate, BorderLayout.EAST);
		
		window.add(t, BorderLayout.CENTER);
		window.add(y, BorderLayout.SOUTH);
		
		window.setResizable(false);
		window.add(pan, BorderLayout.NORTH);
		window.setSize(500,500);
		window.setTitle("Webcam");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setLocationRelativeTo(null);
		window.pack();
		window.setVisible(true);
	}
	
	public static JFrame getWindow() {
		return window;
	}	
	public static Thread getThread() {
		return camera_thread;
	}
	
	public static BufferedImage thresholdImage(BufferedImage image, int threshold, boolean inverted) {
	    BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
	    result.getGraphics().drawImage(image, 0, 0, null);
	    WritableRaster raster = result.getRaster();
	    int[] pixels = new int[image.getWidth()];
	    for (int y = 0; y < image.getHeight(); y++) {
	        raster.getPixels(0, y, image.getWidth(), 1, pixels);
	        for (int i = 0; i < pixels.length; i++) {
	        	if (!inverted) {
	        		if (pixels[i] < threshold) pixels[i] = 255;
	            	else pixels[i] = 0;
	        	} else {
	        		if (pixels[i] < threshold) pixels[i] = 0;
		            else pixels[i] = 255;
	        	}
	        }
	        raster.setPixels(0, y, image.getWidth(), 1, pixels);
	    }
	    return result;
	}

	public static IplImage detectObjects(IplImage srcImage){
		
	    IplImage resultImage = opencv_core.cvCloneImage(srcImage);

	    CvMemStorage mem = CvMemStorage.create();
	    CvSeq contours = new CvSeq();
	    CvSeq ptr = new CvSeq();

	    opencv_imgproc.cvFindContours(srcImage, mem, contours, Loader.sizeof(CvContour.class) , opencv_imgproc.CV_RETR_CCOMP, opencv_imgproc.CV_CHAIN_APPROX_SIMPLE, opencv_core.cvPoint(0,0));

	    CvRect boundbox;

	    for (ptr = contours; ptr != null && !ptr.isNull(); ptr = ptr.h_next()) {
	        try{
	        	boundbox = opencv_imgproc.cvBoundingRect(ptr, 0);
		        opencv_core.cvRectangle( resultImage , opencv_core.cvPoint( boundbox.x(), boundbox.y() ), 
			            opencv_core.cvPoint( boundbox.x() + boundbox.width(), boundbox.y() + boundbox.height()),
			            opencv_core.cvScalar( 120, 255, 10, 0 ), 1, 0, 0 );
		        opencv_core.cvPutText(resultImage, boundbox.width()+" * "+boundbox.height(), opencv_core.cvPoint(boundbox.x(), boundbox.y()), opencv_core.cvFont(0.8, 1), opencv_core.cvScalar(120, 255, 10, 0));
		        opencv_core.cvPutText(resultImage, "["+boundbox.x()+";"+boundbox.y()+"]", opencv_core.cvPoint(boundbox.x(), boundbox.y()-2), opencv_core.cvFont(0.8, 1), opencv_core.cvScalar(120, 255, 10, 0));
		        opencv_core.cvPutText(resultImage,"~"+(new DecimalFormat("#.##").format((boundbox.width() > boundbox.height() ? boundbox.width() : boundbox.height())/base1cm))+"cm", opencv_core.cvPoint(boundbox.x(), boundbox.y() + boundbox.height()-1), opencv_core.cvFont(0.8, 1), opencv_core.cvScalar(120, 255, 10, 0));
	        	}
	        catch(RuntimeException e){}
	    }

	    return resultImage;
	}

	private static IplImage logShapesToConsole(IplImage g2) {
		
		HashMap<ArrayList<Double>, Integer> bdd = loadBDD();
		
		if (bdd == null) {return null;}
		IplImage resultImage = opencv_core.cvCloneImage(g2);

	    CvMemStorage mem = CvMemStorage.create();
	    CvSeq contours = new CvSeq();
	    CvSeq ptr = new CvSeq();

	    opencv_imgproc.cvFindContours(g2, mem, contours, Loader.sizeof(CvContour.class) , opencv_imgproc.CV_RETR_CCOMP, opencv_imgproc.CV_CHAIN_APPROX_SIMPLE, opencv_core.cvPoint(0,0));

	    CvRect boundbox;

	    for (ptr = contours; ptr != null && !ptr.isNull(); ptr = ptr.h_next()) {
	        	boundbox = opencv_imgproc.cvBoundingRect(ptr, 0);
	        	double width = boundbox.width() >= boundbox.height() ? boundbox.width() : boundbox.height();
	        	double height = boundbox.width() < boundbox.height() ? boundbox.width() : boundbox.height();
	        	
	        	width = Double.parseDouble(new DecimalFormat("#.##").format(width/base1cm).replaceAll(",", "."));
	        	height = Double.parseDouble(new DecimalFormat("#.##").format(height/base1cm).replaceAll(",", "."));
	        	
	        	
	        	@SuppressWarnings("unchecked")
				ArrayList<Double>[] keyset = (ArrayList<Double>[])bdd.keySet().toArray(new ArrayList[bdd.keySet().size()]);
	        	for (int i = 0; i < keyset.length; i ++) {
	        		if (width <= keyset[i].get(0)+10 && width >= keyset[i].get(0)-10 && height <= keyset[i].get(1)+10 && height >= keyset[i].get(1)-10) {
	        			System.out.println("Found: ID "+bdd.get(keyset[i])+"["+keyset[i].get(0)+";"+keyset[i].get(1)+"] at ("+boundbox.x()+","+boundbox.y()+").");
	        		}
	        	}
	    }

	    return resultImage;
	}
	
	@SuppressWarnings("unchecked")
	public static double calibrate(BufferedImage src) {
		  if (options.getSelectedItem() == "B&W" || options.getSelectedItem() == "iB&W"|| options.getSelectedItem() == "T") {
		    CvMemStorage mem = CvMemStorage.create();
		    CvSeq contours = new CvSeq();
		    CvSeq ptr = new CvSeq();

		    IplImage grabbedImage = IplImage.createFrom(src);
		    
		    opencv_imgproc.cvFindContours(grabbedImage, mem, contours, Loader.sizeof(CvContour.class) , opencv_imgproc.CV_RETR_CCOMP, opencv_imgproc.CV_CHAIN_APPROX_SIMPLE, opencv_core.cvPoint(0,0));

		    CvRect boundbox;
		    HashMap<ArrayList<Integer>,ArrayList<Integer>> datas = new HashMap<ArrayList<Integer>,ArrayList<Integer>>(); 
		    for (ptr = contours; ptr != null && !ptr.isNull(); ptr = ptr.h_next()) {
		        try{
		        	boundbox = opencv_imgproc.cvBoundingRect(ptr, 0);
			        ArrayList<Integer> a = new ArrayList<Integer>();
			        ArrayList<Integer> b = new ArrayList<Integer>();
			        a.add(boundbox.width()); a.add(boundbox.height()); b.add(boundbox.x()); b.add(boundbox.y());
			        datas.put(a,b);
		        	}
		        catch(RuntimeException e){}
		    }
		    int max = 0;
		    int max_i = 0;
		    for (int i = 0; i < datas.size(); i++) {
				ArrayList<Integer> size = (ArrayList<Integer>)(datas.keySet().toArray(new Object[datas.keySet().size()])[i]);
		    	
		    	if (size.get(0) >= size.get(1)) {
		    		if (size.get(0) > max) {
		    			max = size.get(0);
		    			max_i=i;
		    		}
		    	} else {
		    		if (size.get(1) > max) {
		    			max = size.get(1);
		    			max_i=i;
		    		}
		    	}
		    }
		    ArrayList<ArrayList<Integer>> box = new ArrayList<ArrayList<Integer>>();
		    ArrayList<Integer> size = (ArrayList<Integer>)(datas.keySet().toArray(new Object[datas.keySet().size()])[max_i]);
		    ArrayList<Integer> pos = datas.get(size);
		    box.add(size);
		    box.add(pos);
		    base1cm=(box.get(0).get(0) > box.get(0).get(1) ? box.get(0).get(0) : box.get(0).get(1));
		    
		    writeLine(new File("settings.conf"), "base1cm", Double.toString(base1cm),false);
		  }
		return base1cm;
		}
	
	public static void setup_settings(File file) {
		if (!file.exists()) {
			try {
				file.createNewFile();
				setup_settings(file);
			} catch (IOException e) {}
		}
		if (file.getName().contains("settings")) {
			if (readLine(file, "base1cm") == "0x.E1") writeLine(file, "base1cm", Double.toString(base1cm),false);
			if (readLine(file, ".onload_mode") == "0x.E1") writeLine(file, ".onload_mode", "Ø",false);
			if (readLine(file, ".onload_filterPower") == "0x.E1") writeLine(file, ".onload_filterPower", "50",false);
			if (readLine(file, ".onload_displayContours") == "0x.E1") writeLine(file, ".onload_displayContours", "false",false);
			if (readLine(file, ".onload_logShapesToConsole") == "0x.E1") writeLine(file, ".onload_logShapesToConsole", "false",false);
			if (readLine(file, ".onload_merge") == "0x.E1") writeLine(file, ".onload_merge", "1",false);
		} else if (file.getName().contains("database") && empty(file)) {
			writeComment(file, "Ajouter des types de blocs de la façon suivante:   id:longueur;largeur", false);
			writeComment(file, "Exemple:   1:3.2;5.0", false);
			List<String> lines = new ArrayList<String>();
			try {
				lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("Creating "+file.getName()+" with:");
			for (int i = 0; i < lines.size(); i++) {
				System.out.println(lines.get(i));
			}
			try {
				Files.write(file.toPath(), lines, Charset.defaultCharset(), StandardOpenOption.CREATE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String readLine(File file, String str) {
		try {
			if (!file.exists()) setup_settings(file);
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("'"+str+"'"+":") || line.startsWith("'"+str+"'"+": ") || line.startsWith(str+":") || line.startsWith(str+": ")) {
					br.close();
					fis.close();
					line = line.replaceAll("'", "").replaceFirst(": ", ":");
					String value = line.split(":", 2)[1];
					value = value.endsWith("\"") ? value.substring(0, value.length()-1) : value;
					value = value.startsWith("\"") ? value.substring(1, value.length()) : value;
					return value;
				}
			}
			br.close();	
			fis.close();
			return "0x.E1";
		} catch (IOException e) {
			e.printStackTrace();
			return "0x.E2";
		}
	}
	
	public static int getLineIndex(File file, String key, String mode) {
		if (!file.exists()) setup_settings(file);
		
		if (mode == "add") {
			try {
				return Files.readAllLines(file.toPath(), Charset.defaultCharset()).size()-1;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (mode == "overwrite") {
			
			try {
				if (!file.exists()) {setup_settings(file); return getLineIndex(file, key, mode);}
				FileInputStream fis = new FileInputStream(file);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis));
				String line = null;
				int index = 0;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("'"+key+"'"+":") || line.startsWith("'"+key+"'"+": ") || line.startsWith(key+":") || line.startsWith(key+": ")) {
						br.close();
						fis.close();
						return index;
						
					}
					index++;
				}
				br.close();	
				fis.close();
				return Files.readAllLines(file.toPath(), Charset.defaultCharset()).size()-1;
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
			
		} else {
			return 0;
		}
		return 0;		
	}
	
	public static boolean writeComment(File file, String text, boolean apostrophy) {
		if (!file.exists()) setup_settings(file);
		boolean result = false;
		try {
			List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
			if (lines.size() > 0) {
				int index = getLineIndex(file, "#", "add");
					if (apostrophy) lines.add(index, "#"+"'"+ text+"'");
					else lines.add(index, "#"+text);
					Files.write(file.toPath(), lines, Charset.defaultCharset(), StandardOpenOption.CREATE);
			} else {
				FileWriter fw = new FileWriter(file);
				BufferedWriter bw = new BufferedWriter(fw);
				if (apostrophy) bw.write("#"+"'"+ text+"'");
				else bw.write("#"+text);
				bw.close();
			}
			result = true;
		} catch (IOException e) {
			result = false;
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static boolean writeLine(File file, String key, String value, boolean apostrophy) {
		if (!file.exists()) setup_settings(file);
		boolean result = false;
		try {
			List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
			if (lines.size() > 0) {
				int index = getLineIndex(file, key, "overwrite");
					if (apostrophy) lines.add(index, key+":'"+ value+"'");
					else lines.set(index, key+":"+value);
					Files.write(file.toPath(), lines, Charset.defaultCharset(), StandardOpenOption.CREATE);
			} else {
				FileWriter fw = new FileWriter(file);
				BufferedWriter bw = new BufferedWriter(fw);
				if (apostrophy) bw.write(key+":'"+ value+"'");
				else bw.write(key+":"+value);
				bw.close();
			}
			result = true;
		} catch (IOException e) {
			result = false;
			e.printStackTrace();
		}
		
		
		return result;
	}
	
	public static List<String> getLines(File file, String comment_tag) {
		if (!file.exists()) setup_settings(file);
		List<String> lines = new ArrayList<String>();
		try {
			
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (!line.startsWith(comment_tag)) {
					lines.add(line);
				}
			}
			br.close();	
			fis.close();
			return lines;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}
	
	public static HashMap<ArrayList<Double>,Integer> loadBDD() {
		File file = new File("database.db");
		HashMap<ArrayList<Double>,Integer> database = new HashMap<ArrayList<Double>,Integer>();
		
		List<String> lines = getLines(file,"#");
		System.out.println("-===============:[BDD]:===============-");
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] inf = line.split(":",2);
			ArrayList<Double> inf_l = new ArrayList<Double>();
			inf_l.add(Double.parseDouble(inf[1].split(";",2)[0]));
			inf_l.add(Double.parseDouble(inf[1].split(";",2)[1]));
			database.put(inf_l, Integer.parseInt(inf[0].replaceAll("\"", "")));
			System.out.println(inf_l+": "+inf[0]);
		 }
		return database;
	}
	
	public static Thread camera_thread() {
		if (camera_thread == null) {
		camera_thread = new Thread() {
			public void run() {
				is_camera_running = true;
				CvCapture capture = opencv_highgui.cvCreateCameraCapture(0);
				opencv_highgui.cvSetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 480);
				opencv_highgui.cvSetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 640);
				
				IplImage grabbedImage = opencv_highgui.cvQueryFrame(capture);
				frame = new CanvasFrame("Webcam "+"("+threshold+")");
				frame.addWindowListener(new java.awt.event.WindowAdapter() {
				    @Override
				    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				            camera_thread = null;
				            frame = null;
				    }
				});
				while(frame != null && frame.isVisible() && (grabbedImage = opencv_highgui.cvQueryFrame(capture)) != null) {
					
					BufferedImage buff = grabbedImage.getBufferedImage();
					
					//Image color processing
					switch ((String)Main.options.getSelectedItem()) {
					case "Ø": {
						break;
					}
					case "B&W": {
						buff = thresholdImage(buff, filter_power.getValue(), false);
						opencv_imgproc.GaussianBlur(IplImage.createFrom(buff), grabbedImage, opencv_core.cvSize(1,1), 0, 0, 0);
						if (should_calibrate) {
							should_calibrate = false;
							try{calibrate(buff);}
							catch (Exception e) {e.printStackTrace();}
						}
						break;
					}
					case "iB&W": {
						buff = thresholdImage(buff, filter_power.getValue(), true);
						opencv_imgproc.GaussianBlur(IplImage.createFrom(buff), grabbedImage, opencv_core.cvSize(1,1), 0, 0, 0);
						if (should_calibrate) {
							should_calibrate = false;
							try{calibrate(buff);}
							catch (Exception e) {e.printStackTrace();}
						}
						break;
					}
					case "SoG": {
						ImageFilter filter = new GrayFilter(true, filter_power.getValue());  
						ImageProducer producer = new FilteredImageSource(buff.getSource(), filter);
						Image mage = Toolkit.getDefaultToolkit().createImage(producer);  
						   BufferedImage bimage = new BufferedImage(mage.getWidth(null), mage.getHeight(null), BufferedImage.TYPE_BYTE_GRAY);

						    // Draw the image on to the buffered image
						    Graphics2D bGr = bimage.createGraphics();
						    bGr.drawImage(mage, 0, 0, null);
						    bGr.dispose();

						    // Return the buffered image
						    buff = bimage;
						break;
					}
					case "T": {
						break;
					}
					default: {
						break;
					}
					}
					 		
					
					grabbedImage = IplImage.createFrom(buff);
					IplImage g2 = grabbedImage.clone();
					
					if (contours.isSelected() && contours.isEnabled()) {
						if (filter_power.getValue() != filter_power.getMinimum() && filter_power.getValue() != filter_power.getMaximum()) {contours.setEnabled(true);g2=detectObjects(g2);}
						else {contours.setEnabled(false);contours.setSelected(false);shapes.setEnabled(false);shapes.setSelected(false);}
					}
					if (shapes.isSelected() && shapes.isEnabled()) {
						if (filter_power.getValue() != filter_power.getMinimum() && filter_power.getValue() != filter_power.getMaximum()) {shapes.setEnabled(true);g2=logShapesToConsole(g2);}
						else {shapes.setEnabled(false);contours.setSelected(false);shapes.setEnabled(false);shapes.setSelected(false);}
					}
					
					if (frame != null) {frame.setLocationRelativeTo(null);
					frame.showImage(g2);}
				}
			}
		};
		camera_thread.start();
		}
		return camera_thread;
		
	}
	
	public static boolean empty(File file) {
			return file.length() == 0 || !file.exists() || file.isDirectory();
	}
}

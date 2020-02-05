package application;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import com.sun.javafx.scene.control.skin.SliderSkin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import utilities.Utilities;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.filechooser.*;
import javax.swing.plaf.SliderUI;

import java.io.*;

public class Controller extends JPanel{
	
	@FXML
	private ImageView imageView; // the image display window in the GUI
	
	@FXML
	private Slider slider;
	
	private Mat image;
	
	private int width;
	private int height;
	private int sampleRate; // sampling frequency
	private int sampleSizeInBits;
	private int numberOfChannels;
	private double[] freq; // frequencies for each particular row
	private int numberOfQuantizionLevels;
	private int numberOfSamplesPerColumn;
	
	private VideoCapture capture;
	private ScheduledExecutorService timer;
	
	   JButton go;
	   String sourceFolder="";
	   String theFile="";
	   JFileChooser chooser;
	   String choosertitle;
	
	@FXML
	private void initialize() {
		// Optional: You should modify the logic so that the user can change these values
		// You may also do some experiments with different values
		width = 64;
		height = 64;
		sampleRate = 8000;
		sampleSizeInBits = 8;
		numberOfChannels = 1;
		
		numberOfQuantizionLevels = 16;
		
		numberOfSamplesPerColumn = 500;
		
		// assign frequencies for each particular row
		freq = new double[height]; // Be sure you understand why it is height rather than width
		freq[height/2-1] = 440.0; // 440KHz - Sound of A (La)
		for (int m = height/2; m < height; m++) {
			freq[m] = freq[m-1] * Math.pow(2, 1.0/12.0); 
		}
		for (int m = height/2-2; m >=0; m--) {
			freq[m] = freq[m+1] * Math.pow(2, -1.0/12.0); 
		}
	}
	
	
	protected void createFrameGrabber() throws InterruptedException {
		 if (capture != null && capture.isOpened()) { // the video must be open
		 double framePerSecond = capture.get(Videoio.CAP_PROP_FPS);
		 // create a runnable to fetch new frames periodically
		 Runnable frameGrabber = new Runnable() {
			 
		 @Override
		 public void run() {
			 Mat frame = new Mat();
			 if (capture.read(frame)) { // decode successfully
				 javafx.scene.image.Image im = Utilities.mat2Image(frame);
				 Utilities.onFXThread(imageView.imageProperty(), im);
				 double currentFrameNumber = capture.get(Videoio.CAP_PROP_POS_FRAMES);
				 double totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
				 slider.setValue(currentFrameNumber / totalFrameCount * (slider.getMax() - slider.getMin()));
			 } else { // reach the end of the video
				 capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
			 }
			 }
		 };
		 // terminate the timer if it is running
		 if (timer != null && !timer.isShutdown()) {
			 timer.shutdown();
			 timer.awaitTermination(Math.round(1000/framePerSecond), TimeUnit.MILLISECONDS);
		 }
		 // run the frame grabber
		 timer = Executors.newSingleThreadScheduledExecutor();
		 timer.scheduleAtFixedRate(frameGrabber, 0, Math.round(1000/framePerSecond), TimeUnit.MILLISECONDS);
		 }
	}
    
	// This method should return the filename of the image to be played
	// You should insert your code here to allow user to select the file
	private String getImageFilename() {
	     chooser = new JFileChooser(); 
	     chooser.setCurrentDirectory(new java.io.File("."));
	     chooser.setDialogTitle(choosertitle);
	     FileNameExtensionFilter filter = new FileNameExtensionFilter(
	        "JPG, PNG, GIF, and MP4", "jpg", "gif", "png", "mp4");
	     chooser.setFileFilter(filter);
	     chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    
	    
	    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
	      
	         String dirr = "" + chooser.getCurrentDirectory();
	         File file = chooser.getSelectedFile();
	       
	      if(dirr.substring(dirr.length()-1,dirr.length()).equals(".")){
	           dirr = dirr.substring(0,dirr.length()-1);
	           sourceFolder=""+dirr + "" + file.getName();
	        }else{
	            
	            sourceFolder=""+dirr + "/" + file.getName();
	        }

	          System.out.println("Folder path: " + dirr + " | File Name: " + file.getName());
	          System.out.println(sourceFolder);
	 			//ExamineImage.lum(sourceFolder);
	          return sourceFolder;
	    
	      } else {
	  		return "resources/test.png";
	      }

	}
	
	@FXML
	protected void openImage(ActionEvent event) throws InterruptedException {
		// This method opens an image and display it using the GUI
		// You should modify the logic so that it opens and displays a video
		
		//(MEL)TODO: ADD IN THIS VIDEO CAPTURE CODE FOR VIDEO PLAY
		capture = new VideoCapture(getImageFilename()); // open video file
		 if (capture.isOpened()) { // open successfully
		 createFrameGrabber();
		 }
		 
		//FOR IMAGES
//		final String imageFilename = getImageFilename();
//		image = Imgcodecs.imread(imageFilename);
//		imageView.setImage(Utilities.mat2Image(image)); 
		
		// You don't have to understand how mat2Image() works. 
		// In short, it converts the image from the Mat format to the Image format
		// The Mat format is used by the opencv library, and the Image format is used by JavaFX
		// BTW, you should be able to explain briefly what opencv and JavaFX are after finishing this assignment
	}

	@FXML
	protected void playImage(ActionEvent event) throws LineUnavailableException {
		// This method "plays" the image opened by the user
		// You should modify the logic so that it plays a video rather than an image
		if (image != null) {
			// convert the image from RGB to grayscale
			Mat grayImage = new Mat();
			Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
			
			// resize the image
			Mat resizedImage = new Mat();
			Imgproc.resize(grayImage, resizedImage, new Size(width, height));
			
			// quantization
			double[][] roundedImage = new double[resizedImage.rows()][resizedImage.cols()];
			for (int row = 0; row < resizedImage.rows(); row++) {
				for (int col = 0; col < resizedImage.cols(); col++) {
					roundedImage[row][col] = (double)Math.floor(resizedImage.get(row, col)[0]/numberOfQuantizionLevels) / numberOfQuantizionLevels;
				}
			}
			
			// I used an AudioFormat object and a SourceDataLine object to perform audio output. Feel free to try other options
	        AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, numberOfChannels, true, true);
            SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
            sourceDataLine.open(audioFormat, sampleRate);
            sourceDataLine.start();
            
            for (int col = 0; col < width; col++) {
            	byte[] audioBuffer = new byte[numberOfSamplesPerColumn];
            	for (int t = 1; t <= numberOfSamplesPerColumn; t++) {
            		double signal = 0;
                	for (int row = 0; row < height; row++) {
                		int m = height - row - 1; // Be sure you understand why it is height rather width, and why we subtract 1 
                		int time = t + col * numberOfSamplesPerColumn;
                		double ss = Math.sin(2 * Math.PI * freq[m] * (double)time/sampleRate);
                		signal += roundedImage[row][col] * ss;
                	}
                	double normalizedSignal = signal / height; // signal: [-height, height];  normalizedSignal: [-1, 1]
                	audioBuffer[t-1] = (byte) (normalizedSignal*0x7F); // Be sure you understand what the weird number 0x7F is for
            	}
            	sourceDataLine.write(audioBuffer, 0, numberOfSamplesPerColumn);
            }
            sourceDataLine.drain();
            sourceDataLine.close();
		} else {
			// What should you do here?
		}
	}
}

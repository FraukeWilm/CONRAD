package edu.stanford.rsl.tutorial.modelObserver;
import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.utils.ImageUtil;
import edu.stanford.rsl.conrad.utils.VisualizationUtil;
import ij.ImageJ;
import ij.ImagePlus;

/**
 * Model_demo provides demo example of model_obserever using Hotelling Observer. 
 * It creates one set of images with signal and noise, called signal class
 *  and another set of images with noisy background only, called non-signal class. 
 *  Hotelling observer is used to classify images with signal versus images without signal. 
 *   
 *  This code contains code transferred from the IQmodelo framework. 
 *  The original functions were written by Adam Wunderlich in MATLAB and
 *  are available online, URL: https://github.com/DIDSR/IQmodelo  
 * @author Priyal Patel
 * @Supervisors: Frank Schebesch, Andreas Maier
 */
public class ModelDemo{
	
	float background_component[]; //Amplitude of noise component 1 and noise component 2
	int n=150;                    //Number of samples of both classes   
	double[] space={1.0,1.0};     //Spacing between pixel point of image
	double[] class1;              //Score of class1(signal present class)
	double[] class2;              //Score of class2(signal absent class) 
	int width=500;                //Width of an image
	int height=500;               //Height of an image
	float template_value=2.5f;     // Gaussian standard deviation of reference signal 
	String shape;                 // shape of reference signal image("Disk" or "Gaussian") 
	/**
	 * Constructor 
	 * @param background_component  
	 * @param n                      
	 * @param space                 
	 * @param class1
	 * @param class2
	 * @param width
	 * @param height
	 * @param template_value
	 * @param shape 
	 */
	
	public ModelDemo(int width,int height,int n,float background_component[],float template_value,double[] class1,double[] class2,double[] space,String shape) {
		
		super();
		
		// TODO Auto-generated constructor stub
		this.n =n;
		this.background_component=background_component;
		this.height=height;
		this.width=width;
		this.class1=class1;
		this.class2=class2;
		this.template_value=template_value;
		this.space= space;
		this.shape = shape;
		generateImages(this.height,this.width,background_component,this.template_value,shape);
	}
	
	/**
	 * Default constructor 
	 */
	
	public ModelDemo() {
		
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * computation of score of class1 (signal present image) and score of class2 (signal absent image)
	 * @param width
	 * @param height
	 * @param background_component
	 * @param template_value
	 * @param shape
	 */
	void generateImages(int width,int height,float background_component[],float template_value,String shape){
		
		//reference image with only signal(it can be disk or Gaussian)
		Grid2D template = new Grid2D(width, height);
		CreateImages image1 = new CreateImages(width, height,space);
		template = image1.templates(width, height, template_value,shape);
		template.show();
		
		//Generates n sets of image for both signal present and signal absent classes.
		for (int i = 0; i < n; i++) {
			Grid2D sp_image;// Signal present image
			Grid2D sa_image;// Signal absent image
			
			//Generates Gaussian noise with two different amplitude components 
			CreateImages[] background_noise = new CreateImages[2];
			background_noise[0] = new CreateImages(width, height,space);
			background_noise[1] = new CreateImages(width, height,space);
			
			//Apply Cone-filter on previously generated Gaussian noise
			Grid2D[] filtered_noise = new Grid2D[2];
			filtered_noise[0] = background_noise[0].coneFilter(background_noise[0],background_component[0]);
			filtered_noise[1] = background_noise[1].coneFilter(background_noise[1],background_component[1]);
			
			// Creates Gaussian signal
			Grid2D signal = new Grid2D(width, height);
			signal = background_noise[1].createSignal();
			
			//Generates signal-Present image
			sp_image = background_noise[0].signalPresent(filtered_noise, signal);
			
			//Generates signal-absent image 
			sa_image= background_noise[1].signalAbsent(filtered_noise);

			//Computes Score of class 1 and class 2 
			for (int y = 0; y <height; y++) {
				for (int x = 0; x < width; x++) {
					class1[i] += (sp_image.getAtIndex(y,x) * template.getAtIndex(y,x));
					class2[i] += (sa_image.getAtIndex(y,x) * template.getAtIndex(y,x));
				}
			}
			System.out.println("class1            " + class1[i]);
			System.out.println("class2            " + class2[i]);
			
			// shows one set of computed images
			if (i == 1) {
				
				sa_image.show();
				signal.show();
				sp_image.show();
			}
		}
		
	}
	
	/**
	 * Example on how to use Model_demo class
	 * @param args not used
	 */
	public static void main(String[] args){
		new ImageJ();
		int width=500;
		int height=500;
		int n=50;
		double[] space={1.,1.};
		float [] background_component={-5.0f,5f};
		double[] class1 = new double[n];
		double[] class2 = new double[n];
		float template_value=2.5f;
		String shape = "Disk";//Select shape of reference signal ("Disk" or "Gaussian")
		
		//Model_demo constructor generates score of class1(signal-present) and class2(signal-absent)
		ModelDemo a = new ModelDemo(width,height,n,background_component,template_value,class1,class2,space,shape);
		
		// Get the ROC curve using ROC class  
		ROC roc = new ROC(a.class1, a.class2, true);
		roc.computeFractions();
		
		// create ROC plot
		VisualizationUtil.createPlot(roc.getFPF(), roc.getSensitivity(), "ROC", "fpf", "sens").show();
	}	
}
/*
 * Copyright (C) 2010-2016 -Priyal Patel,Frank Schebesch,Andreas Maier 
 * CONRAD is developed as an Open Source project under the GNU General Public License
 * (GPL).
 */

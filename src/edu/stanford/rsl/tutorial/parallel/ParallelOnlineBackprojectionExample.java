package edu.stanford.rsl.tutorial.parallel;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.NumericPointwiseOperators;
import edu.stanford.rsl.tutorial.phantoms.DotsGrid2D;
import edu.stanford.rsl.tutorial.phantoms.MickeyMouseGrid2D;
import edu.stanford.rsl.tutorial.phantoms.Phantom;
import edu.stanford.rsl.tutorial.phantoms.UniformCircleGrid2D;
import ij.IJ;
import ij.ImageJ;


/**
 * This example can be used to demonstrate Backprojection
 *
 */
public class ParallelOnlineBackprojectionExample {

	public static void main (String [] args){
		new ImageJ();
		
		int x = 256;
		int y = 256;
		// Create a phantom
		Phantom phan = new DotsGrid2D(x, y);
		phan = new UniformCircleGrid2D(x, y);
		//phan = new MickeyMouseGrid2D(x, y);
		phan.show("The Phantom");
		
		// Project forward parallel
		ParallelProjector2D projector = new ParallelProjector2D(Math.PI-Math.PI/180.0, Math.PI/180.0, 400, 1);
		Grid2D sinogram = projector.projectRayDriven(phan);
		sinogram.show("The Sinogram");
		Grid2D filteredSinogram = new Grid2D(sinogram);
		
		Grid2D empty = new Grid2D(x,y);
		empty.show("Current Reconstruction");
		
		// Backproject and show
		ParallelBackprojector2D backproj = new ParallelBackprojector2D(x, y, 1, 1);
		for (int i =0; i < filteredSinogram.getSize()[1]; i++){
			Grid2D toAdd = backproj.backprojectPixelDriven(filteredSinogram, i);
			//toAdd.show();
			NumericPointwiseOperators.addBy(empty, toAdd);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			IJ.run("Enhance Contrast", "0");
			//return;
		}
		System.out.println("Backprojection Done");
		
	}
	
}
/*
 * Copyright (C) 2010-2014 Shiyang Hu
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
*/
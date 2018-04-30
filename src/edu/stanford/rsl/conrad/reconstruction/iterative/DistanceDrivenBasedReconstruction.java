package edu.stanford.rsl.conrad.reconstruction.iterative;

/**
 * @author febmeng
 *
 */

import edu.stanford.rsl.conrad.data.numeric.Grid3D;
import edu.stanford.rsl.conrad.data.numeric.NumericPointwiseOperators;
import edu.stanford.rsl.conrad.geometry.Projection;
import edu.stanford.rsl.conrad.geometry.trajectories.Trajectory;
import edu.stanford.rsl.conrad.numerics.SimpleMatrix;
import edu.stanford.rsl.conrad.numerics.SimpleOperators;
import edu.stanford.rsl.conrad.numerics.SimpleVector;


public class DistanceDrivenBasedReconstruction  extends ModelBasedIterativeReconstruction {


	public class LeastSquareReconstructionCG {

	}

	private static final long serialVersionUID = 1L;
	public boolean Debug = true;
	public boolean Debug1 = false;
	public boolean Debug2 = false;
	protected static final int MAX_WEIGHT_LENGTH_U = 8;
	protected static final int MAX_WEIGHT_LENGTH_V = 8;

	public DistanceDrivenBasedReconstruction( Trajectory dataTrajectory ) {
		super(dataTrajectory);
		// TODO Auto-generated constructor stub
	}
	
	
	@Override
	public void forwardproject(Grid3D projImage, Grid3D volImage) throws Exception {

		//zero out whole projection image		
		NumericPointwiseOperators.fill(projImage, 0.0f);
		Projection proj;

		for ( int p = 0; p < nImages ; p++ ){
			proj = getGeometry().getProjectionMatrix(p);
			distanceDrivenProjView( projImage, volImage, proj, p );
		}

		//if (Debug) projImage.printOneSlice(8);
	}


	@Override
	public void backproject(Grid3D projImage, Grid3D volImage) throws Exception {

		//zero out whole volume image
		NumericPointwiseOperators.fill(volImage, 0.0f);
		Projection proj;

		for ( int p = 0; p < nImages; p++ ){
			proj = getGeometry().getProjectionMatrix(p);
			distanceDrivenBackView( projImage, volImage, proj, p );
		}

	}

	protected void distanceDrivenProjView( Grid3D projImage, Grid3D volImage, Projection proj, final int ip ){

		SimpleMatrix mat = proj.computeP();
		SimpleVector cameraCenter = proj.computeCameraCenter();

		// buffer have move of voxel
		SimpleVector halfVoxelMoveX = mat.getCol(0);
		halfVoxelMoveX.multiplyBy(dx/2);
		SimpleVector halfVoxelMoveY = mat.getCol(1);
		halfVoxelMoveY.multiplyBy(dy/2);
		SimpleVector fullVoxelMoveZ = mat.getCol(2);
		fullVoxelMoveZ.multiplyBy(dz);

		SimpleVector point3d = new SimpleVector(4);
		point3d.setElementValue(3, 1);

		SimpleVector point2d, point2dMinus, point2dPlus;

		double cx = cameraCenter.getElement(0);
		double cy = cameraCenter.getElement(1);
		double cz = cameraCenter.getElement(2);
		double dsx0, dsy0, dsxy0_sqr;
		float coordLeft, coordRight, coordBottom, coordTop, coordStep;
		double ds0;
		float amplitude;
		int iumin, iumax, ivmin, ivmax;

		if (Debug1) {
			System.out.println( "Camera Center: " + cx + ", " + cy);
			System.out.println( "Voxel Spaceing: " + dx + ", " + dy);
			System.out.println( "Projection matrix: ");
			printSimpleMatrix(mat);
			System.out.println( "Half voxel moves: ");
			printSimpleVector(halfVoxelMoveX);
			printSimpleVector(halfVoxelMoveY);
		} 

		for (int i = 0; i < maxI; i++){

			point3d.setElementValue(0, i*dx - offsetX  );

			for (int j = 0; j < maxJ; j++){
				
				point3d.setElementValue(1, j*dy - offsetY );
				
				point3d.setElementValue(2, -dz/2 - offsetZ);
				point2d = SimpleOperators.multiply(mat, point3d);

				dsx0 = Math.abs(point3d.getElement(0) - cx);
				dsy0 = Math.abs(point3d.getElement(1) - cy);
				dsxy0_sqr = dsx0*dsx0 + dsy0*dsy0;

				if (Debug1){
					System.out.println("dsx0="+ dsx0 + ", dsy0=" + dsy0 + ", dsxy0_sqr= " + dsxy0_sqr);
				}

				if ( dsy0 > dsx0){
					ds0 = dsy0;
					point2dMinus = SimpleOperators.subtract(point2d, halfVoxelMoveX);
					point2dPlus = SimpleOperators.add(point2d, halfVoxelMoveX);					
				}else{
					ds0 = dsx0;
					point2dMinus = SimpleOperators.subtract(point2d, halfVoxelMoveY);
					point2dPlus = SimpleOperators.add(point2d, halfVoxelMoveY);
				}

				coordLeft =  (float) ( point2dMinus.getElement(0) / point2dMinus.getElement(2) + 0.5 );
				coordRight = (float) ( point2dPlus.getElement(0) / point2dPlus.getElement(2) + 0.5 );

				if ( coordLeft > coordRight ){
					float temp = coordRight;
					coordRight = coordLeft;
					coordLeft = temp;	
				}

				coordLeft = Math.max(coordLeft, 0);
				coordRight = Math.min(coordRight, maxU);

				if (coordLeft >= coordRight){
					continue;
				}

				iumin = (int)coordLeft;
				iumax = (int)coordRight;
				iumax = Math.min(iumax, maxU-1);

				iumax = Math.min(iumax, iumin + MAX_WEIGHT_LENGTH_U - 1);

				float[] weightU = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

				if ( iumax == iumin){
					weightU[0] = coordRight - coordLeft;
				}else{
					weightU[0] = iumin + 1 - coordLeft;
					weightU[iumax - iumin] = coordRight - iumax;
				};


				coordBottom = (float) ( point2d.getElement(1) / point2d.getElement(2) + 0.5 );
				point2d.add(fullVoxelMoveZ);
				coordTop = (float) ( point2d.getElement(1) / point2d.getElement(2) + 0.5 );
				coordStep = coordTop - coordBottom;


				for ( int k = 0; k < maxK; k++ ){

					double dsz0 = Math.abs(k*dz-offsetZ-cz); 

					if ( coordBottom <= 0 ){
						coordBottom = coordTop;
						coordTop = coordTop + coordStep;
						continue;
					}

					if ( coordTop >= maxV ){
						break;
					}

					ivmin = (int)coordBottom;
					ivmax = (int)coordTop;

					if (Debug2) {
						System.out.println( "iu: iumin= " + iumin + ", \t iumax=" + iumax );
						System.out.println( "iv: ivmin= " + ivmin + ", \t ivmax=" + ivmax );
					}

					float[] weightV = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

					ivmax = Math.min(ivmax, ivmin + MAX_WEIGHT_LENGTH_V - 1);			
					
					if ( ivmin == ivmax ){
						weightV[0] = coordTop - coordBottom;
					}else{
						weightV[0] = ivmin + 1 - coordBottom;
						weightV[ivmax - ivmin] = coordTop - ivmax;
					};

					amplitude = (float) ( Math.sqrt(dsz0*dsz0 + dsxy0_sqr) / ds0);

					if (Debug2){
						int[] detSize = projImage.getSize();
						int[] volSize = volImage.getSize();
						System.out.println("Detector Size: " + detSize[0] + " X " + detSize[1] + " X " + detSize[2] );
						System.out.println("Volume Size: "+ volSize[0] + " X " + volSize[1] + " X " + volSize[2] );
						for (int u = 0 ; u <detSize[1]; u++ ){
							System.out.println("u=" + u );
							for (int v = 0; v < detSize[2]; v++){
								System.out.print(projImage.getAtIndex(0, u, v) + "\t");
							}
							System.out.println();
						}
					}

					float tempVal = volImage.getAtIndex(i, j, k) * amplitude ;

					for ( int iu = iumin, iiu = 0; iu <= iumax; iu++, iiu++ ){
						float temp = tempVal * weightU[iiu];
						for (int iv = ivmin, iiv = 0; iv <= ivmax; iv++, iiv++){
							projImage.addAtIndex(ip, iu, iv, temp * weightV[iiv] );
						} //iv
					} //iu

					coordBottom = coordTop;
					coordTop = coordTop + coordStep;

				} //k
			}//j		
		}//i
	}

	protected void distanceDrivenBackView( Grid3D projImage, Grid3D volImage, Projection proj, final int ip ){
		SimpleMatrix mat = proj.computeP();
		SimpleVector cameraCenter = proj.computeCameraCenter();

		// buffer have move of voxel
		SimpleVector halfVoxelMoveX = mat.getCol(0);
		halfVoxelMoveX.multiplyBy(dx/2);
		SimpleVector halfVoxelMoveY = mat.getCol(1);
		halfVoxelMoveY.multiplyBy(dy/2);
		SimpleVector fullVoxelMoveZ = mat.getCol(2);
		fullVoxelMoveZ.multiplyBy(dz);

		SimpleVector point3d = new SimpleVector(4);
		point3d.setElementValue(3, 1);

		SimpleVector point2d, point2dMinus, point2dPlus;

		double cx = cameraCenter.getElement(0);
		double cy = cameraCenter.getElement(1);
		double cz = cameraCenter.getElement(2);
		double dsx0, dsy0, dsxy0_sqr;
		float coordLeft, coordRight, coordBottom, coordTop, coordStep;
		double ds0;
		float amplitude;
		int iumin, iumax, ivmin, ivmax;

		if (Debug1) {
			System.out.println( "Camera Center: " + cx + ", " + cy);
			System.out.println( "Voxel Spaceing: " + dx + ", " + dy);
			System.out.println( "Projection matrix: ");
			printSimpleMatrix(mat);
			System.out.println( "Half voxel moves: ");
			printSimpleVector(halfVoxelMoveX);
			printSimpleVector(halfVoxelMoveY);
		} 

		for (int i = 0; i < maxI; i++){

			point3d.setElementValue(0, i*dx - offsetX  );

			for (int j = 0; j < maxJ; j++){
				
				point3d.setElementValue(1, j*dy - offsetY );
				
				point3d.setElementValue(2, -dz/2 - offsetZ);
				point2d = SimpleOperators.multiply(mat, point3d);

				dsx0 = Math.abs(point3d.getElement(0) - cx);
				dsy0 = Math.abs(point3d.getElement(1) - cy);
				dsxy0_sqr = dsx0*dsx0 + dsy0*dsy0;

				if (Debug1){
					System.out.println("dsx0="+ dsx0 + ", dsy0=" + dsy0 + ", dsxy0_sqr= " + dsxy0_sqr);
				}

				if ( dsy0 > dsx0){
					ds0 = dsy0;
					point2dMinus = SimpleOperators.subtract(point2d, halfVoxelMoveX);
					point2dPlus = SimpleOperators.add(point2d, halfVoxelMoveX);					
				}else{
					ds0 = dsx0;
					point2dMinus = SimpleOperators.subtract(point2d, halfVoxelMoveY);
					point2dPlus = SimpleOperators.add(point2d, halfVoxelMoveY);
				}

				coordLeft =  (float) ( point2dMinus.getElement(0) / point2dMinus.getElement(2) + 0.5 );
				coordRight = (float) ( point2dPlus.getElement(0) / point2dPlus.getElement(2) + 0.5 );

				if ( coordLeft > coordRight ){
					float temp = coordRight;
					coordRight = coordLeft;
					coordLeft = temp;	
				}

				coordLeft = Math.max(coordLeft, 0);
				coordRight = Math.min(coordRight, maxU);

				if (coordLeft >= coordRight){
					continue;
				}

				iumin = (int)coordLeft;
				iumax = (int)coordRight;
				iumax = Math.min(iumax, maxU-1);

				iumax = Math.min(iumax, iumin + MAX_WEIGHT_LENGTH_U - 1);

				float[] weightU = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

				if ( iumax == iumin){
					weightU[0] = coordRight - coordLeft;
				}else{
					weightU[0] = iumin + 1 - coordLeft;
					weightU[iumax - iumin] = coordRight - iumax;
				};


				coordBottom = (float) ( point2d.getElement(1) / point2d.getElement(2) + 0.5 );
				point2d.add(fullVoxelMoveZ);
				coordTop = (float) ( point2d.getElement(1) / point2d.getElement(2) + 0.5 );
				coordStep = coordTop - coordBottom;


				for ( int k = 0; k < maxK; k++ ){

					double dsz0 = Math.abs(k*dz-offsetZ-cz); 

					if ( coordBottom <= 0 ){
						coordBottom = coordTop;
						coordTop = coordTop + coordStep;
						continue;
					}

					if ( coordTop >= maxV ){
						break;
					}

					ivmin = (int)coordBottom;
					ivmax = (int)coordTop;

					if (Debug2) {
						System.out.println( "iu: iumin= " + iumin + ", \t iumax=" + iumax );
						System.out.println( "iv: ivmin= " + ivmin + ", \t ivmax=" + ivmax );
					}

					float[] weightV = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

					ivmax = Math.min(ivmax, ivmin + MAX_WEIGHT_LENGTH_V - 1);			
					
					if ( ivmin == ivmax ){
						weightV[0] = coordTop - coordBottom;
					}else{
						weightV[0] = ivmin + 1 - coordBottom;
						weightV[ivmax - ivmin] = coordTop - ivmax;
					};

					amplitude = (float) ( Math.sqrt(dsz0*dsz0 + dsxy0_sqr) / ds0);

					if (Debug2){
						int[] detSize = projImage.getSize();
						int[] volSize = volImage.getSize();
						System.out.println("Detector Size: " + detSize[0] + " X " + detSize[1] + " X " + detSize[2] );
						System.out.println("Volume Size: "+ volSize[0] + " X " + volSize[1] + " X " + volSize[2] );
						for (int u = 0 ; u <detSize[1]; u++ ){
							System.out.println("u=" + u );
							for (int v = 0; v < detSize[2]; v++){
								System.out.print(projImage.getAtIndex(0, u, v) + "\t");
							}
							System.out.println();
						}
					}

					float tempVal = 0.0f;

					for ( int iu = iumin, iiu = 0; iu <= iumax; iu++, iiu++ ){
						float sum = 0.0f;
						for (int iv = ivmin, iiv = 0; iv <= ivmax; iv++, iiv++){
							sum += weightV[iiv] * projImage.getAtIndex(ip, iu, iv);
						} //iv
						tempVal += sum * weightU[iiu];
					} //iu

					tempVal = tempVal * amplitude;
					volImage.addAtIndex(i, j, k, tempVal);

					//volImage.addAtIndex(i, j, k, 1);
					
					//update for next voxel
					coordBottom = coordTop;
					coordTop = coordTop + coordStep;

				} //k
			} //j
		} //i

	}

	@Override
	public String getBibtexCitation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMedlineCitation() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getToolName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void iterativeReconstruct() throws Exception {
		forwardproject( projectionViews, volumeImage);
		// TODO Auto-generated method stub
		
	}


}
/*
 * Copyright (C) 2010-2014 Meng Wu
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
*/
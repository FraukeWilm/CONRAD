package edu.stanford.rsl.conrad.utils;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.text.NumberFormat;
import java.util.HashMap;


public abstract class FloatArrayUtil {

	private static HashMap<Integer, float[]> arrayBuffer = new HashMap<Integer, float[]>();
	private static int max = -1;

	/**
	 * Stores an array for later visualization at index imageNumber
	 * @param imageNumber the number
	 * @param array the array
	 */
	public static void saveForVisualization(int imageNumber, float [] array){
		arrayBuffer.put(new Integer(imageNumber), array);
		if (imageNumber > max) max = imageNumber;
	}

	/**
	 * Performs a 1-D convolution of the input array with the kernel array.<BR>
	 * New array will be only of size <br>
	 * <pre>
	 * output.lenght = input.length - (2 * (kernel.length/2));
	 * </pre>
	 * (Note that integer arithmetic is used here)<br>
	 * @param input the array to be convolved
	 * @param kernel the kernel
	 * @return the output array.
	 */
	public static float [] convolve(float [] input, float [] kernel){
		int offset = ((kernel.length) / 2);
		float [] revan = new float [input.length - (2* offset)];
		float weightSum = 0;
		for (int j = 0; j < kernel.length; j++){
			weightSum += kernel[j];
		}
		if (weightSum == 0) weightSum = 1; 
		for (int i = offset; i < input.length-offset;i++){
			float sum = 0;
			for (int j = -offset; j <= offset; j++){
				sum += kernel[offset+j] * input[i+j];
			}
			sum /= weightSum;
			revan [i-offset] = sum;
		}
		return revan;
	}

	/**
	 * Displays the arrays stored with "saveForVisualization" as ImagePlus.
	 * @param title the title of the ImagePlus
	 * @return the reference to the ImagePlus
	 * 
	 * @see #saveForVisualization(int imageNumber, float [] array)
	 */
	public static ImagePlus visualizeBufferedArrays(String title){
		if (max >= 0) {
			int height = max+1;
			int width = arrayBuffer.get(new Integer(0)).length;
			FloatProcessor flo = new FloatProcessor(width, height);
			for (int j = 0; j <= max; j++){
				float [] array = arrayBuffer.get(new Integer(j));
				for (int i = 0; i < array.length; i++){
					flo.putPixelValue(i, max - j, array[i]);
				}
			}
			return VisualizationUtil.showImageProcessor(flo, title);
		} else {
			return null;
		}
	}

	/**
	 * Forces an complex float array to be symmetric. Left / first half of the array is mirrored to the right / second half
	 * @param array the complex array
	 */
	public static void forceSymmetryComplexDoubleArray(float [] array){
		// Force Symmetry
		int width = array.length / 2;
		for (int i = 0; i < (width/2); i++){
			array[(width) + (2 * i)] = array[(width)-(2*i)];
			array[(width) + (2 * i)+1] = array[(width)-(2*i)+1];
		}
	}

	/**
	 * tests if any of the values in the given array is NaN
	 * @param array
	 * @return true if the array contains at least one entry with NaN
	 */
	public static boolean isNaN(float [] array){
		boolean revan = true;
		for (int i = 0; i < array.length; i++){
			if (revan && Double.isNaN(array[i])) revan = false;
		}
		return !revan;
	}

	/**
	 * Forces a real float array to be symmetric. Left / first half of the array is mirrored to the right / second half
	 * @param array the real array
	 */
	public static void forceSymmetryRealArray(float [] array){
		// Force Symmetry
		int width = array.length;
		for (int i = 0; i < (width/2); i++){
			array[(width/2) + (i)] = array[(width/2)-(i)];
		}
	}

	/**
	 * returns the closest index in the array to the given value
	 * @param x the value
	 * @param array the array
	 * @return the desired index in the array
	 */
	public static int findClosestIndex(float x, float [] array){
		float min = (float) Double.MAX_VALUE;
		int pos = 0;
		for (int i = 0; i < array.length; i++){
			float dist = Math.abs(array[i]-x);
			if (dist < min){
				min = dist;
				pos = i;
				if (dist == 0) break;
			}
		}
		return pos;
	}
	
	
	/**
	 * computes the covariance between two arrays
	 *
	 * @param x the one array
	 * @param y the other array
	 * @return the correlation
	 */
	public static float covarianceOfArrays(float [] x, float [] y){
		float meanX = computeMean(x);
		float meanY = computeMean(y);
		float covariance = 0;
		for (int i=0; i< x.length; i++){
			covariance += ((x[i] - meanX) * (y[i] - meanY)) / x.length;
		}
		return covariance / x.length;
	}

	/**
	 * computes the correlation coefficient between two arrays after Pearson
	 *
	 * @param x the one array
	 * @param y the other array
	 * @return the correlation
	 */
	public static float correlateArrays(float [] x, float [] y){
		float meanX = computeMean(x);
		float meanY = computeMean(y);
		float covariance = 0;
		float varX = 0, varY = 0;
		for (int i=0; i< x.length; i++){
			varX += Math.pow(x[i] - meanX, 2) / x.length;
			varY += Math.pow(y[i] - meanY, 2) / y.length;
			covariance += ((x[i] - meanX) * (y[i] - meanY)) / x.length;
		}
		if (varX == 0) varX = (float) CONRAD.SMALL_VALUE;
		if (varY == 0) varY = (float) CONRAD.SMALL_VALUE;
		return (float) (covariance / (Math.sqrt(varX) * Math.sqrt(varY)));
	}
	
	/**
	 * computes the correlation coefficient between two arrays after Pearson
	 *
	 * @param x the one array
	 * @param y the other array
	 * @return the correlation
	 */
	public static float computeSSIMArrays(float [] x, float [] y){
		float meanX = computeMean(x);
		float meanY = computeMean(y);
		float covariance = 0;
		float varX = 0, varY = 0;
		for (int i=0; i< x.length; i++){
			varX += Math.pow(x[i] - meanX, 2) / x.length;
			varY += Math.pow(y[i] - meanY, 2) / y.length;
			covariance += ((x[i] - meanX) * (y[i] - meanY)) / x.length;
		}
		if (varX == 0) varX = (float) CONRAD.SMALL_VALUE;
		if (varY == 0) varY = (float) CONRAD.SMALL_VALUE;
		return (float) ((2*covariance *2*meanX*meanY) / ((varX+varY) * (Math.pow(meanY, 2) + Math.pow(meanX, 2))));
	}

	
	/**
	 * computes the concordance correlation coefficient between two arrays
	 *
	 * @param x the one array
	 * @param y the other array
	 * @return the correlation
	 */
	public static float concordanceCorrelateArrays(float [] x, float [] y){
		float meanX = computeMean(x);
		float meanY = computeMean(y);
		float covariance = 0;
		float varX = 0, varY = 0;
		for (int i=0; i< x.length; i++){
			varX += Math.pow(x[i] - meanX, 2) / x.length;
			varY += Math.pow(y[i] - meanY, 2) / y.length;
			covariance += ((x[i] - meanX) * (y[i] - meanY)) / x.length;
		}
		if (varX == 0) varX = (float) CONRAD.SMALL_VALUE;
		if (varY == 0) varY = (float) CONRAD.SMALL_VALUE;
		return (float) ((2* covariance) / (varX + varY + Math.pow(meanX - meanY, 2)));
	}
	
	/**
	 * computes the mean square error of array x to array y
	 *
	 * @param x the one array
	 * @param y the other array
	 * @return the mean square error
	 */
	public static float computeMeanSquareError(float [] x, float [] y){
		float sum = 0;
		for (int i=0; i< x.length; i++){
			sum += Math.pow(x[i] - y[i], 2);
		}
		return sum / x.length;
	}
	
	/**
	 * computes the root mean square error of array x to array y
	 *
	 * @param x the one array
	 * @param y the other array
	 * @return the root mean square error.
	 */
	public static float computeRootMeanSquareError(float [] x, float [] y){
		return (float) Math.sqrt(computeMeanSquareError(x, y));
	}

	public static void suppressCenter(float [] weights, int threshold){
		for (int i = 1; i < weights.length; i++){
			if (!((i < threshold) || ((weights.length - i) < threshold))) {
				weights[i] = (weights[threshold] + weights[weights.length-threshold]) /2;
			}
		}
	}

	/**
	 * Removes outliers from the array which differ more than threshold from the last value.
	 * @param weights the weight
	 * @param threshold the threshold
	 */
	public static void removeOutliers(float [] weights, float threshold){
		for (int i = 1; i < weights.length; i++){
			if (Math.abs(weights[i] - weights[i - 1]) > threshold) weights[i] = weights[i - 1] + threshold * Math.signum(- weights[i - 1] + weights[i]);
		}
	}

	/**
	 * computes the mean of the array "values" on the interval [start, end].
	 * @param values the array
	 * @param start the start index
	 * @param end the end index
	 * @return the mean value
	 */
	public static float computeMean (float [] values, int start, int end){
		float revan = 0;
		for(int i = start; i <= end; i++){
			revan += values[i];
		}
		revan /= end - start + 1;
		return revan;
	}

	/**
	 * Computes the average increment of the array
	 * @param array the array
	 * @return the average increment
	 */
	public static float computeAverageIncrement(float [] array){
		float increment = 0;
		for (int i = 1; i < array.length; i++){
			float value = Math.abs(array[i-1] - array[i]);
			if (value > 180) { 
				value -= 360;
				value = Math.abs(value);
			}
			increment += value;
		}
		return increment / (array.length - 1);
	}

	/**
	 * Performs mean filtering of the array.
	 * @param weights the array
	 * @param context the context to be used for smoothing (from -context/2 to context/2)
	 * @return the smoothed array
	 */
	public static float[] meanFilter(float [] weights, int context){
		float meanFiltered [] = new float [weights.length];
		float mean = 0;
		for (int i = 0; i < weights.length; i++) {
			if (i > context / 2){
				mean -= weights[i - (context/2)];
			} if (i < (weights.length -1) - (context / 2)){
				mean += weights[i + (context /2)];
			}
			if (i < context/2){
				meanFiltered[i] = computeMean(weights, 0, i);
			} else if ((i+ context/2) >= weights.length){
				meanFiltered[i] = computeMean(weights, i, weights.length - 1);
			} else {
				meanFiltered[i] = computeMean(weights, i - context/2, i + context/2);
			}
		}
		return meanFiltered;
	}

	/**
	 * Gaussian smoothing of the elements of the array "weights"
	 * @param weights the array
	 * @param sigma the standard deviation
	 * @return the smoothed array
	 */
	public static float[] gaussianFilter(float [] weights, float sigma){
		float meanFiltered [] = new float [weights.length];
		int center = (int) Math.floor(sigma * 1.5) + 1;
		float kernel [] = new float [(int) Math.ceil(center*2 +1)];
		float kernelSum = 0;
		for (int j = 0; j < (center*2) + 1; j++){
			kernel[j] = (float) (Math.exp(-0.5 * Math.pow((center-j) / sigma, 2))/sigma/Math.sqrt(2*Math.PI));
			kernelSum += kernel[j];
		}
		for (int i =0; i< meanFiltered.length; i++){
			float sum = 0;		
			for (int j = -center; j <= center; j++){
				// Out of bounds at left side
				if (i+j < 0)
					sum += kernel[j+center] * weights[0];
				// Out of bounds at right side
				else if (i+j > weights.length-1)
					sum += kernel[j+center] * weights[weights.length-1];
				// Convolution applied inside the valid part of the signal
				else
					sum += kernel[j+center] * weights[i+j];
			}
			meanFiltered[i] = sum / kernelSum;
		}
		return meanFiltered;
	}

	/**
	 * Computes the standard deviation given an array and its mean value
	 * @param array the array
	 * @param mean the mean value of the array
	 * @return the standard deviation
	 */
	public static float computeStddev(float[] array, float mean){
		float stddev = 0;
		for (int i = 0; i < array.length; i++){	
			stddev += Math.pow(array[i] - mean, 2);
		}
		return (float) Math.sqrt(stddev / array.length);
	}
	
	public static float computeStddev(float[] array, float mean, int start, int end){
		float stddev = 0;
		for (int i = start; i < end; i++){	
			stddev += Math.pow(array[i] - mean, 2);
		}
		return (float) Math.sqrt(stddev / (end-start));
	}

	/**
	 * Computes the mean value of a given array
	 * @param array the array
	 * @return the mean value as float
	 */
	public static float computeMean(float[] array){
		float mean = 0;
		for (int i = 0; i < array.length; i++){	
			mean += array[i];
		}
		return mean / array.length;
	}

	/**
	 * Returns the minimal and the maximal value in a given array
	 * @param array the array
	 * @return an array with minimal and maximal value
	 */
	public static float [] minAndMaxOfArray(float [] array){
		float [] revan = new float [2];
		revan[0] = Float.MAX_VALUE;
		revan[1] = -Float.MAX_VALUE;
		for (int i = 0; i < array.length; i++){
			if (array[i] < revan[0]) {
				revan[0] = array[i];
			}
			if (array[i] > revan[1]) {
				revan[1] = array[i];
			}
		}
		return revan;		
	}

	/**
	 * Returns the minimal value in a given array
	 * @param array the array
	 * @return the minimal value
	 */
	public static float minOfArray(float [] array){
		float min = Float.MAX_VALUE;
		for (int i = 0; i < array.length; i++){
			if (array[i] < min) {
				min = array[i];
			}

		}
		return min;		
	}


	/**
	 * forces monotony onto the input array
	 * @param array the array
	 * @param rising force rising monotony?
	 */
	protected void forceMonotony(float [] array, boolean rising){
		float lastValid = array[0];
		for (int i=0;i< array.length; i++){
			float value = array[i];
			if (rising) {
				if (value < lastValid) {
					value = lastValid;
				} else {
					lastValid = value;
				}
			} else {
				if (value > lastValid) {
					value = lastValid;
				} else {
					lastValid = value;
				}
			}
			array[i] = value;
		}
	}

	/**
	 * Adds one array to the first array
	 * @param sum the first array
	 * @param toAdd the array to add
	 */
	public static void add(float[] sum, float[] toAdd) {
		for (int i =0; i < sum.length; i++){
			sum[i] += toAdd[i];
		}
	}

	/**
	 * Adds a constant to the first array
	 * @param sum the first array
	 * @param toAdd the constant to add
	 */
	public static float [] add(float[] sum, float toAdd) {
		for (int i =0; i < sum.length; i++){
			sum[i] += toAdd;
		}
		return sum;
	}

	/**
	 * Divides all entries of array by divident.
	 * @param array the array
	 * @param divident the number used for division.
	 */
	public static float [] divide(float[] array, float divident) {
		for (int i =0; i < array.length; i++){
			array[i] /= divident;
		}
		return array;
	}

	/**
	 * Multiplies all entries of array by factor.
	 * @param array the array
	 * @param factor the number used for multiplication.
	 */
	public static float [] multiply(float[] array, float factor) {
		for (int i =0; i < array.length; i++){
			array[i] *= factor;
		}
		return array;
	}

	/**
	 * Multiplies all entries of the two arrays element by element.<bR>
	 * Works in place and overwrites array.
	 * @param array the array
	 * @param array2 the other array.
	 */
	public static void multiply(float[] array, float[] array2) {
		for (int i =0; i < array.length; i++){
			array[i] *= array2[i];
		}
	}

	/**
	 * Uses Math.exp() on all elements of the array
	 * Works in place and overwrites array.
	 * @param array the array
	 */
	public static void exp(float[] array) {
		for (int i =0; i < array.length; i++){
			array[i] = (float) Math.exp(array[i]);
		}
	}

	/**
	 * Divides all entries of the two arrays element by element.<bR>
	 * Works in place and overwrites array.
	 * @param array the array
	 * @param divident the other array.
	 */
	public static float [] divide(float[] array, float[] divident) {
		for (int i =0; i < array.length; i++){
			array[i] /= divident[i];
		}
		return array;
	}

	/**
	 * Uses Math.log() on all elements of the array
	 * Works in place and overwrites array.
	 * @param array the array
	 */
	public static void log(float[] array) {
		for (int i =0; i < array.length; i++){
			array[i] = (float) Math.log(array[i]);
		}
	}

	/**
	 * Prints the contents of the float array on standard out.
	 * @param array
	 * @param nf the NumberFormat
	 */
	public static void print(float[] array, NumberFormat nf) {
		System.out.print("[");
		for (int i =0; i < array.length; i++){
			System.out.print(" " + nf.format(array[i]));
		}
		System.out.println(" ]");
	}

	/**
	 * Prints the array on standard out and denotes the arrays name.
	 * @param name the name
	 * @param array the array
	 * @param nf the number format
	 */
	public static void print(String name, float[] array, NumberFormat nf) {
		System.out.print(name + " = ");
		print(array, nf);
	}

	/**
	 * Prints the array on standard out. Uses NumberFormat.getInstance() for number formatting
	 * @param name the name 
	 * @param array the array
	 */
	public static void print(String name, float [] array){
		print(name, array, NumberFormat.getInstance());
	}

	/**
	 * Prints the array on standard out. Uses NumberFormat.getInstance() for number formatting
	 * @param array the array
	 */
	public static void print (float [] array){
		print(array, NumberFormat.getInstance());
	}

	/**
	 * calls Math.pow for each element of the array
	 * @param array
	 * @param exp the exponent.
	 * @return reference to the input array
	 */
	public static float [] pow(float[] array, float exp) {
		for (int i =0; i < array.length; i++){
			array[i] = (float) Math.pow((double)array[i], exp);
		}
		return array;
	}

	public static float[] min(float [] array, float min) {
		for (int i =0; i < array.length; i++){
			array[i] = Math.min(array[i], min);
		}
		return array;
	}
	
	/**
	 * Converts the array to a String representation. Calls toString(array, " ").
	 * @param array the array
	 * @return the String representation
	 * @see #toString(float[],String)
	 */
	public static String toString(float [] array){
		return toString(array, " ");
	}

	/**
	 * Converts the array to a String representation. delimiter is used to connect the elements of the array.
	 * @param array the array
	 * @param delimiter the delimiter
	 * @return the String representation
	 */
	public static String toString(float[] array, String delimiter) {
		String revan = array[0] + delimiter;
		for(int i=1;i<array.length -1;i++){
			revan += array[i] + delimiter;
		}
		revan += array[array.length-1];
		return revan;
	}

}
/*
 * Copyright (C) 2010-2014  Andreas Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
*/
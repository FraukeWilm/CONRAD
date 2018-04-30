package edu.stanford.rsl.conrad.volume3d.operations;

public class InitializeLowPass extends VoxelOperation {

	private float[] fMax;
	private float[] fDelta;
	private float lpUpper;


	@Override
	protected void performVoxelOperation() {
		float r_abs = 0;

		int dim_loop = 0;

		float pos = -fMax[dim_loop] + indexX * fDelta[dim_loop];
		r_abs += pos * pos;
		dim_loop=1;
		pos = -fMax[dim_loop] + indexY * fDelta[dim_loop];
		r_abs += pos * pos;
		dim_loop=2;
		pos = -fMax[dim_loop] + indexZ * fDelta[dim_loop];
		r_abs += pos * pos;

		r_abs = (float) Math.sqrt(r_abs);

		if (r_abs <= lpUpper) {
			float tmp=(float) Math.cos(Math.PI*r_abs/(2*lpUpper));
			vol.data[indexX][indexY][indexZ] = (tmp*tmp);
		} else
			vol.data[indexX][indexY][indexZ] = 0;


	}

	@Override
	public ParallelVolumeOperation clone() {
		InitializeLowPass clone = new InitializeLowPass();
		clone.fMax = fMax;
		clone.fDelta = fDelta;
		clone.lpUpper = lpUpper;
		return clone;
	}

	public float[] getfMax() {
		return fMax;
	}

	public void setfMax(float[] fMax) {
		this.fMax = fMax;
	}

	public float[] getfDelta() {
		return fDelta;
	}

	public void setfDelta(float[] fDelta) {
		this.fDelta = fDelta;
	}

	public float getLpUpper() {
		return lpUpper;
	}

	public void setLpUpper(float lpUpper) {
		this.lpUpper = lpUpper;
	}


}
/*
 * Copyright (C) 2010-2014  Andreas Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
*/
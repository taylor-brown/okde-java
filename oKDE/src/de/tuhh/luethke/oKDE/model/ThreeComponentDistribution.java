package de.tuhh.luethke.oKDE.model;

import org.ejml.simple.SimpleMatrix;

import de.tuhh.luethke.oKDE.Exceptions.TooManyComponentsException;

public class ThreeComponentDistribution extends MultipleComponentDistribution{
	private final static int NO_OF_COMPONENTS = 3;
	
	public ThreeComponentDistribution(double[] weights, SimpleMatrix[] means, SimpleMatrix[] covariances, SimpleMatrix bandwidth) throws TooManyComponentsException {
		super(weights, means, covariances, bandwidth);
		// check number of components
		if (!(weights.length == NO_OF_COMPONENTS & means.length == NO_OF_COMPONENTS & covariances.length == NO_OF_COMPONENTS))
			throw new TooManyComponentsException();
	}
	
}
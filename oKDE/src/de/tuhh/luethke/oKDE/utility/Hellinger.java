package de.tuhh.luethke.oKDE.utility;

import java.util.ArrayList;
import java.util.List;

import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

import de.tuhh.luethke.oKDE.Exceptions.EmptyDistributionException;
import de.tuhh.luethke.oKDE.Exceptions.TooManyComponentsException;
import de.tuhh.luethke.oKDE.model.OneComponentDistribution;
import de.tuhh.luethke.oKDE.model.SampleDist;
import de.tuhh.luethke.oKDE.model.ThreeComponentDistribution;
import de.tuhh.luethke.oKDE.model.TwoComponentDistribution;

public class Hellinger {

	private static final double MIN_TOL = 1e-5;

	public static double calculateUnscentedHellingerDistance(OneComponentDistribution dist1, TwoComponentDistribution dist2) throws Exception {
		ThreeComponentDistribution dist0 = mergeSampleDists(dist1, dist2, 0.5, 0.5);
		// TODO: remove components with negative weights from dist0
		
		
		List<SigmaPoint> sigmaPoints = getAllSigmaPoints(dist0, 3);
		System.out.println("sigmapoints: "+sigmaPoints.size());
		ArrayList<SimpleMatrix> points = new ArrayList<SimpleMatrix>();
		ArrayList<Double> weights = new ArrayList<Double>();
		for(SigmaPoint p : sigmaPoints){
			points.add(p.getmPointVecor());
			weights.add(p.getmWeight());
			System.out.println(p.getmPointVecor()+" - "+p.getmWeight());
		}
		
		List<Double> dist1Ev = dist1.evaluate(points, false, false);
		List<Double> dist2Ev = dist2.evaluate(points, true, false);
		dist1Ev = setNegativeValuesToZero(dist1Ev);
		dist2Ev = setNegativeValuesToZero(dist2Ev);
		
		List<Double> dist0Ev = dist0.evaluate(points, true, false);
		SimpleMatrix mat0 = doubleListToMatrix(dist0Ev);
		/*
			g = (sqrt(pdf_f1)- sqrt(pdf_f2)).^2 ;
			H = sqrt(abs(sum(W.*g./pdf_f0)/2)) ; 
			H = H;
		 */
		SimpleMatrix mat1 = doubleListToMatrix(dist1Ev);
		SimpleMatrix mat2 = doubleListToMatrix(dist2Ev);
		SimpleMatrix weightsMatrix = doubleListToMatrix(weights);
		SimpleMatrix g = MatrixOps.elemPow( (MatrixOps.elemSqrt(mat1).minus(MatrixOps.elemSqrt(mat2))), 2 );
		SimpleMatrix tmp = new SimpleMatrix(weightsMatrix);
		tmp = weightsMatrix.elementMult(g);
		CommonOps.elementDiv(tmp.getMatrix(), mat0.getMatrix(), tmp.getMatrix());
		double val = tmp.elementSum();
		double H = Math.sqrt(Math.abs(val/2));
		System.out.println("Hellinger dist: "+H);
		return H;
	}
	
	private static SimpleMatrix doubleListToMatrix(List<Double> valueList){
		SimpleMatrix m = new SimpleMatrix(1,valueList.size());
		for(int i=0; i<valueList.size(); i++)
			m.set(0, i, valueList.get(i));
		return m;
	}
	
	private static List<Double> setNegativeValuesToZero(List<Double> valueList){
		for(int i=0; i<valueList.size(); i++) {
			if(valueList.get(i) < 0)
				valueList.set(i,0d);
		}
		return valueList;
	}
	private static List<Double> elementSqrt(List<Double> valueList){
		for(int i=0; i<valueList.size(); i++) {
			valueList.set(i,Math.sqrt(valueList.get(i)));
		}
		return valueList;
	}
	
	private static ThreeComponentDistribution mergeSampleDists(OneComponentDistribution dist1, TwoComponentDistribution dist2, double w1, double w2) {
		SimpleMatrix[] means = new SimpleMatrix[3];
		int  i = 0;
		for(;i<dist2.getSubMeans().length; i++){
			means[i] = dist2.getSubMeans()[i];
		}
		means[i] = dist1.getGlobalMean();

		SimpleMatrix[] covs = new SimpleMatrix[3];
		i = 0;
		for(;i<dist2.getSubCovariances().length; i++){
			covs[i] = dist2.getSubCovariances()[i];
		}
		covs[i] = dist1.getGlobalCovariance();
		
		double[] weights = new double[3];
		i = 0;
		for(;i<dist2.getSubWeights().length; i++){
			weights[i] = dist2.getSubWeights()[i] * w2;
		}
		weights[i] = dist1.getWeightSum() * w1;

		ThreeComponentDistribution dist = null;
		try {
			dist = new ThreeComponentDistribution(weights, means, covs);
		} catch (TooManyComponentsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return dist;
	}

	public static List<SigmaPoint> getAllSigmaPoints(ThreeComponentDistribution distribution, int max) throws Exception {
		ArrayList<SigmaPoint> sigmaPoints = new ArrayList<SigmaPoint>();
		int noOfComponents = distribution.getSubMeans().length;
		int dim = distribution.getSubMeans()[0].numRows();
		int k = max - dim;
		int noOfSigmaPoints;
		if (k != 0)
			noOfSigmaPoints = 2 * dim + 1;
		else
			noOfSigmaPoints = 2 * dim;
		/*
		 * for(int i=0; i<(noOfSigmaPoints*noOfComponents); i++){ X.add(new
		 * SimpleMatrix(dim,0)); }
		 */
		
		ArrayList<Double> weights = new ArrayList<Double>();
		for (int i = 0; i < (2 * dim); i++) {
			weights.add(new Double(1d / (2 * ((double) dim + k))));
		}
		if (k != 0)
			weights.add(new Double((double) k / (double) (dim + k)));
		double sum = 0;
		for (Double d : weights) {
			sum += d;
		}
		if ((sum - 1) > MIN_TOL)
			throw new Exception("Weights in the unscented transform should sum to one!");

		for (int i = 0; i < noOfComponents; i++) {
			List<SimpleMatrix> x = getSigmaPoints(distribution.getSubMeans()[i], distribution.getSubCovariances()[i],
					noOfSigmaPoints, k);
			int count = 0;
			double componentWeight = distribution.getSubWeights()[i];
			for(SimpleMatrix m : x){
				sigmaPoints.add(new SigmaPoint(m, weights.get(count)*componentWeight, weights.get(count)));
				count++;
			}
		}

		

		return sigmaPoints;
	}

	/**
	 * Returns 2n+k sigma points starting with mean as the first point
	 * 
	 * @param mean
	 * @param cov
	 * @param no
	 * @param k
	 * @return
	 */
	private static List<SimpleMatrix> getSigmaPoints(SimpleMatrix mean, SimpleMatrix cov, int no, int k) {
		List<SimpleMatrix> resultVectors = new ArrayList<SimpleMatrix>();

		int n = cov.numRows();
		SimpleSVD svd = cov.svd(true);
		SimpleMatrix U = svd.getU();
		SimpleMatrix S = svd.getW();
		SimpleMatrix V = svd.getV();

		S = U.mult(MatrixOps.elemSqrt(S)).scale(Math.sqrt(n + k));

		for (int i = 0; i < S.numCols(); i++) {
			SimpleMatrix columnVector = S.extractVector(false, i);
			SimpleMatrix negColumnVector = S.extractVector(false, i).scale(-1);
			resultVectors.add(columnVector.plus(mean));
			resultVectors.add(negColumnVector.plus(mean));
		}
		if (k != 0)
			resultVectors.add(mean);

		return resultVectors;
	}
	
}

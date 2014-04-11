package com.jalil.munge;

import com.jalil.math.*;

public interface Regression<P extends Parameters, E extends Estimate> {

	public void importData(double[] yTrain, double[][] xTrain, double[] yCrossValid, double[][] xCrossValid);
	public void setOptimPars(int maxiter);
	public void setParameters(P pars);
	public double predict(E estimate, double[] xTest);
	public double[] predictDerivative(E estimate, double[] xTest) throws UnsupportedOperationException;
	public E stochasticLearn(boolean silent);
	public E batchLearn(boolean silent);
	public double generalizationError(E estimate);
	public <F extends EstimateFactory<P,E>> F getEstimateFactory();
}
package com.jalil.munge;

import com.jalil.math.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.lang.reflect.*;

public  class  RegressionAnalysis<R extends Regression<P, E>, P extends Parameters, E extends Estimate> {

	List<double[]> data;
	int ndependentVars;
	int[] order;
	double[][] yTrain;
	double[][] xTrain;
	double[][] yCrossValid;
	double[][] xCrossValid;
	double[] mu, sig;

	R model;
	List<E> estimates;


	public RegressionAnalysis(R model) {
		this.model = model;
	}

	public void loadTrainingData(List<double[]> data, int ndependentVars) {
		if(data.size() == 0)
			throw new IllegalArgumentException("Training data has no example.");
		int ncolumn = data.get(0).length;
		if(ndependentVars < 0 || ndependentVars >= ncolumn)
			throw new IllegalArgumentException("The number of dependent variables is invalid.");
		for (double[] example : data) {
			if(example.length != ncolumn)
				throw new IllegalArgumentException("The number of columns in training data doens't match.");
		}
		this.data = data;
		this.ndependentVars = ndependentVars;
		int n = ncolumn - ndependentVars;
		mu = new double[n];
		sig = new double[n];
		for (int j = 0; j < n; j++) {
			sig[j] = 1;
		}
	}

	public void partitionData(boolean shuffle) {
		partitionData(shuffle, 0.7);
	}	

	public void partitionData(boolean shuffle, double trainingFraction) {
		if(data == null)
			throw new RuntimeException("No data is loaded.");
		System.out.println("Partitioning started ...");
		int m = data.size();
		if(shuffle) {
			order = MyMath.sample(m, m);			
		} else {
			order = new int[m];
			for (int i = 0; i < m; i++)
				order[i] = i;
		}
		double[] row;
		int mTrain = (int) Math.floor(trainingFraction * m);
		yTrain = new double[mTrain][];
		xTrain = new double[mTrain][];
		for(int i = 0; i < mTrain; i++) {
			row = data.get(order[i]);
			yTrain[i] = Arrays.copyOfRange(row, 0, ndependentVars);
			xTrain[i] = Arrays.copyOfRange(row, ndependentVars, row.length);
		}			
		yCrossValid = new double[m - mTrain][];
		xCrossValid = new double[m - mTrain][];
		for(int i = mTrain; i < m; i++) {
			row = data.get(order[i]);
			yCrossValid[i - mTrain] = Arrays.copyOfRange(row, 0, ndependentVars);
			xCrossValid[i - mTrain] = Arrays.copyOfRange(row, ndependentVars, row.length);
		}
	}

	public void normalizeFeature() {
		if(data == null)
			throw new RuntimeException("No data is loaded.");
		System.out.println("Feature normalization started ...");
		int n = xTrain[0].length;
		int mTrain = yTrain.length;
		int mCrossValid = yCrossValid.length;
		for (int j = 0; j < n; j++) {
			mu[j] = 0;
			for (int i = 0; i < mTrain; i++)
				mu[j] += xTrain[i][j];
			mu[j] /= mTrain;
			sig[j] = 0;
			for (int i = 0; i < mTrain; i++) {
				xTrain[i][j] -= mu[j];
				sig[j] += xTrain[i][j] * xTrain[i][j];
			}
			sig[j] = Math.sqrt(sig[j] / mTrain);
			for (int i = 0; i < mCrossValid; i++)
				xCrossValid[i][j] -= mu[j];
			if(sig[j] > 0) {
				for (int i = 0; i < mTrain; i++)
					xTrain[i][j] /= sig[j];
				for (int i = 0; i < mCrossValid; i++)
					xCrossValid[i][j] /= sig[j];
			}
		}
	}

	public int[] getCrossValidationSet() {
		int mTrain = yTrain.length;
		int mCrossValid = yCrossValid.length;
		int[] crossValidSet = new int[mCrossValid];
		for (int i = mTrain; i < mTrain + mCrossValid; i++) {
			crossValidSet[i - mTrain] = order[i];
		}

		return crossValidSet;
	}

	public void setOptimPars(int maxiter, double alpha) {
		model.setOptimPars(maxiter, alpha);
	}

	public void learn(P[] parsList) {
		int mTrain = yTrain.length;
		int mCrossValid = yCrossValid.length;
		double[] yTrainOneDim = new double[mTrain];
		double[] yCrossValidOneDim = new double[mCrossValid];
		int npar = parsList.length;
		estimates = new LinkedList<E>();
		E estimateOneDim;
		double errorOneDim;
		E bestEstimateOneDim;
		double bestErrorOneDim;
		int bestParamOneDim;
		StringBuilder report = new StringBuilder();
		for (int d = 0; d < ndependentVars; d++) {
			System.out.println("Estimation of dependent variable #" + (d + 1));
			report.append("\nDependent var #" + (d + 1));
			for (int i = 0; i < mTrain; i++) {
				yTrainOneDim[i] = yTrain[i][d];
			}
			for (int i = 0; i < mCrossValid; i++) {
				yCrossValidOneDim[i] = yCrossValid[i][d];
			}
			report.append("\nParameters\tError");
			System.out.println("Parameters : " + parsList[0]);
			model.importData(yTrainOneDim, xTrain, yCrossValidOneDim, xCrossValid);
			model.setParameters(parsList[0]);
			bestEstimateOneDim = model.stochasticLearn(false);
			bestErrorOneDim = model.generalizationError(bestEstimateOneDim);
			bestParamOneDim = 0;
			report.append("\n" + parsList[0] + "\t" + bestErrorOneDim);
			for (int i = 1; i < npar; i++) {
				System.out.println("Parameters : " + parsList[i]);
				model.setParameters(parsList[i]);
				estimateOneDim = model.stochasticLearn(false);
				errorOneDim = model.generalizationError(estimateOneDim);
				report.append("\n" + parsList[i] + "\t" + errorOneDim);
				if(errorOneDim < bestErrorOneDim) {
					bestEstimateOneDim = estimateOneDim;
					bestErrorOneDim = errorOneDim;
					bestParamOneDim = i;
				}
			}
			estimates.add(bestEstimateOneDim);
			report.append("\n\nThe best accuracy is given by : " + parsList[bestParamOneDim]);
		}
		System.out.println(report.toString());
	}

	public double[] predict(double[] xExample) {
		int n = xExample.length;
		double[] xTest = new double[n];
		for (int j = 0; j < n; j++) {
			xTest[j] = xExample[j] - mu[j];
			if(sig[j] > 0)
				xTest[j] /= sig[j];
		}
		double[] prediction = new double[ndependentVars];
		for (int d = 0; d < ndependentVars; d++) {
			prediction[d] = model.predict(estimates.get(d), xTest);
		}

		return prediction;
	}

	public double[][] predictJacobean(double[] xExample) throws UnsupportedOperationException {
		int n = xExample.length;
		double[] xTest = new double[n];
		for (int j = 0; j < n; j++) {
			xTest[j] = xExample[j] - mu[j];
			if(sig[j] > 0)
				xTest[j] /= sig[j];
		}
		double[][] prediction = new double[ndependentVars][n];
		for (int d = 0; d < ndependentVars; d++) {
			prediction[d] = model.predictDerivative(estimates.get(d), xTest);
		}
		for (int j = 0; j < n; j++) {
			if(sig[j] > 0) {
				for (int d = 0; d < ndependentVars; d++)
					prediction[d][j] /= sig[j];
			}
		}

		return prediction;
	}

	public void saveEstimate(String filename) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		builder.append("mu: ");
		builder.append("[\n");
		for (double mean : mu) {
			builder.append(mean + " , ");
		}
		builder.delete(builder.length() - 2, builder.length());
		builder.append("\n]\n");
		builder.append(", ");
		builder.append("sig: ");
		builder.append("[\n");
		for (double sd : sig) {
			builder.append(sd + " , ");
		}
		builder.delete(builder.length() - 2, builder.length());
		builder.append("\n]\n");
		builder.append(", ");
		builder.append("estimates: ");
		builder.append("[\n");
		for (int d = 0; d < ndependentVars; d++) {
			builder.append("{\n");
			builder.append("dimension: " + (d + 1) + "\n");
			builder.append(", ");
			builder.append("data: ");
			builder.append(estimates.get(d).toString());
			builder.append("}\n");
			builder.append(", ");
		}
		builder.delete(builder.length() - 2, builder.length());
		builder.append("\n]\n");
		builder.append("}\n\n");
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write(builder.toString());
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {if(out != null) out.close();} catch(IOException e) {e.printStackTrace();}
		}
	}

	public void loadEstimate(String filename) {
		StringBuilder builder = new StringBuilder();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(filename));
			String line;
			while((line = in.readLine()) != null) {
				builder.append(line);
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {if(in != null) in.close();} catch(IOException e) {e.printStackTrace();}
		}
		String xml = builder.toString();
		Pattern mu_p = Pattern.compile("\\bmu:\\s*\\[(\\s*[-+]?\\d*(?:[.]\\d*)?(?:e[-+]?\\d+)?(?:\\s*,\\s*[-+]?\\d*(?:[.]\\d*)?(?:e[-+]?\\d+)?)*\\s*)\\]");
		Pattern sig_p = Pattern.compile("\\bsig:\\s*\\[(\\s*[-+]?\\d*(?:[.]\\d*)?(?:e[-+]?\\d+)?(?:\\s*,\\s*[-+]?\\d*(?:[.]\\d*)?(?:e[-+]?\\d+)?)*\\s*)\\]");
		Pattern estimates_p = Pattern.compile("\\bestimates:\\s*\\[\\s*\\{\\s*dimension:\\s*\\d+\\s*,\\s*data:\\s*\\[\\s*\\{[^\\[\\]]+\\}\\s*\\]\\s*\\}(\\s*,\\s*\\{\\s*dimension:\\s*\\d+\\s*,\\s*data:\\s*\\[\\s*\\{[^\\[\\]]+\\}\\s*\\]\\s*\\})*\\s*\\]");
		Matcher mu_m = mu_p.matcher(xml);
		int n = 0;
		estimates = new LinkedList<E>();
		if(mu_m.find()) {
			String[] muFields = mu_m.group(1).split(",");
			n = muFields.length;
			mu = new double[n];
			for (int j = 0; j < n; j++) {
				mu[j] = Double.parseDouble(muFields[j]);
			}
		}
		Matcher sig_m = sig_p.matcher(xml);
		if(sig_m.find()) {
			String[] sigFields = sig_m.group(1).split(",");
			if(n != sigFields.length)
				throw new IllegalArgumentException("mu and sig have different size.");
			sig = new double[n];
			for (int j = 0; j < n; j++) {
				sig[j] = Double.parseDouble(sigFields[j]);
			}			
		}
		Matcher estimates_m = estimates_p.matcher(xml);
		if(estimates_m.find()) {
			Pattern dimension_p = Pattern.compile("\\{\\s*dimension:\\s*\\d+\\s*,\\s*data:\\s*(\\[\\s*\\{[^\\[\\]]+\\}\\s*\\])\\s*\\}");
			Matcher dimension_m = dimension_p.matcher(estimates_m.group());
			EstimateFactory<P,E> factory = model.getEstimateFactory();
			ndependentVars = 0;
			while(dimension_m.find()) {
				ndependentVars++;
				estimates.add(factory.valueOf(dimension_m.group(1)));
			}
		}
	}

}
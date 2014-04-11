package com.jalil.munge;

public interface EstimateFactory<P extends Parameters, E extends Estimate> {

	public E createEstimate(int nfeature, P parameters);
	public  E valueOf(String estimateString);
		
}
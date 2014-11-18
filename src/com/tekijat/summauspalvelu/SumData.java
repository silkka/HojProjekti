package com.tekijat.summauspalvelu;
/**
 * S‰ilˆˆ summan ja sen alkioiden m‰‰r‰n. Kunkin metodin aikana luokka asetetaan synchronized tilaan,
 * jottei samanaista lukua ja kirjoitusta tapahtuisi.
 * @author Antti Peltola
 */
public class SumData {
	
	private int sum;
	private int numberOfsums;
	
	public SumData(){
		this.sum = 0;
		this.numberOfsums = 0;
	}
	/**
	 * Lis‰‰ summaan arvon value.
	 * @param value value != null
	 * @.post getSum() = OLD.getSum() + value
	 */
	public void addSum(int value){
		synchronized (this) {
			this.sum +=value;
			addNumberOfSums();
		}
	}
	/**
	 * Palauttaa senhetkisen summan.
	 * @return RETURN != null
	 */
	public int getSum(){
		synchronized (this) {
			return this.sum;
			
		}
	}
	/**
	 * Nostaa summattujen alkioiden m‰‰r‰‰ yhdell‰.
	 */
	private void addNumberOfSums(){
		synchronized (this) {
			this.numberOfsums++;
		}
		
	}
	/**
	 * Palauttaa summattujen alkioiden m‰‰r‰n.
	 * @return RETURN != null
	 */
	public int getNumberOfSums(){
		synchronized (this) {
			return numberOfsums;
		}
		
	}
}

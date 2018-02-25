package vision;

public class DerivativeOfVision extends GripMain{//fix later
	private static double lastMeasuredDistance;
	private static double changeOfRate;
	private static double derivative;
	private static double lastMeasuredTime;
	private static boolean notFirstTime = false;
	
	public double changeOfValue(double Distance, long currentTime) {
		
		if(notFirstTime) {
			
			changeOfRate = Distance - lastMeasuredDistance;
			derivative = (changeOfRate/(currentTime - lastMeasuredTime));// make into a named variable
			
		}
		else {
			notFirstTime = !notFirstTime;
			derivative = 0;
			
		}
		
		lastMeasuredDistance = Distance;
		lastMeasuredTime = currentTime;
		return derivative;
	}
}



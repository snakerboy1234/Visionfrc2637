package vision;

import java.awt.image.BufferedImage;
import java.lang.Math;

import org.opencv.core.Core;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.Scalar;

public class GripMain {
	
	/*
	 * These constants in the code are used in the mathematics
	 * for finding the blocks distance from the cube which are
	 * angular
	 */
	
	private static final double PI = Math.PI;
	private static final double RAD2DEG = 180.0 / PI;
	private static final double DEG2RAD = 1.0 / RAD2DEG;

	private static final double CUBE_DIAMETER_INCHES = 17;
	private static final double VIEW_ANGLE_DIAGONAL_DEGREES = 78;
	private static final double VIEW_ANGLE_DIAGONAL_RADIANS = VIEW_ANGLE_DIAGONAL_DEGREES * DEG2RAD;
	private static final double MAX_DERIVATIVE_THRESHOLD = 0.4;//more than twice 15 feet per second (inches/ms)

	private static final String roboNetworkName = "10.26.37.2";	//"172.22.11.2";
	private static final int visionPort = 2637;
	
	public static long startTime;
	public static long endTime;
	public static long timeTaken;
	
	@SuppressWarnings("unused")
	public static void main(String[] arg) {
		
		//This line of code loads the opencv library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		double legLenthIsosceles;
		double heightIsosceles;
		double hypotenuseSquared;
		double measX;
		double measY;
		double objRadius;
		double heading;
		double distance; 
		double derivativeDistance;
		
		int sizedFrameWidth = 480;
		int sizedFrameHeight = 270;
		int framesProcessed = 0;
		int smallBlobCount = 0;
		int bigBlobCount = 0;
	
		Improc projectImage = new Improc();
		Mat imageCircle = new Mat();
		DerivativeOfVision derivative = new DerivativeOfVision();
		UDPClient sendDataRoborio = new UDPClient(roboNetworkName, visionPort);
		GripPipeline detectYellowCube = new GripPipeline();
		VideoCapture cap = new VideoCapture();
		
		//Starts the camera at port zero
		cap.open(0);

		hypotenuseSquared =  sizedFrameWidth*sizedFrameWidth + sizedFrameHeight*sizedFrameHeight;
		legLenthIsosceles = Math.sqrt(hypotenuseSquared/(2*(1.0-Math.cos(VIEW_ANGLE_DIAGONAL_DEGREES))));
		heightIsosceles = legLenthIsosceles*Math.cos(0.5*VIEW_ANGLE_DIAGONAL_RADIANS);
				
		Mat blurOutput;
		Mat frame;
		MatOfKeyPoint blobList;
		KeyPoint[] blobArray;
		BufferedImage rectangle;		
		
		while(true) {
			
			startTime = System.currentTimeMillis();
			
			measX = 1;
			measY = 1;
			objRadius = 1;
			
			
			//sizedFrame = new Mat();
			blurOutput = new Mat();
			frame = new Mat();
			cap.retrieve(frame);
			
			
			if(frame.empty())
			{
				//break;
				continue;
			}
			
			++framesProcessed;
			
			//We need to catch exceptions during this process function to ensure we keep 
			//running even if there is a strange exception.
			//I have seen an exception occur in the blur detection pipeline
			detectYellowCube.process(frame);
			detectYellowCube.resizeImageOutput();
			
			blurOutput = detectYellowCube.blurOutput();
			
			blobList = detectYellowCube.findBlobsOutput();
			blobArray = blobList.toArray();
			
			if(!(blobArray.length == 0)) {
				
				KeyPoint blob = getLargestBlob(blobArray);
				
				measX = blob.pt.x;
				measY = blob.pt.y;
				
				objRadius = blob.size * .625;
				double opposite = measX - .5*sizedFrameWidth;
				double adjacent = heightIsosceles;
				
				heading = RAD2DEG * Math.atan(opposite/adjacent);
				opposite = measY - 0.5*sizedFrameHeight;
				distance = CUBE_DIAMETER_INCHES * heightIsosceles/(objRadius);
				
				if (blob.size < 10)
				{
					smallBlobCount++;
				}
				else
				{
					bigBlobCount++;
				}
				
				endTime = System.currentTimeMillis(); 
				timeTaken = endTime - startTime;
				derivativeDistance = derivative.changeOfValue(distance, System.currentTimeMillis());
				//all: %5s big: %5s frequency: %5.2f(for camera testing distance error)
				if(Math.abs(derivativeDistance) > MAX_DERIVATIVE_THRESHOLD) {
					System.out.println(String.format("Reject:, %5s: ( %8.2f, %8.2f, %8.2f),  [ %7.2f, %7.2f ],dD/dt =  %.3f,time: %d",
							Integer.toString(framesProcessed), 
							measX, 
							measY, 
							objRadius, 
							heading, 
							distance,
							//smallBlobCount,
							//bigBlobCount,
							//100*((double)bigBlobCount)/((double)framesProcessed),
							derivativeDistance,
							timeTaken
							));
				}
				else {
				System.out.println(String.format("Accept:, %5s: ( %8.2f, %8.2f, %8.2f),  [ %7.2f, %7.2f ],dD/dt =  %.3f,time: %d", 
							Integer.toString(framesProcessed), 
							measX, 
					        measY, 
							objRadius, 
							heading, 
							distance,
							//smallBlobCount,
							//bigBlobCount,
							//100*((double)bigBlobCount)/((double)framesProcessed),
							derivativeDistance,
							timeTaken
							));
							sendDataRoborio.sendVisionPacket(heading, distance);
				}
				//BufferedImage blurImage = projectImage.Mat2BufferedImage(blurOutput);
				
				//projectImage.displayImage(blurImage);
				
				//Draw a circle
			
				blurOutput.copyTo(imageCircle);
				
				org.opencv.imgproc.Imgproc.circle(imageCircle, new Point(measX, measY), (int)(objRadius), new Scalar(98, 244, 66));
				
				rectangle = Improc.Mat2BufferedImage(imageCircle);
				
				projectImage.displayImage(rectangle);
				
				/*
				imageCircle = blurOutput.clone();
				Mat square1 = new Mat(imageCircle,square);
				BufferedImage circle2 = projectImage.Mat2BufferedImage(imageCircle);
				projectImage.displayImage(circle2);
				//blurCircle.showImage(imageCircle);
				 */

			}	
			else {
				System.out.println("( " + framesProcessed + " ) No Blobs");
			}
		
		}
		
		//System.out.println("Done!\n");
	}
	
	
	public static KeyPoint getLargestBlob(KeyPoint[] blobList)
	{
		KeyPoint largest = blobList[0];
		
		for (KeyPoint kp : blobList){
			if (largest.size < kp.size){
				largest = kp;
			}
		}
		
		return largest;
	}
}

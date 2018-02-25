package vision;

import java.awt.image.BufferedImage;
import java.lang.Math;

import org.opencv.core.Core;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.core.Rect;
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
	
	/*
	 * These constants in the code are used in the mathematics
	 * for finding the blocks distance from the cube which are 
	 * non angular. 
	 */
	
	private static final double CUBE_DIAMETER_INCHES = 17;
	private static final double VIEW_ANGLE_DIAGONAL_DEGREES = 78;
	private static final double VIEW_ANGLE_DIAGONAL_RADIANS = VIEW_ANGLE_DIAGONAL_DEGREES * DEG2RAD;
	
	/*Timer variables
	 * This set of variables is used for a timer in the code.
	 * This allows the editor of the code to know how long it
	 * is taking for a frame to be processed within it.
	 */
	
	public static long startTime;
	public static long endTime;
	public static long timeTaken;
	
	public static void main(String[] arg) {
		
		//This line of code loads the opencv library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		double legLenthIsosceles;
		double heightIsosceles;
		double hypotenuseSquared;
		
		Mat imageCircle = new Mat();
		Mat source;
		Mat squareToMat = new Mat();
		
		//Defines the derivative class 
		DerivativeOfVision derivative = new DerivativeOfVision();
		
		//Defines the yellow cube class
		GripPipeline detectYellowCube = new GripPipeline();
		
		//Defines the camera
		VideoCapture cap = new VideoCapture();
		
		//Starts the camera at port zero
		cap.open(0);
		
		//If the camera is unable to open this line shoots back an error.
		/*
		 * Currently no matter what this line activates and I don't
		 * know why.
		 */
		if(!cap.equals(0)) {
			System.out.println("Error opening video file");
			
		}
		
		//The following two integers are used to define the resolution we will be running at.
		int sizedFrameWidth = 480;
		int sizedFrameHeight = 270;
		
		/*
		 * The following equation below is used to convert
		 */
		hypotenuseSquared =  sizedFrameWidth*sizedFrameWidth + sizedFrameHeight*sizedFrameHeight;
		legLenthIsosceles = Math.sqrt(hypotenuseSquared/(2*(1.0-Math.cos(VIEW_ANGLE_DIAGONAL_DEGREES))));
		heightIsosceles = legLenthIsosceles*Math.cos(0.5*VIEW_ANGLE_DIAGONAL_RADIANS);
		
		VideoWriter video = new VideoWriter("outcpp.avi", VideoWriter.fourcc('M','J','P','G'), 10, new Size(sizedFrameWidth*4, sizedFrameHeight*4));
		
		// Create the square that will be used to outline our blobs
		
		
		Rect square = new Rect((int) 1,(int) 1,(int) 1,(int) 1);
		//Mat squareImg  = new Mat();
		//Mat squareToMat = new Mat(squareImg,square);
		
		Mat sizedFrame;
		Mat blurOutput;
		Mat frame;
		
		MatOfKeyPoint blobList;
		KeyPoint[] blobArray;
		
		BufferedImage rectangle;
		
		double measX, measY, objRadius, heading, pitch, distance ,derivativeDistance;
		
		
		int framesProcessed = 0;
		int smallBlobCount = 0;
		int bigBlobCount = 0;
		
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
			
			//video.write(frame);
			
			++framesProcessed;
			
			detectYellowCube.process(frame);
			
			sizedFrame = detectYellowCube.resizeImageOutput();
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
				pitch = RAD2DEG *Math.atan(opposite/adjacent);
				distance = CUBE_DIAMETER_INCHES * heightIsosceles/(objRadius);
				
				//System.out.println("( " + framesProcessed + " )( " + measX + ", " + measY + " )," + objRadius + " [ " + heading + ", " + distance + " ]");
				/*
				System.out.println(String.format("%5s: ( %8.2f, %8.2f, %8.2f),  [ %7.2f, %7.2f ]", Integer.toString(framesProcessed), 
																									measX, 
																									measY, 
																									objRadius, 
																									heading, 
																									distance));
				*/
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
				if(Math.abs(derivativeDistance) > 8) {
					System.out.println(String.format("Reject:, %5s: ( %8.2f, %8.2f, %8.2f),  [ %7.2f, %7.2f ] small: %5s big: %5s frequency: %5.2f,dD/dt =  %.3f,time: %d",
							Integer.toString(framesProcessed), 
							measX, 
							measY, 
							objRadius, 
							heading, 
							distance,
							smallBlobCount,
							bigBlobCount,
							100*((double)bigBlobCount)/((double)framesProcessed),
							derivativeDistance,
							timeTaken
							));
				}
				else {
				System.out.println(String.format("Accept:, %5s: ( %8.2f, %8.2f, %8.2f),  [ %7.2f, %7.2f ] small: %5s big: %5s frequency: %5.2f,dD/dt =  %.3f,time: %d", 
							Integer.toString(framesProcessed), 
							measX, 
					        measY, 
							objRadius, 
							heading, 
							distance,
							smallBlobCount,
							bigBlobCount,
							100*((double)bigBlobCount)/((double)framesProcessed),
							derivativeDistance,
							timeTaken
							));
				}
				//BufferedImage blurImage = projectImage.Mat2BufferedImage(blurOutput);
				
				//projectImage.displayImage(blurImage);
				
				//Draw a circle
			
				blurOutput.copyTo(imageCircle);
				
				square.x = (int)measX;
				square.y = (int)measY;
				square.height = (int) objRadius;
				square.width = (int) objRadius;
				
				org.opencv.imgproc.Imgproc.circle(imageCircle, new Point(measX, measY), (int)(objRadius), new Scalar(98, 244, 66));
				
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
		
		for (KeyPoint kp : blobList)
		{
			if (largest.size < kp.size)
			{
				largest = kp;
			}
		}
		
		return largest;
	}
}

package vision;

import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import com.atul.JavaOpenCV.Imshow;

public class GripMain {

	private static final double PI = Math.PI;
	private static final double RAD2DEG = 180.0 / PI;
	private static final double DEG2RAD = 1.0 / RAD2DEG;

	private static final double CUBE_DIAMETER_INCHES = 17;
	private static final double VIEW_ANGLE_DIAGONAL_DEGREES = 78;
	private static final double VIEW_ANGLE_DIAGONAL_RADIANS = VIEW_ANGLE_DIAGONAL_DEGREES * DEG2RAD;

	public static void main(String[] arg) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		double legLenthIsosceles;
		double heightIsosceles;
		double hypotenuseSquared;
		
		Imshow sizedShow = new Imshow("SizedFrame");
		Imshow blurShow = new Imshow("BlurFrame");
		Imshow circleShow = new Imshow("imageCircle");
		Imshow blurCircle = new Imshow("blurCircle");
				
		
		Mat source;
		//KeyPoint blobList;
		ArrayList<KeyPoint> blobList = new ArrayList<KeyPoint>();
		
		GripPipeline detectYellowCube = new GripPipeline();
		
		VideoCapture cap = new VideoCapture();
		
		cap.open(1);
		
		if(!cap.equals(1)) {
			System.out.println("Error opening vido file");
			
		}
		
		//int frameWidth = cap.get(CV_PROP_FRAME_WIDTH);
		//int frameHeight = cap.get(CV_CAP_FRAME_HEIGHT);
		
		//System.out.println("Frame: " + frameWidth + ", " + frameHeight);
		
		int sizedFrameWidth = 480;
		
		int sizedFrameHeight = 270;
		
		hypotenuseSquared =  sizedFrameWidth*sizedFrameWidth + sizedFrameHeight*sizedFrameHeight;
		
		legLenthIsosceles = Math.sqrt(hypotenuseSquared/(2*(1.0-Math.cos(VIEW_ANGLE_DIAGONAL_DEGREES))));
		heightIsosceles = legLenthIsosceles*Math.cos(0.5*VIEW_ANGLE_DIAGONAL_RADIANS);
		
		VideoWriter video = new VideoWriter("outcpp.avi", VideoWriter.fourcc('M','J','P','G'), 10, new Size(sizedFrameWidth*4, sizedFrameHeight*4));
		
		int framesProcessed = 0;
		while(true) {
			double measX = 1;
			double measY = 1;
			double objRadius = 1;
			double heading;
			double pitch;
			double distance;
			
			Mat sizedFrame = new Mat();
			Mat blurOutput = new Mat();
			Mat frame = new Mat();
			cap.retrieve(frame);
			
			if(frame.empty())
				break;
			
			video.write(frame);
			
			++framesProcessed;
			
			detectYellowCube.process(frame);
			
			sizedFrame = detectYellowCube.resizeImageOutput();
			blurOutput = detectYellowCube.blurOutput();
			
			//sizedShow.showImage(sizedFrame);
			blurShow.showImage(blurOutput);
			
			MatOfKeyPoint tempBlobList = detectYellowCube.findBlobsOutput();
			
			if(!blobList.isEmpty()) {
				measX = blobList.get(0).pt.x;
				measY = blobList.get(0).pt.y;
				
				objRadius = blobList.get(0).size * .6;
				double opposite = measX - .5*sizedFrameWidth;
				double adjacent = heightIsosceles;
				
				heading = RAD2DEG * Math.atan(opposite/adjacent);
				opposite = measY - 0.5*sizedFrameHeight;
				pitch = RAD2DEG *Math.atan(opposite/adjacent);
				distance = CUBE_DIAMETER_INCHES * heightIsosceles/(2*objRadius);
				
				System.out.println("( " + framesProcessed + " )( " + measX + ", " + measY + " )," + objRadius + " [ " + heading + ", " + distance + " ]");
				
				// Draw a circle
				//Mat imageCircle = sizedFrame.clone();
				//circle(imageCircle, new Point(measX, measY), objRadius, new Scalar(255, 0, 0), 1, 16);
				//circleShow.showImage(imageCircle);
				
				//imageCircle = blurOutput.clone();
				//circle(imageCircle, new Point(measX, measY), objRadius, new Scalar(255, 0, 0), 1, 16);
				//blurCircle.showImage(imageCircle);
				
			}	else {
				System.out.println("( " + framesProcessed + " ) No Blobs");
			}
			/*
			c.wait(500);
			if(c == 27) {
				break;
				
			}*/
		
		}
		
		System.out.println("Done!\n");

	}

}

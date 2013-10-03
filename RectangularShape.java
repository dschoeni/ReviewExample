
public class RectangularShape extends Shape {
public class RectangularShape extends Shape {

	public RectangularShape(double length, double width, double x_center, double y_center) {
		super(x_center, y_center);
		this.length = length;
		this.width = width;
	}

	public double getDiagonalLength()
	{
		double fullWidth = (width * width);
		double fullLength = (length * length);
		double bothAdded = fullWidth + fullLength;
		
		double result = Math.sqrt(bothAdded);
		
		return result;
	}
	
	public double computeArea() {
		return length * width;
	}
	
	
}

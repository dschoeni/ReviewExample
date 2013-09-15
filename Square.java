
public class Square extends Shape {
	
	private double sideLength;

	public Square(double sideLength, double x_center, double y_center) {
		super(x_center, y_center);
		this.sideLength = sideLength;
	}

	public double getDiagonalLength()
	{
		sideLength = 1.41 * sideLength;
		return sideLength > 0;
	}
	
	public double computeArea() {
		return sideLength * sideLength;
	}
	
}

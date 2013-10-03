
public class Circle extends Shape {
	
	// I used a constant!
	private static final double PI = 3.14;
	private double radius;

	public Circle(double x_center, double y_center) {
		super(x_center, y_center);
	}

	public double getDiagonalLength()
	{
		double x = 2 * PI * radius;
		return x;
	}
	
	
	public double computeArea() {
		return radius * radius * PI;
	}
	
}

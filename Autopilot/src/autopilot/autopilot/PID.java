package autopilot.autopilot;

public class PID {

	public double kp;
	public double ki;
	public double kd;
	private double integrator = 0;
	private double differentiator = 0;
	private double error_d1 = 0;
	private int tau;
	private double limit;
	
	public PID(double kp, double kd, double ki, double limit) {
		
		this.kp = kp;
		this.ki = ki;
		this.kd = kd;
		this.limit = limit;
		tau = 5;
	}
	
	public void setGains(double[] gains) {
		
		if(gains.length != 3)
			return;
		kp = gains[0];
		ki = gains[1];
		kd = gains[2];
	}
	
	
	public double control(double commanded, double current, double dT) {
		
		double error = commanded - current;
		integrator = integrator + (dT/2)*(error + error_d1);
		differentiator = (2*tau-dT)/(2*tau+dT)*differentiator + (2/(2*tau+dT))*(error - error_d1);
		double value = kp*error;// + ki*integrator + kd*differentiator;
		
		//anti-wind-up
		if(value > limit)
			value = limit;
		if(value < -limit)
			value = -limit;
		integrator = integrator + dT/ki *(value - (kp*error + ki*integrator + kd*differentiator));
		
		error_d1 = error;
		return value;
	}
	
	public static int toServoCommand(double deltaSurface) {
		
		return 1500 + (int)(500*deltaSurface);
	}
	
	public static int toThrottleCommand(double deltaT) {
		if(deltaT < 0)
			deltaT = 0;
		return 1000 + (int)(deltaT*1000);
	}
}

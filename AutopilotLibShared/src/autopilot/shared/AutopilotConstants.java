package autopilot.shared;

public class AutopilotConstants {

	public static final String PREFIX = "#!autopilot";
	private static final String COMMAND = "command";
	private static final String GAINS = "gains";
	public static final int P = 0;
	public static final int I = 1;
	public static final int D = 2;
	public static final int COMMAND_MSG_TYPE = 0;
	public static final int GAINS_MSG_TYPE = 1;
	public static final int UNDEFINED_TYPE = -1;
	
	public static String sendCommand(double command) {
		String message = PREFIX + "\n" + COMMAND + "\n" + command;
		return message;
	}
	public static double getCommand(String message) {
		String command = message.split("\n")[2];
		return Double.parseDouble(command);
	}
	public static String sendGains(double p, double i, double d) {
		String message = PREFIX + "\n" + GAINS + "\n" + p + "," + i + "," + d;
		return message;
	}
	public static void getGains(String message, double[] gains) {
		String all = message.split("\n")[2];
		String[] meh = all.split(",");
		gains[P] = Double.parseDouble(meh[P]);
		gains[I] = Double.parseDouble(meh[I]);
		gains[D] = Double.parseDouble(meh[D]);
	}
	public static int getMsgType(String message) {
		String word = message.split("\n")[1];
		if(word == COMMAND)
			return COMMAND_MSG_TYPE;
		else if(word == GAINS)
			return GAINS_MSG_TYPE;
		else
			return UNDEFINED_TYPE;
	}
}

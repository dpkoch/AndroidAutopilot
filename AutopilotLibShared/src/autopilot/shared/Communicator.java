package autopilot.shared;

public class Communicator {

	private static final String PREFIX = "#!autopilot";
	private static final String COMMAND = "cmd";
	private static final String GAINS = "gns";
	private static final String WAYPOINT = "wp";
	private static final String TRIM = "trim";
	public static final int P = 0;
	public static final int I = 1;
	public static final int D = 2;
	public static final int LONGITUDE = 0;
	public static final int LATITUDE = 1;
	public static final int COMMAND_MSG_TYPE = 0;
	public static final int GAINS_MSG_TYPE = 1;
	public static final int WAYPOINT_MSG_TYPE = 2;
	public static final int TRIM_MSG_TYPE = 3;
	public static final int UNDEFINED_TYPE = -1;
	
	public static String sendCommand(String command) {
		String message = PREFIX + "\n" + COMMAND + "\n" + command;
		return message;
	}
	public static double getCommand(String message) {
		String command = message.split("\n")[2];
		return Double.parseDouble(command);
	}
	
	public static String sendGains(String p, String i, String d) {
		if(p.equals(""))
			p = "0";
		if(i.equals(""))
			i = "0";
		if(d.equals(""))
			d = "0";
		String message = PREFIX + "\n" + GAINS + "\n" + p + "," + i + "," + d;
		return message;
	}
	
	public static void getGains(String message, double[] gains) {
		String word = message.split("\n")[2];
		String[] meh = word.split(",");
		if(meh[P] != null && !meh[P].equals(""))
			gains[P] = Double.parseDouble(meh[P]);
		if(meh[I] != null && !meh[I].equals(""))
			gains[I] = Double.parseDouble(meh[I]);
		if(meh[D] != null && !meh[D].equals(""))
			gains[D] = Double.parseDouble(meh[D]);
	}
	
	public static String sendWaypoint(Double longitude, Double latitude) {
		String message = PREFIX + "\n" + WAYPOINT + "\n" + longitude + "," + latitude;
		return message;
	}
	
	public static void getWaypoint(String message, double[] location) {
		String word = message.split("\n")[2];
		String[] wpLocation = word.split(",");
		location[LONGITUDE] = Double.parseDouble(wpLocation[LONGITUDE]);
		location[LATITUDE] = Double.parseDouble(wpLocation[LATITUDE]);
	}
	
	public static String sendTrim() {
		String message = PREFIX + "\n" + TRIM;
		return message;
	}
	
	public static int getMsgType(String message) {
		String word = message.split("\n")[1];
		if(word.equals(COMMAND))
			return COMMAND_MSG_TYPE;
		else if(word.equals(GAINS))
			return GAINS_MSG_TYPE;
		else if(word.equals(WAYPOINT))
			return WAYPOINT_MSG_TYPE;
		else if(word.equals(TRIM))
			return TRIM_MSG_TYPE;
		else
			return UNDEFINED_TYPE;
	}
	
	public static boolean isAutopilotMSG(String message) {
		String prefix = message.split("\n")[0];
		if(message == null)
			return false;
		if(prefix.equals(PREFIX))
			return true;
		else
			return false;
	}
}

package autopilot.autopilot;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.widget.ToggleButton;
import autopilot.autopilot.R;


public class MainActivity extends IOIOActivity {

	private ToggleButton button;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		button = (ToggleButton) findViewById(R.id.toggle_button);
	}
	
	class Looper extends BaseIOIOLooper {
		
		private DigitalOutput led;
		
		@Override
		protected void setup() throws ConnectionLostException {
			led = ioio_.openDigitalOutput(0, true);
		}
		
		@Override
		public void loop() throws ConnectionLostException {
			led.write(!button.isChecked());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
	}
	
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
}

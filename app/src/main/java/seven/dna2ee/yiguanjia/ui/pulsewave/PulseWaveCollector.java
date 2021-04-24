package seven.dna2ee.yiguanjia.ui.pulsewave;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import seven.dna2ee.yiguanjia.driver.SerialInputOutputManager;
import seven.dna2ee.yiguanjia.driver.UsbSerialDriver;
import seven.dna2ee.yiguanjia.driver.UsbSerialPort;
import seven.dna2ee.yiguanjia.driver.UsbSerialProber;

public class PulseWaveCollector implements Runnable, SerialInputOutputManager.Listener {
    private static final int WAIT_MILLIS = 2000;
    private static final int SAMPLE_PER_MIN = 1830 * 60;
    private static final String ACTION_USB_PERMISSION = "seven.dna2ee.yiguanjia.USB_PERMISSION";

    private Activity activity;
    private PulseWaveViewModel viewModel;
    private UsbManager usbMgr;
    private UsbSerialDriver driver;

    private int[] buf = new int[SAMPLE_PER_MIN + 1];
    private int bufCur = 0;
    private int bufHead = 0;
    private boolean init;
    private boolean loop;

    // private String debugData = "";

    private void putOnePoint(int data) {
        buf[bufCur] = data;
        bufCur ++;
        if (bufCur >= SAMPLE_PER_MIN + 1) {
            bufCur = 0;
        }
        if (bufCur == bufHead) {
            bufHead ++;
            if (bufHead >= SAMPLE_PER_MIN + 1) {
                bufHead = 0;
            }
        }
    }

    public PulseWaveCollector(PulseWaveViewModel _viewModel, UsbManager _usbMgr, Activity _activity) {
        this.viewModel = _viewModel;
        this.usbMgr = _usbMgr;
        this.activity = _activity;
        this.loop = false;
        this.init = true;
    }

    private void attemptToGetUSBPermission() {
        if (this.usbMgr.hasPermission(driver.getDevice())) return;
        this.usbMgr.requestPermission(
                driver.getDevice(),
                PendingIntent.getBroadcast(this.activity, 0, new Intent(ACTION_USB_PERMISSION), 0)
        );
    }

    public void stop() {
        this.loop = false;
    }

    public void start() {
        this.loop = true;
        this.init = false;
        new Thread(this, "PulseWaveCollector").start();
    }

    private void sendUserMessage(String s) {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PulseWaveCollector.this.viewModel.setMsg(s);
            }
        });
    }

    private void sendNewPoints(float[] fs) {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PulseWaveCollector.this.viewModel.setNewPoints(fs);
            }
        });
    }

    public boolean isInDataLoop() {
        return this.loop;
    }

    public boolean isInitialized() {
        return this.init;
    }

    @Override
    public void run() {
        // Find all available drivers from attached devices.
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(this.usbMgr);
        if (availableDrivers.isEmpty()) {
            this.loop = false;
            this.init = true;
            this.sendUserMessage("YiGuanJia device X: 0");
            return;
        }

        // Open a connection to the first available driver.
        this.driver = availableDrivers.get(0);
        this.attemptToGetUSBPermission();
        UsbDeviceConnection connection = this.usbMgr.openDevice(driver.getDevice());
        if (connection == null) {
            this.loop = false;
            this.init = true;
            this.sendUserMessage(String.format("YiGuanJia device X: %d", availableDrivers.size()));
            Log.e("YiGuanJia", "YiGuanJia device is not available.");
            return;
        }

        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        SerialInputOutputManager usbIoManager = null;
        try {
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            usbIoManager = new SerialInputOutputManager(port, this);
            usbIoManager.start();
            port.write(new byte[]{ (byte)0xC0, 0x0C }, WAIT_MILLIS);
            this.viewModel.setMsg("collecting ...");
            while (this.loop) {
                Thread.sleep(1000);
            }
            port.write(new byte[] { (byte)0xC0, 0x00, 0x00, 0x00 }, WAIT_MILLIS);
        } catch (InterruptedException e) {
        } catch (IOException e) {
            this.viewModel.setMsg(e.toString());
        } finally {
            this.loop = false;
            if (usbIoManager != null) {
                usbIoManager.stop();
            }
            try {
                port.close();
            } catch (IOException e) {
                Log.e("YiGuanJia", "YiGuanJia device close error.");
            }
        }
    }

    @Override
    public void onNewData(byte[] data) {
        int n = data.length;
        if (n < 4) return;
        // A0 00 .. ..
        int N = n / 4, cur = 0;
        float[] newpoints = new float[N];
        for (int i = 2; i < n; i += 4) {
            int ah = data[i], al = data[i+1];
            int v = ah * 256 + al;
            newpoints[cur] = v; cur ++;
            putOnePoint(v);
        }
        sendNewPoints(newpoints);
        // this.debugData = StringEx.bytesToHex(data);
        // Log.i("YiGuanJia", String.format("!!Data!!: %s", debugData));
    }

    @Override
    public void onRunError(Exception e) {
    }
}

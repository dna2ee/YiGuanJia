package seven.dna2ee.yiguanjia.ui.accupoint;

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
import seven.dna2ee.yiguanjia.util.StringEx;

public class AccuPointCollector implements Runnable, SerialInputOutputManager.Listener {
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;
    private static final String ACTION_USB_PERMISSION = "seven.dna2ee.yiguanjia.USB_PERMISSION";

    private Activity activity;
    private AccuPointViewModel viewModel;
    private UsbManager usbMgr;
    private UsbSerialDriver driver;

    private double accupoint;
    private boolean init;
    private boolean loop;

    private String debugData = "";

    public AccuPointCollector(AccuPointViewModel _viewModel, UsbManager _usbMgr, Activity _activity) {
        this.viewModel = _viewModel;
        this.usbMgr = _usbMgr;
        this.activity = _activity;
        this.loop = false;
        this.init = true;
        this.accupoint = -1.0;
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
        new Thread(this, "AccuPointCollector").start();
    }

    private void sendUserMessage(String s) {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AccuPointCollector.this.viewModel.setMsg(s);
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
            this.viewModel.setMsg(String.format("YiGuanJia device X: %d", availableDrivers.size()));
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
            port.write(new byte[]{ (byte)0xC0, 0x03 }, WRITE_WAIT_MILLIS);
            while (this.loop) {
                // sendUserMessage(String.format("data: %s", debugData));
                if (this.accupoint < 0 && !this.init) {
                    this.init = true;
                    this.viewModel.setAccupoint(Double.POSITIVE_INFINITY);
                    sendUserMessage("initializing ...");
                } else if (this.accupoint < 1.0e-3) {
                    this.viewModel.setAccupoint(Double.POSITIVE_INFINITY);
                    sendUserMessage(String.format("Current: Inf"));
                } else {
                    double s = 500000.0 / this.accupoint;
                    this.viewModel.setAccupoint(s);
                    sendUserMessage(String.format("Current: %.2f", s));
                }
                Thread.sleep(1000);
            }
            port.write(new byte[] { (byte)0xC0, 0x00, 0x00, 0x00 }, WRITE_WAIT_MILLIS);
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
        if (n < 2) return;
        if (n % 2 != 0) return;
        double sum = 0.0;
        int count = 0;
        for (int i = 1; i < n; i += 4) {
            sum += data[i];
            count ++;
        }
        if (count > 0) {
            if (this.accupoint < 0) {
                this.accupoint = sum / count;
            } else {
                this.accupoint = this.accupoint * 0.9 + sum / count * 0.1;
            }
        }
        this.debugData = StringEx.bytesToHex(data);
        Log.i("YiGuanJia", String.format("!!Data!!: %s", debugData));
    }

    @Override
    public void onRunError(Exception e) {
    }
}

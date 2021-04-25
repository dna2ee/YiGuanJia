package seven.dna2ee.yiguanjia.ui.pulsewave;

import android.content.Context;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import seven.dna2ee.yiguanjia.R;

public class PulseWaveFragment extends Fragment {

    private static final int CHART_POINT_N = 3000;
    private PulseWaveCollector collector;
    private LineChart chart;
    private ArrayList<Entry> values;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_empty, container, false);
        final LinearLayout body = root.findViewById(R.id.body);
        final Context context = this.getContext();

        final LinearLayout panelInput = new LinearLayout(context);
        panelInput.setOrientation(LinearLayout.VERTICAL);
        final Button btnRecord = new Button(context);
        btnRecord.setText("...");
        btnRecord.setFocusable(true);
        panelInput.addView(btnRecord);
        body.addView(panelInput);

        final ScrollView panelRecords = new ScrollView(context);
        final LinearLayout layoutRecords = new LinearLayout(context);
        layoutRecords.setOrientation(LinearLayout.VERTICAL);
        this.chart = new LineChart(context);
        chart.setDrawBorders(false);
        chart.setMaxVisibleValueCount(20);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setClickable(false);
        chart.setDrawMarkers(false);
        chart.setMinimumHeight(300);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setDrawLabels(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setDrawLabels(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.getDescription().setEnabled(false);
        this.values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            float val = (float) (Math.random() * 100);
            values.add(new Entry(i, val));
        }
        chart.setData(new LineData(new LineDataSet[] { new LineDataSet(values, "test") }));
        chart.getData().setHighlightEnabled(false);
        LineDataSet dataset = (LineDataSet)chart.getData().getDataSetByIndex(0);
        dataset.setDrawCircles(false);
        dataset.setColor(Color.RED);
        layoutRecords.addView(chart);

        final Button btnSave = new Button(context);
        btnSave.setText("save");
        btnSave.setFocusable(true);
        layoutRecords.addView(btnSave);
        panelRecords.addView(layoutRecords);

        panelRecords.setPadding(5, 5, 5, 5);
        body.addView(panelRecords);

        PulseWaveViewModel viewModel = new ViewModelProvider(this).get(PulseWaveViewModel.class);
        viewModel.getCurrent().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                btnRecord.setText(s);
            }
        });
        viewModel.getNewPoints().observe(getViewLifecycleOwner(), new Observer<float[]>() {
            @Override
            public void onChanged(float[] newpoints) {
                int n = values.size();
                Entry last = null;
                if (n == 0) {
                    last = new Entry(-1, 0);
                } else {
                    last = values.get(n - 1);
                }
                int cur = (int)last.getX() + 1;
                for (float x : newpoints) {
                    values.add(new Entry(cur, x));
                    cur ++;
                }
                n = n + newpoints.length - CHART_POINT_N;
                if (n > 0) {
                    Entry[] origin = new Entry[values.size()]; values.toArray(origin);
                    Entry[] trunc = new Entry[CHART_POINT_N];
                    System.arraycopy(origin, n, trunc, 0, CHART_POINT_N);
                    values = new ArrayList<>(Arrays.asList(trunc));
                }
                if (cur > 1000000) {
                    cur = 0;
                    for (Entry x : values) {
                        x.setX(cur); cur ++;
                    }
                }
                ((LineDataSet)chart.getData().getDataSetByIndex(0)).setValues(values);
                chart.getData().notifyDataChanged();
                chart.notifyDataSetChanged();
                chart.invalidate();
            }
        });
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (collector.isInitialized() && !collector.isInDataLoop()) {
                    viewModel.setMsg("Starting collector ...");
                    collector.start();
                    return;
                }
                // TODO: click when collecting
            }
        });
        btnRecord.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                collector.stop();
                viewModel.setMsg("Collector stopped.");
                return true;
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Integer> r = collector.getPulseWave1min();
                Date d = new Date();
                Calendar cal = Calendar.getInstance(TimeZone.getDefault());
                cal.setTime(d);

                String externalDir = "/sdcard";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    externalDir = Environment.getStorageDirectory().getAbsolutePath();
                }

                File f = new File(String.format(
                        externalDir + "/pulsewave-%d-%d-%d-%d-%d-%d.%d.dat",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.get(Calendar.HOUR),
                        cal.get(Calendar.MINUTE),
                        cal.get(Calendar.SECOND),
                        cal.get(Calendar.MILLISECOND)
                ));
                byte int16LE[] = new byte[2];
                try {
                    FileOutputStream writer = new FileOutputStream(f);
                    for (int x : r) {
                        int16LE[0] = (byte)(x % 256);
                        int16LE[1] = (byte)(x / 256);
                        writer.write(int16LE);
                    }
                    writer.close();
                } catch (IOException e) {
                    Log.e("YiGuanJia", e.toString());
                } finally {
                }
            }
        });

        this.collector = new PulseWaveCollector(
                viewModel,
                (UsbManager) this.getActivity().getSystemService(context.USB_SERVICE),
                this.getActivity()
        );
        viewModel.setMsg("click to start");

        return root;
    }
}
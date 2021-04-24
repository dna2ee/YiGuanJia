package seven.dna2ee.yiguanjia.ui.pulsewave;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;

import seven.dna2ee.yiguanjia.R;

public class PulseWaveFragment extends Fragment {

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
        ((LineDataSet)chart.getData().getDataSetByIndex(0)).setDrawCircles(false);
        panelRecords.addView(chart);
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
                n = n + newpoints.length - 1000;
                if (n > 0) {
                    Entry[] origin = new Entry[values.size()]; values.toArray(origin);
                    Entry[] trunc = new Entry[1000];
                    System.arraycopy(origin, n, trunc, 0, 1000);
                    values = new ArrayList<>(Arrays.asList(trunc));
                }
                if (cur > 100000) {
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

        this.collector = new PulseWaveCollector(
                viewModel,
                (UsbManager) this.getActivity().getSystemService(context.USB_SERVICE),
                this.getActivity()
        );
        viewModel.setMsg("click to start");

        return root;
    }
}
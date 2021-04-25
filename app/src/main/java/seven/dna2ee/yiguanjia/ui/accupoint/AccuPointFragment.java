package seven.dna2ee.yiguanjia.ui.accupoint;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import seven.dna2ee.yiguanjia.R;

public class AccuPointFragment extends Fragment {

    private AccuPointCollector collector;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_empty, container, false);
        final LinearLayout body = root.findViewById(R.id.body);
        final Context context = this.getContext();

        final LinearLayout panelInput = new LinearLayout(context);
        panelInput.setOrientation(LinearLayout.VERTICAL);
        final Button btnRecord = new Button(context);
        btnRecord.setText("...");
        btnRecord.setFocusable(true);
        panelInput.addView(btnRecord);
        final EditText txtName = new EditText(context);
        txtName.setHint("AccuPoint Name");
        txtName.setMaxLines(1);
        txtName.setInputType(InputType.TYPE_CLASS_TEXT);
        panelInput.addView(txtName);
        body.addView(panelInput);

        final ScrollView panelRecords = new ScrollView(context);
        final LinearLayout layoutRecords = new LinearLayout(context);
        layoutRecords.setOrientation(LinearLayout.VERTICAL);
        final TextView txtRecords = new TextView(context);
        txtRecords.setTextIsSelectable(true);
        txtRecords.setText("");
        txtRecords.setFocusable(true);
        layoutRecords.addView(txtRecords);

        final Button btnSave = new Button(context);
        btnSave.setText("save");
        btnSave.setFocusable(true);
        layoutRecords.addView(btnSave);

        panelRecords.addView(layoutRecords);
        panelRecords.setPadding(5, 5, 5, 5);
        body.addView(panelRecords);

        final AccuPointViewModel viewModel = new ViewModelProvider(this).get(AccuPointViewModel.class);
        viewModel.getRecords().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                txtRecords.setText(s);
                panelRecords.post(new Runnable() {
                    @Override
                    public void run() {
                        panelRecords.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
        viewModel.getCurrent().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                btnRecord.setText(s);
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
                String name = txtName.getText().toString();
                if (name.length() == 0) {
                    name = "(anonymous)";
                }
                viewModel.addAccupointRecord(name);
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
                ArrayList<String> r = viewModel.getRawRecords();
                Date d = new Date();
                Calendar cal = Calendar.getInstance(TimeZone.getDefault());
                cal.setTime(d);

                String externalDir = "/sdcard";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    externalDir = Environment.getStorageDirectory().getAbsolutePath();
                }

                File f = new File(String.format(
                        externalDir + "/accupoint-%d-%d-%d-%d-%d-%d.%d.txt",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.get(Calendar.HOUR),
                        cal.get(Calendar.MINUTE),
                        cal.get(Calendar.SECOND),
                        cal.get(Calendar.MILLISECOND)
                ));
                try {
                    PrintWriter writer = new PrintWriter(f);
                    for (String one : r) {
                        writer.println(one);
                    }
                    writer.close();
                } catch (IOException e) {
                    Log.e("YiGuanJia", e.toString());
                } finally {
                }
            }
        });

        this.collector = new AccuPointCollector(
                viewModel,
                (UsbManager) this.getActivity().getSystemService(context.USB_SERVICE),
                this.getActivity()
        );
        viewModel.setMsg("click to start");

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.collector.stop();
    }
}
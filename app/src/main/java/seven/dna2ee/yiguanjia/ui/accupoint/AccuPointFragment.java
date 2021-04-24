package seven.dna2ee.yiguanjia.ui.accupoint;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
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
        final TextView txtRecords = new TextView(context);
        txtRecords.setTextIsSelectable(true);
        txtRecords.setText("");
        txtRecords.setFocusable(true);
        panelRecords.addView(txtRecords);
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
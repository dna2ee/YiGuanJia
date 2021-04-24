package seven.dna2ee.yiguanjia.ui.pulsewave;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import seven.dna2ee.yiguanjia.R;

public class PulseWaveFragment extends Fragment {

    private PulseWaveViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel =
                new ViewModelProvider(this).get(PulseWaveViewModel.class);
        View root = inflater.inflate(R.layout.fragment_pulsewave, container, false);
        final TextView textView = root.findViewById(R.id.text_dashboard);
        viewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}
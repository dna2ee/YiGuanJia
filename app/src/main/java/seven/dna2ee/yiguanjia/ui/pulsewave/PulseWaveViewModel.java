package seven.dna2ee.yiguanjia.ui.pulsewave;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PulseWaveViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public PulseWaveViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is PulseWave dashboard fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
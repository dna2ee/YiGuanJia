package seven.dna2ee.yiguanjia.ui.pulsewave;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class PulseWaveViewModel extends ViewModel {

    private MutableLiveData<String> textCurrent;
    private MutableLiveData<float[]> listNewPoints;

    private String msg;

    public PulseWaveViewModel() {
        this.textCurrent = new MutableLiveData<>();
        this.listNewPoints = new MutableLiveData<>();
        this.msg = "...";
    }

    public void setMsg(String _msg) {
        this.msg = _msg;
        textCurrent.setValue(this.msg);
    }

    public void setNewPoints(float[] data) {
        listNewPoints.setValue(data);
    }

    public LiveData<String> getCurrent() { return textCurrent; }

    public LiveData<float[]> getNewPoints() { return listNewPoints; }
}